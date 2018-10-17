/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr;

import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import de.uros.citlab.errorrate.util.HeatMapUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Module, which uses the {@link PathCalculatorGraph} to calculate the error
 * rates between tokens. Some other classes are needed, to calculate the error
 * rate. See {@link ITokenizer} and
 * {@link IStringNormalizer} for more details.
 *
 * @author gundram
 */
public class ErrorModuleEnd2End implements IErrorModule {
    private final ObjectCounter<Count> counter = new ObjectCounter<>();
    private final ObjectCounter<RecoRef> counterSub = new ObjectCounter<>();
    private final ITokenizer tokenizer;
    private final Boolean detailed;
    private final IStringNormalizer stringNormalizer;
    private final PathCalculatorGraph<String, String> pathCalculator = new PathCalculatorGraph<>();
    private final Mode mode;
    private final Voter voter = new Voter();

    public ErrorModuleEnd2End(ICategorizer categorizer, IStringNormalizer stringNormalizer, Mode mode, Boolean detailed) {
        this(new TokenizerCategorizer(categorizer), stringNormalizer, mode, detailed);
    }

    public enum Mode {
        READINGORDER,
        IGNORE_READINGORDER,
        IGNORE_SEGMENTATION
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, Boolean detailed) {
        this.detailed = detailed;
        this.mode = mode;
        if (tokenizer == null) {
            throw new RuntimeException("no tokenizer given (is null)");
        }
        this.tokenizer = tokenizer;
        this.stringNormalizer = stringNormalizer;
        pathCalculator.addCostCalculator(new CCDeletion(voter));
        pathCalculator.addCostCalculator(new CCInsertion(voter));
        pathCalculator.addCostCalculator(new CCSubOrCor(voter));
        switch (mode) {
            case IGNORE_READINGORDER:
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) new LineLineBreakRecoJump(voter));
                break;
            case READINGORDER:
                break;
            default:
                throw new RuntimeException("not implemented yet");
        }
    }

    /**
     * normalize and tokenize both inputs. Afterwards find the cheapest cost to
     * manipulate the recognition tokens to come to the reference tokens. Count
     * the manipulation which had to be done. If detailed==null or
     * detailed==True, confusion/substitution map is filled.
     *
     * @param reco hypothesis
     * @param ref  reference
     */
    @Override
    public void calculate(String reco, String ref) {
        //use string normalizer, if set
        if (stringNormalizer != null) {
            reco = stringNormalizer.normalize(reco);
            ref = stringNormalizer.normalize(ref);
        }

        //TODO: better place to add "\n" to strings!
        if (!reco.startsWith("\n")) {
            reco = "\n" + reco;
        }
        if (!reco.endsWith("\n")) {
            reco += "\n";
        }
        if (!ref.startsWith("\n")) {
            ref = "\n" + ref;
        }
        if (!ref.endsWith("\n")) {
            ref += "\n";
        }
        //tokenize both strings
        String[] recos = tokenizer.tokenize(reco).toArray(new String[0]);
        String[] refs = tokenizer.tokenize(ref).toArray(new String[0]);
        //use dynamic programming to calculate the cheapest path through the dynamic programming tabular
//        calcBestPathFast(recos, refs);
        pathCalculator.setUpdateScheme(PathCalculatorGraph.UpdateScheme.ALL);
        PathCalculatorGraph.DistanceMat<String, String> mat = pathCalculator.calcDynProg(Arrays.asList(recos), Arrays.asList(refs));
        double[][] out = new double[mat.getSizeY()][mat.getSizeX()];
        for (int i = 0; i < out.length; i++) {
            double[] outV = out[i];
            for (int j = 0; j < outV.length; j++) {
                PathCalculatorGraph.IDistance<String, String> dist = mat.get(i, j);
                outV[j] = dist == null ? 0 : dist.getCostsAcc();
            }
        }
        List<PathCalculatorGraph.IDistance<String, String>> calcBestPath = pathCalculator.calcBestPath(mat);
        if (calcBestPath != null) {
            double max = calcBestPath.get(calcBestPath.size()-1).getCostsAcc();
//            for (int i = 0; i < out.length; i++) {
//                double[] vec = out[i];
//                for (int j = 0; j < vec.length; j++) {
//                    max = Math.max(max, vec[j]);
//                }
//            }
            for (int i = 0; i < out.length; i++) {
                double[] vec = out[i];
                for (int j = 0; j < vec.length; j++) {
                    if(vec[j]>max+1e-6){
                        vec[j]=max;
                    }
                }
            }

            max *= 0.5;
            for (PathCalculatorGraph.IDistance<String, String> dist : calcBestPath) {
                int y = dist.getPoint()[0];
                int x = dist.getPoint()[1];
//                if (y > 0 && x > 0) {
                out[y][x] = out[y][x] +  max;
                System.out.println(dist);
//                }
            }
        }
        HeatMapUtil.save(HeatMapUtil.getHeatMap(out, 3), new File("out.png"));
        if (calcBestPath == null) {
            return;
        }
        for (Count c : Count.values()) {
            counter.add(c, 0L);
        }
        //plus 1 because we add a \n
        //TODO: clean handling of \n
        int[] usedReco = new int[recos.length];
        for (PathCalculatorGraph.IDistance<String, String> dist : calcBestPath) {
            String m = dist.getManipulation();
            if (m == null) {
                continue;
            }
            switch (DistanceStrStr.TYPE.valueOf(m)) {
                case DEL:
                case INS:
                case SUB:
                    counter.add(Count.ERR);
                case COR:
                    counter.add(Count.valueOf(dist.getManipulation()));
//                    if(dist.getPoint()[0] - 1>=usedReco.length){
//                        throw new RuntimeException("not in bound");
//                    }
                    //do not count INS because no reco-chars are use while INS.
                    if (dist.getRecos() != null) {
                        usedReco[dist.getPoint()[0] - 1] += dist.getRecos().length;
                    }
                    break;
                case COR_LINEBREAK:
                    break;
                case JUMP_RECO:
                    break;
                default:
                    System.out.println("found type '" + dist.getManipulation() + "'");
            }
        }
        String s = Arrays.toString(usedReco);
        System.out.println("------------------------------------------");
        System.out.println(reco.replace("\n", "\\n"));
        System.out.println(s);
        System.out.println(Arrays.toString(recos).replace("\n", "*"));
        System.out.println("------------------------------------------");
        int i = countUnusedChars(usedReco, recos);
        if (!hasMultiUse(usedReco)) {
            i -= countMultiplyChars(usedReco, recos);
        }
        counter.add(Count.DEL, i);
        counter.add(Count.ERR, i);
        if (hasMultiUse(usedReco)) {
            System.out.println("stop");
        }
        counter.add(Count.GT, refs.length);
        counter.add(Count.HYP, recos.length);

    }

    private int countUnusedChars(int[] usage, String[] out) {
        int count = 0;
        for (int i = 0; i < usage.length; i++) {
            if (usage[i] == 0 && !voter.isLineBreak(out[i])) {
                count++;
            }
        }
        return count;
    }

    private int countMultiplyChars(int[] usage, String[] out) {
        int count = 0;
        for (int i = 0; i < usage.length; i++) {
            if (usage[i] > 0 && !voter.isLineBreak(out[i])) {
                count += usage[i] - 1;
            }
        }
        return count;
    }

    private boolean hasMultiUse(int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] > 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset() {
        counter.reset();
        counterSub.reset();
    }

    /**
     * returns the absolute and relative frequency of manipulation. If
     * detailed==null or detailed==True, the confusion map is added in before
     * the basic statistic.
     *
     * @return human readable result
     */
    @Override
    public List<String> getResults() {
        LinkedList<String> res = new LinkedList<>();
        if (detailed == null || detailed) {
            for (Pair<RecoRef, Long> pair : counterSub.getResultOccurrence()) {
                RecoRef first = pair.getFirst();
                String key1;
                switch (first.recos.length) {
                    case 0:
                        key1 = "";
                        break;
                    case 1:
                        key1 = first.recos[0];
                        break;
                    default:
                        key1 = Arrays.toString(first.recos);
                }
                String key2;
                switch (first.refs.length) {
                    case 0:
                        key2 = "";
                        break;
                    case 1:
                        key2 = first.refs[0];
                        break;
                    default:
                        key2 = Arrays.toString(first.refs);
                }
                res.addFirst("[" + key1 + "=>" + key2 + "]=" + pair.getSecond());
            }
        }
        List<Pair<Count, Long>> resultOccurrence = getCounter().getResultOccurrence();
//        long sum = 0;
//        int length = 1;
//        for (Pair<Count, Long> pair : resultOccurrence) {
//            sum += pair.getSecond();
//            length = Math.max(length, pair.getFirst().toString().length());
//        }
//        int length2 = String.valueOf(sum).length();
//        res.add(String.format("%" + length + "s =%6.2f%% ; %" + length2 + "d", "ALL", 100.0, sum));
//        for (Pair<Count, Long> pair : resultOccurrence) {
//            res.add(String.format("%" + length + "s =%6.2f%% ; %" + length2 + "d", pair.toString(), (((double) pair.getSecond()) / sum * 100), pair.getSecond()));
//        }
        res.add(resultOccurrence.toString());
        return res;
    }

    private static class CostCalculatorIntern implements PathCalculatorGraph.ICostCalculator<String, String> {

        private List<String> recos;
        private List<String> refs;
        private PathCalculatorGraph.DistanceMat<String, String> mat;
        private final String manipulation;
        private final String[] emptyList = new String[0];

        public CostCalculatorIntern(String manipulation) {
            this.manipulation = manipulation;
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int y = point[0];
            final int x = point[1];
            switch (manipulation) {
                case "SUB": {
                    int xx = x + 1;
                    int yy = y + 1;
                    if (yy >= recos.size() || xx >= refs.size()) {
                        return null;
                    }
                    final double cost = recos.get(yy).equals(refs.get(xx)) ? 0 : 1;
                    return new PathCalculatorGraph.Distance<>(cost == 0 ? "COR" : "SUB",
                            cost, dist.getCostsAcc() + cost,
                            new int[]{yy, xx},
                            point, new String[]{recos.get(yy)},
                            new String[]{refs.get(xx)});
                }
                case "INS": {
                    int xx = x + 1;
                    if (xx >= refs.size()) {
                        return null;
                    }
                    final double cost = 1;
                    return new PathCalculatorGraph.Distance<>("INS",
                            cost, dist.getCostsAcc() + cost,
                            new int[]{y, xx},
                            point, emptyList,
                            new String[]{refs.get(xx)});
                }
                case "DEL": {
                    int yy = y + 1;
                    if (yy >= recos.size()) {
                        return null;
                    }
                    final double cost = 1;
                    return new PathCalculatorGraph.Distance<>("DEL",
                            cost, dist.getCostsAcc() + cost,
                            new int[]{yy, x},
                            point, new String[]{recos.get(yy)},
                            emptyList);
                }
                default:
                    throw new RuntimeException("not expected manipulation " + manipulation);
            }
        }

        @Override
        public void init(PathCalculatorGraph.DistanceMat<String, String> mat, List<String> recos, List<String> refs) {
            this.mat = mat;
            this.recos = recos;
            this.refs = refs;
        }

    }

    @Override
    public ObjectCounter<Count> getCounter() {
        return counter;
    }

    private static class Voter {
        private boolean isSpace(String s) {
            return " ".equals(s);
        }

        private boolean isLineBreak(String s) {
            return "\n".equals(s);
        }
    }

    private static abstract class CCAbstract implements PathCalculatorGraph.ICostCalculator<String, String> {

        PathCalculatorGraph.DistanceMat<String, String> mat;
        List<String> recos;
        List<String> refs;
        final private Voter voter;
        int[] lineBreaksReco;
        int[] lineBreaksRef;

        public CCAbstract(Voter voter) {
            this.voter = voter;
        }

        boolean isSpaceRef(int pos) {
            return voter.isSpace(refs.get(pos));
        }

        boolean isSpaceReco(int pos) {
            return voter.isSpace(recos.get(pos));
        }

        boolean isLineBreakRef(int pos) {
            return voter.isLineBreak(refs.get(pos));
        }

        boolean isLineBreakReco(int pos) {
            return voter.isLineBreak(recos.get(pos));
        }

        @Override
        public void init(PathCalculatorGraph.DistanceMat<String, String> mat, List<String> recos, List<String> refs) {
            this.mat = mat;
            this.recos = recos;
            this.refs = refs;
            lineBreaksReco = getLineBreaks(recos);
            lineBreaksRef = getLineBreaks(refs);
        }

        private int[] getLineBreaks(List<String> text) {
            int[] tmp = new int[text.size()];
            int cnt = 0;
            for (int i = 0; i < text.size(); i++) {
                if (voter.isLineBreak(text.get(i))) {
                    tmp[cnt++] = i;
                }
            }
            int[] res = new int[cnt];
            System.arraycopy(tmp, 0, res, 0, cnt);
            return res;
        }

    }

    private static class DistanceStrStr implements PathCalculatorGraph.IDistance<String, String> {

        private DistanceStrStr(TYPE type, double costAcc, String reco, String ref, int[] pointPrevious, int[] point) {
            this.type = type;
            this.costAcc = costAcc;
            this.reco = reco;
            this.ref = ref;
            this.pointPrevious = pointPrevious;
            this.point = point;
        }

        private enum TYPE {INS, DEL, SUB, COR, JUMP_RECO, COR_LINEBREAK}

        private final TYPE type;
        private final double costAcc;
        private final String reco;
        private final String ref;
        private final int[] pointPrevious;
        private final int[] point;

        @Override
        public String toString() {
            return "DistanceStrStr{" +
                    "type=" + type +
                    ", costAcc=" + costAcc +
                    ", reco='" + (reco == null ? reco : reco.replace("\n", "\\n")) + '\'' +
                    ", ref='" + (ref == null ? ref : ref.replace("\n", "\\n")) + '\'' +
                    ", pointPrevious=" + Arrays.toString(pointPrevious) +
                    ", point=" + Arrays.toString(point) +
                    '}';
        }

        @Override
        public double getCosts() {
            switch (type) {
                case DEL:
                case INS:
                case SUB:
                    return 1;
                default:
                    return 0;
            }
        }

        @Override
        public double getCostsAcc() {
            return costAcc;
        }

        @Override
        public String[] getRecos() {
            return reco == null ? null : new String[]{reco};
        }

        @Override
        public String[] getReferences() {
            return ref == null ? null : new String[]{ref};
        }

        @Override
        public String getManipulation() {
            return type == null ? null : type.toString();
        }

        @Override
        public int[] getPointPrevious() {
            return pointPrevious;
        }

        @Override
        public int[] getPoint() {
            return point;
        }

        @Override
        public boolean equals(PathCalculatorGraph.IDistance<String, String> obj) {
            return Arrays.equals(point, obj.getPoint()) && getManipulation().equals(obj.getManipulation()) && Arrays.equals(pointPrevious, obj.getPointPrevious());
        }

        @Override
        public int compareTo(PathCalculatorGraph.IDistance<String, String> o) {
            return Double.compare(getCostsAcc(), o.getCostsAcc());
        }
    }

    private static class CCInsertion extends CCAbstract {

        public CCInsertion(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int x = point[1] + 1;
            if (x >= refs.size()) {
                return null;
            }
            if (isLineBreakRef(x)) {
                final int y = point[0];
                if (isLineBreakReco(y)) {
                    int[] next = new int[]{point[0], x};
                    return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, dist.getCostsAcc(), null, refs.get(x), point, next);
                }
                return null;
            }
            final String part = refs.get(x);
            int[] next = new int[]{point[0], x};
            return new DistanceStrStr(DistanceStrStr.TYPE.INS, dist.getCostsAcc() + 1, null, part, point, next);
        }
    }

    private static class CCDeletion extends CCAbstract {

        public CCDeletion(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int y = point[0] + 1;
            if (y >= recos.size()) {
                return null;
            }
            if (isLineBreakReco(y)) {
                final int x = point[1];
                if (isLineBreakRef(x)) {
                    int[] next = new int[]{y, point[1]};
                    return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, dist.getCostsAcc(), recos.get(y), null, point, next);
                }
                return null;
            }
            final String part = recos.get(y);
            int[] next = new int[]{y, point[1]};
            return new DistanceStrStr(DistanceStrStr.TYPE.DEL, dist.getCostsAcc() + 1, part, null, point, next);
        }
    }

    private static class CCSubOrCor extends CCAbstract {

        public CCSubOrCor(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            if (point[0] + 1 < recos.size() && point[1] + 1 < refs.size()) {
                final int y = point[0] + 1;
                final int x = point[1] + 1;
                final String reco = recos.get(y);
                final String ref = refs.get(x);
                final boolean cor = reco.equals(ref);
                int[] next = new int[]{y, x};
                if (cor) {
                    if (isLineBreakReco(y)) {
                        //line break characters are no real characters - so they do not have to be count!
                        return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, dist.getCostsAcc(), null, null, point, next);
                    }
                    //normal case: characters are equal
                    return new DistanceStrStr(DistanceStrStr.TYPE.COR, dist.getCostsAcc(), reco, ref, point, next);
                }
                if (isLineBreakReco(y) || isLineBreakRef(x)) {
                    //if only one of these is a line break: it is not allowed to substitute one character against one line break!
                    return null;
                }
                //normal case: characters are unequal
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.SUB,
                        dist.getCostsAcc() + 1, reco, ref, point, next);
            }
            return null;
        }
    }

    private static class LineLineBreakRecoJump extends CCAbstract implements PathCalculatorGraph.ICostCalculatorMulti<String, String> {

        public LineLineBreakRecoJump(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            throw new NotImplementedException();
        }

        @Override
        public List<PathCalculatorGraph.IDistance<String, String>> getNeighbours(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            int x = point[1] + 1;
            int y = point[0] + 1;
            if (x >= refs.size() || !isLineBreakRef(x) || y >= recos.size() || !isLineBreakReco(y)) {
                return null;
            }
            List<PathCalculatorGraph.IDistance<String, String>> res = new LinkedList<>();
            for (int i = 0; i < this.lineBreaksReco.length; i++) {
                final int target = lineBreaksReco[i];
                if (target != y) {
                    res.add(new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, dist.getCostsAcc(), recos.get(y), refs.get(x), point, new int[]{target, x}));
                }
            }
            return res;
        }
    }

    private String toOneLine(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        if (lines.size() == 1) {
            return lines.get(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            sb.append('\n').append(lines.get(i));
        }
        return sb.toString();
    }

    @Override
    public void calculate(List<String> reco, List<String> ref) {
        if (reco.size() == ref.size()) {
            for (int i = 0; i < ref.size(); i++) {
                calculate(reco.get(i), ref.get(i));
            }
        } else {
            calculate(toOneLine(reco), toOneLine(ref));
        }
    }

    private static class RecoRef {

        private String[] recos;
        private String[] refs;

        public RecoRef(String[] recos, String[] refs) {
            this.recos = recos;
            this.refs = refs;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Arrays.deepHashCode(this.recos);
            hash = 29 * hash + Arrays.deepHashCode(this.refs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RecoRef other = (RecoRef) obj;
            if (!Arrays.deepEquals(this.recos, other.recos)) {
                return false;
            }
            if (!Arrays.deepEquals(this.refs, other.refs)) {
                return false;
            }
            return true;
        }

    }

}
