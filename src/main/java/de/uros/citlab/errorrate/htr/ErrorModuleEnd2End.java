/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr;

import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import de.uros.citlab.errorrate.util.GroupUtil;
import de.uros.citlab.errorrate.util.HeatMapUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.errorrate.util.VectorUtil;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final Logger LOG = LoggerFactory.getLogger(ErrorModuleEnd2End.class);

    public ErrorModuleEnd2End(ICategorizer categorizer, IStringNormalizer stringNormalizer, Mode mode, Boolean detailed) {
        this(new TokenizerCategorizer(categorizer), stringNormalizer, mode, detailed);
    }

    public enum Mode {
        RO,
        NO_RO,
        RO_SEG,
        NO_RO_SEG,
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, Boolean detailed) {
        this.detailed = detailed;
        this.mode = mode;
        if (tokenizer == null) {
            throw new RuntimeException("no tokenizer given (is null)");
        }
        this.tokenizer = tokenizer;
        this.stringNormalizer = stringNormalizer;
        pathCalculator.addCostCalculator(new CCDel(voter));
        pathCalculator.addCostCalculator(new CCIns(voter));
        pathCalculator.addCostCalculator(new CCSubOrCor(voter));
        pathCalculator.addCostCalculator(new CCInsLine(voter));
        switch (mode) {
            case NO_RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
                pathCalculator.addCostCalculator(new CCInsNL(voter));
                pathCalculator.addCostCalculator(new CCDelNL(voter));
            case NO_RO:
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) new LineLineBreakRecoJump(voter));
                break;
            case RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
                pathCalculator.addCostCalculator(new CCInsNL(voter));
                pathCalculator.addCostCalculator(new CCDelNL(voter));
            case RO:
                pathCalculator.addCostCalculator(new CCDelLine(voter));//this cost calculator is not needed for IGNORE_READINGORDER because LineLineBreakRecoJump is cheaper anyway
                break;
            default:
                throw new RuntimeException("not implemented yet");
        }
    }

    private static class PathQuality {
        private double error;
        private int startReco, endReco;
        private int startRef, endRef;
        private List<PathCalculatorGraph.IDistance<String, String>> path;

        public PathQuality(double error, int startReco, int endReco, int startRef, int endRef, List<PathCalculatorGraph.IDistance<String, String>> path) {
            this.error = error;
            this.startReco = startReco;
            this.endReco = endReco;
            this.startRef = startRef;
            this.endRef = endRef;
            this.path = path;
        }

        @Override
        public String toString() {
            return "error=" + error +
                    ", reco=[" + startReco + "," + endReco + "]" +
                    "ref=[" + startRef + "," + endRef + "]"
                    + ", path=" + getPath(path);
        }

        private String getPath(List<PathCalculatorGraph.IDistance<String, String>> path) {
            StringBuilder sbReco = new StringBuilder();
            StringBuilder sbRef = new StringBuilder();
            for (PathCalculatorGraph.IDistance<String, String> point : path) {
                String[] recos = point.getRecos();
                String[] refs = point.getReferences();
                if (recos != null) {
                    for (String reco : recos) sbReco.append(reco);
                }
                if (refs != null) {
                    for (String ref : refs) sbRef.append(ref);
                }
            }
            return "\"" + sbReco + "\"=>\"" + sbRef + "\"";
        }
    }

    @Override
    public String toString() {
        return "ErrorModuleEnd2End{" +
                "counter=" + counter +
                ", counterSub=" + counterSub +
                ", tokenizer=" + tokenizer +
                ", detailed=" + detailed +
                ", stringNormalizer=" + stringNormalizer +
                ", pathCalculator=" + pathCalculator +
                ", mode=" + mode +
                ", voter=" + voter +
                '}';
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
//        final String recoOrig = reco;
//        final String refOrig = ref;
        //use string normalizer, if set
        if (stringNormalizer != null) {
            reco = stringNormalizer.normalize(reco);
            ref = stringNormalizer.normalize(ref);
        }

        //tokenize both strings
        String[] recos = tokenizer.tokenize(reco).toArray(new String[0]);
        String[] refs = tokenizer.tokenize(ref).toArray(new String[0]);
        calculate(recos, refs);
    }

    private void log(PathCalculatorGraph.DistanceMat<String, String> mat, List<PathCalculatorGraph.IDistance<String, String>> calcBestPath, String[] recos, String[] refs) {
        if (LOG.isTraceEnabled()) {
            double[][] out = new double[mat.getSizeY()][mat.getSizeX()];
            StringBuilder sb = new StringBuilder();
            sb.append("----- --");
            for (int i = 0; i < refs.length; i++) {
                sb.append(String.format(" %2s", refs[i].replace("\n", "\\n")));
            }
            LOG.trace(sb.toString());
            for (int i = 0; i < out.length; i++) {
                double[] outV = out[i];
                StringBuilder sb1 = new StringBuilder();
                sb1.append(i == 0 ? "-----" : String.format("%5s", recos[i - 1].replace("\n", "\\n")));
                for (int j = 0; j < outV.length; j++) {
                    PathCalculatorGraph.IDistance<String, String> dist = mat.get(i, j);
                    sb1.append(String.format(" %2d", (int) (dist == null ? 0 : dist.getCostsAcc() + 0.5)));
                    outV[j] = dist == null ? 0 : dist.getCostsAcc();
                }
                LOG.trace(sb1.toString());
            }

            if (calcBestPath != null) {
                double max = calcBestPath.get(calcBestPath.size() - 1).getCostsAcc();
//            for (int i = 0; i < out.length; i++) {
//                double[] vec = out[i];
//                for (int j = 0; j < vec.length; j++) {
//                    max = Math.max(max, vec[j]);
//                }
//            }
                for (int i = 0; i < out.length; i++) {
                    double[] vec = out[i];
                    for (int j = 0; j < vec.length; j++) {
                        if (vec[j] > max + 1e-6) {
                            vec[j] = max;
                        }
                    }
                }

                max *= 0.5;
                for (PathCalculatorGraph.IDistance<String, String> dist : calcBestPath) {
                    int y = dist.getPoint()[0];
                    int x = dist.getPoint()[1];
                    out[y][x] = out[y][x] + max;
                    LOG.trace(dist.toString());
                }
            }
            HeatMapUtil.save(HeatMapUtil.getHeatMap(out, 3), new File("out.png"));

        }
    }

    private int[] getUsedRecos(String[] recos, List<PathCalculatorGraph.IDistance<String, String>> calcBestPath) {
        //minus 1 because we add a \n
        //TODO: clean handling of \n
        int[] usedReco = new int[recos.length];
        for (PathCalculatorGraph.IDistance<String, String> dist : calcBestPath) {
            String m = dist.getManipulation();
            if (m == null) {
                throw new RuntimeException("manipulation have to be set");
            }
            switch (DistanceStrStr.TYPE.valueOf(m)) {
                case DEL:
                case INS:
                case SUB:
                case COR:
                    if (dist.getRecos() != null) {
                        usedReco[dist.getPoint()[0] - 1] += dist.getRecos().length;
                    }
                    break;
            }
        }
        return usedReco;
    }

    private void calculate(String[] recos, String[] refs) {
        //use dynamic programming to calculate the cheapest path through the dynamic programming tabular
//        calcBestPathFast(recos, refs);
        pathCalculator.setUpdateScheme(PathCalculatorGraph.UpdateScheme.LAZY);
        PathCalculatorGraph.DistanceMat<String, String> mat = pathCalculator.calcDynProg(Arrays.asList(recos), Arrays.asList(refs));

        List<PathCalculatorGraph.IDistance<String, String>> calcBestPath = pathCalculator.calcBestPath(mat);
        log(mat, calcBestPath, recos, refs);
        if (calcBestPath == null) {
            //TODO: return maximal error? or Thorow RuntimeException?
            LOG.warn("cannot find path between " + Arrays.toString(recos) + " and " + Arrays.toString(refs));
            return;
        }
        for (Count c : Count.values()) {
            counter.add(c, 0L);
        }
        int[] usedReco = getUsedRecos(recos, calcBestPath);
        int max = VectorUtil.max(usedReco);
        //if any reference is used, the resulting array "usedReco" should only conatain zeros.
        if (mode.equals(Mode.RO) || mode.equals(Mode.RO_SEG) || !(max > 1 || countUnusedChars(usedReco, recos) > 0)) {
            ObjectCounter<Count> count = count(calcBestPath, usedReco, recos, refs.length, recos.length);
            LOG.debug("count = " + count);
            counter.addAll(count);
            return;
        }
        // 1. group path into line-line-path
        // 2. sort paths according quality
        // 3. count paths, which have a unique reco
        // 4. all other path/recos have to go into the next round!
        List<PathQuality> grouping = GroupUtil.getGrouping(calcBestPath, new GroupUtil.Joiner<PathCalculatorGraph.IDistance<String, String>>() {
            @Override
            public boolean isGroup(List<PathCalculatorGraph.IDistance<String, String>> group, PathCalculatorGraph.IDistance<String, String> element) {
                DistanceStrStr.TYPE typeBefore = DistanceStrStr.TYPE.valueOf(group.get(group.size() - 1).getManipulation());
                switch (DistanceStrStr.TYPE.valueOf(element.getManipulation())) {
                    case COR:
                    case INS:
                    case DEL:
                    case SUB:
                    case DEL_LINE:
//                        case MERGE_LINE:
//                        case SPLIT_LINE:
                        switch (typeBefore) {
                            case COR:
                            case INS:
                            case DEL:
                            case SUB:
                            case DEL_LINE:
//                                case MERGE_LINE:
//                                case SPLIT_LINE:
                                return true;
                        }
                        return false;
                    case INS_LINE:
                    case JUMP_RECO:
                    case MERGE_LINE:
                    case SPLIT_LINE:
                        return false;
                    case COR_LINEBREAK:
                        return false;
                    default:
                        throw new UnsupportedOperationException("cannot interprete " + element.getManipulation() + ".");
                }
            }

            @Override
            public boolean keepElement(PathCalculatorGraph.IDistance<String, String> element) {
                switch (DistanceStrStr.TYPE.valueOf(element.getManipulation())) {
                    case COR:
                    case INS:
                    case DEL:
                    case SUB:
                        return true;
                    case MERGE_LINE:
                    case SPLIT_LINE:
                    case DEL_LINE:
                    case INS_LINE:
                    case JUMP_RECO:
                    case COR_LINEBREAK:
                        return false;
                    default:
                        throw new UnsupportedOperationException("cannot interprete " + element.getManipulation() + ".");
                }
            }
        }, new GroupUtil.Mapper<PathCalculatorGraph.IDistance<String, String>, PathQuality>() {
            @Override
            public PathQuality map(List<PathCalculatorGraph.IDistance<String, String>> elements) {
                return new PathQuality(
                        elements.get(elements.size() - 1).getCostsAcc() - elements.get(0).getCostsAcc() + elements.get(0).getCosts(),
                        elements.get(0).getPoint()[0] - 1,
                        elements.get(elements.size() - 1).getPoint()[0],
                        elements.get(0).getPoint()[1] - 1,
                        elements.get(elements.size() - 1).getPoint()[1],
                        elements);
            }
        });
        grouping.sort(new Comparator<PathQuality>() {
            @Override
            public int compare(PathQuality o1, PathQuality o2) {
                //TODO: better function here - maybe dependent on ref-length or on path-length
                return Double.compare(o1.error, o2.error);
            }
        });
        if (grouping.isEmpty()) {
            //TODO: is that okay?? or should one slowly increase JUMP_RECO-costs?
            //ough - shortest path is only done by JUMP_RECO and INS_LINE - try to map without JUMP_RECO, but with DEL_LINE
            Mode modeFallback = null;
            switch (this.mode) {
                case NO_RO:
                    modeFallback = Mode.RO;
                    break;
                case NO_RO_SEG:
                    modeFallback = Mode.RO_SEG;
                    break;
                default:
                    throw new RuntimeException("not implemented yet");
            }
            ErrorModuleEnd2End intern = new ErrorModuleEnd2End(tokenizer, null, modeFallback, detailed);
            intern.calculate(recos, refs);
            counter.addAll(intern.getCounter());
            return;
        }
        boolean[] maskReco = new boolean[recos.length];
        boolean[] maskRef = new boolean[refs.length];
        for (PathQuality toDeletePath : grouping) {
//            PathQuality toDeletePath = grouping.get(0);
            if (!reduceMask(maskReco, toDeletePath.startReco, toDeletePath.endReco)) {
                continue;
            }
            if (!reduceMask(maskRef, toDeletePath.startRef, toDeletePath.endRef)) {
                throw new RuntimeException("reference should be used only 1 time in bestPath");
            }
            LOG.debug("found multiply usage of path - delete " + toDeletePath);
            ObjectCounter<Count> count = count(toDeletePath.path, usedReco, recos, toDeletePath.endRef - toDeletePath.startRef, toDeletePath.endReco - toDeletePath.startReco);
            LOG.debug("count {} for sub-task {}", count, toDeletePath.path);
            counter.addAll(count);
        }
        String[] refShorter = getSubProblem(refs, maskRef);
        String[] recosShorter = getSubProblem(recos, maskReco);
        if (refShorter.length <= 1) {
            int i = countChars(recosShorter);
            LOG.debug("add {} times DEL, ERR and HYP to count because ref == \"\"", i);
            counter.add(Count.DEL, i);
            counter.add(Count.ERR, i);
            counter.add(Count.HYP, i);
            return;
        }
        calculate(recosShorter, refShorter);
        return;
    }

    private boolean reduceMask(boolean[] mask, int start, int end) {
        for (int i = start; i < end; i++) {
            if (mask[i]) {
                return false;
            }
        }
        for (int i = start; i < end; i++) {
            mask[i] = true;
        }
        return true;
    }

    private String[] getSubProblem(String[] transcripts, boolean[] toDelete) {
        LinkedList<String> res = new LinkedList<>();
        boolean lastWasLineBreak = false;
        for (int i = 0; i < transcripts.length; i++) {
            if (!toDelete[i]) {
                //prevent adding double-linebreaks
                boolean isLineBreak = voter.isLineBreakOrSpace(transcripts[i]);
                if (!lastWasLineBreak || !isLineBreak) {
                    res.add(transcripts[i]);
                }
                lastWasLineBreak = isLineBreak;
            }
        }
        return res.toArray(new String[0]);
    }

    private ObjectCounter<Count> count(List<PathCalculatorGraph.IDistance<String, String>> path, int[] usedReco, String[] recos, int lenRefs, int lenRecos) {
        ObjectCounter<Count> res = new ObjectCounter<>();
        for (PathCalculatorGraph.IDistance<String, String> dist : path) {
            String m = dist.getManipulation();
            if (m == null) {
                continue;
            }
            switch (DistanceStrStr.TYPE.valueOf(m)) {
                case DEL:
                case INS:
                case SUB:
                    res.add(Count.ERR);
                case COR:
                    res.add(Count.valueOf(dist.getManipulation()));
                    break;
                case INS_LINE:
                    res.add(Count.ERR, dist.getReferences().length);
                    res.add(Count.INS, dist.getReferences().length);
                    break;
                case DEL_LINE:
                    res.add(Count.ERR, dist.getRecos().length);
                    res.add(Count.DEL, dist.getRecos().length);
                    break;
                case COR_LINEBREAK:
                case JUMP_RECO:
                case MERGE_LINE:
                case SPLIT_LINE:
                    break;
                default:
                    throw new RuntimeException("found type '" + dist.getManipulation() + "'");
            }
        }
        res.add(Count.GT, lenRefs);
        res.add(Count.HYP, lenRecos);

        return res;

    }

    private int countUnusedChars(int[] usage, String[] out) {
        int count = 0;
        for (int i = 0; i < usage.length; i++) {
            if (usage[i] == 0 && !voter.isLineBreakOrSpace(out[i])) {
                count++;
            }
        }
        return count;
    }

    private int countChars(String[] out) {
        int count = 0;
        for (int i = 0; i < out.length; i++) {
            if (!voter.isLineBreak(out[i])) count++;
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

        private boolean isLineBreakOrSpace(String s) {
            return isSpace(s) || isLineBreak(s);
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

        private DistanceStrStr(TYPE type, double costs, double costAcc, String reco, String ref, int[] pointPrevious, int[] point) {
            this(type, costs, costAcc, reco == null ? null : new String[]{reco}, ref == null ? null : new String[]{ref}, pointPrevious, point);
        }


        private enum TYPE {
            INS, DEL, SUB, COR,
            JUMP_RECO, COR_LINEBREAK,
            INS_LINE, DEL_LINE,
            SPLIT_LINE, MERGE_LINE
        }

        private final TYPE type;
        private final double costs;
        private final double costAcc;
        private final String[] recos;
        private final String[] refs;
        private final int[] pointPrevious;
        private final int[] point;

        @Override
        public String toString() {
            return ("DistanceStrStr{" +
                    "type=" + type +
                    ", costs=" + costs +
                    ", costAcc=" + costAcc +
                    ", recos=" + Arrays.toString(recos) +
                    ", refs=" + Arrays.toString(refs) +
                    ", pointPrevious=" + Arrays.toString(pointPrevious) +
                    ", point=" + Arrays.toString(point) +
                    '}').replace("\n", "\\n");
        }

        public DistanceStrStr(TYPE type, double costs, double costAcc, String[] recos, String[] refs, int[] pointPrevious, int[] point) {
            this.type = type;
            this.costs = costs;
            this.costAcc = costAcc;
            this.recos = recos;
            this.refs = refs;
            this.pointPrevious = pointPrevious;
            this.point = point;
        }

        @Override
        public double getCosts() {
            return costs;
//            switch (type) {
//                case DEL:
//                case INS:
//                case SUB:
//                    return 1;
//                case COR:
//                case COR_LINEBREAK:
//                case JUMP_RECO:
//                    return 0;
//                case DEL_LINE:
//                    return recos.length;
//                case INS_LINE:
//                    return refs.length;
//                default:
//                    throw new RuntimeException("cannot calculate costs");
//            }
        }

        @Override
        public double getCostsAcc() {
            return costAcc;
        }

        @Override
        public String[] getRecos() {
            return recos;
        }

        @Override
        public String[] getReferences() {
            return refs;
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

    private static class CCDelLine extends CCAbstract {
        public CCDelLine(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int start = point[0];
            if (isLineBreakReco(start) && isLineBreakRef(point[1])) {
                int idx = 0;
                while (idx < lineBreaksReco.length) {
                    if (lineBreaksReco[idx] > start) {
                        break;
                    }
                    idx++;
                }
                if (idx == lineBreaksReco.length) {
                    return null;
                }
                final int end = lineBreaksReco[idx];
                String[] strings = recos.subList(start + 1, end).toArray(new String[0]);
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.DEL_LINE,
                        strings.length,
                        dist.getCostsAcc() + strings.length,
                        strings,
                        null,
                        point,
                        new int[]{end, point[1]});
            }
            return null;
        }
    }

    private static class CCInsLine extends CCAbstract {
        public CCInsLine(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int start = point[1];
            if (isLineBreakReco(point[0]) && isLineBreakRef(start)) {
                int idx = 0;
                while (idx < lineBreaksRef.length) {
                    if (lineBreaksRef[idx] > start) {
                        break;
                    }
                    idx++;
                }
                if (idx == lineBreaksRef.length) {
                    return null;
                }
                final int end = lineBreaksRef[idx];
                String[] strings = refs.subList(start + 1, end).toArray(new String[0]);
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.INS_LINE,
                        strings.length,
                        dist.getCostsAcc() + strings.length,
                        null,
                        strings,
                        point,
                        new int[]{point[0], end});
            }
            return null;
        }
    }

    private static class CCIns extends CCAbstract {

        public CCIns(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int x = point[1] + 1;
            if (x >= refs.size()) {
                return null;
            }
            if (isLineBreakRef(x) && x < refs.size() - 1) {
//                final int y = point[0];
//                if (isLineBreakReco(y)) {
//                    int search = x;
//                    while (search < refs.size()) {
//                        if (isLineBreakRef(search)) break;
//                        search++;
//                    }
//                    if (search == refs.size()) {
//                        return null;
//                    }
//                    int[] next = new int[]{point[0], search};
//                    String[] line = new String[search - x + 1];
//                    for (int i = 0; i < line.length; i++) {
//                        line[i] = refs.get(i + x - 1);
//                    }
//                    return new DistanceStrStr(DistanceStrStr.TYPE.INS_LINE, dist.getCostsAcc() + line.length - 1, null, line, point, next);
//                }
                return null;
            }
            final String part = refs.get(x);
            int[] next = new int[]{point[0], x};
            return new DistanceStrStr(DistanceStrStr.TYPE.INS, 1, dist.getCostsAcc() + 1, null, part, point, next);
        }
    }

    private static class CCDel extends CCAbstract {

        public CCDel(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int y = point[0] + 1;
            if (y >= recos.size()) {
                return null;
            }
            if (isLineBreakReco(y)) {
//                final int x = point[1];
//                if (isLineBreakRef(x)) {
//                    int[] next = new int[]{y, point[1]};
//                    return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, dist.getCostsAcc() + 1, recos.get(y), null, point, next);
//                }
                return null;
            }
            final String part = recos.get(y);
            int[] next = new int[]{y, point[1]};
            return new DistanceStrStr(DistanceStrStr.TYPE.DEL, 1, dist.getCostsAcc() + 1, part, null, point, next);
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
                        return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, 0, dist.getCostsAcc(), (String) null, null, point, next);
                    }
                    //normal case: characters are equal
                    return new DistanceStrStr(DistanceStrStr.TYPE.COR, 0, dist.getCostsAcc(), reco, ref, point, next);
                }
                if (isLineBreakReco(y) || isLineBreakRef(x)) {
                    //if only one of these is a line break: it is not allowed to substitute one character against one line break!
                    return null;
                }
                //normal case: characters are unequal
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.SUB,
                        1,
                        dist.getCostsAcc() + 1,
                        reco,
                        ref,
                        point,
                        next);
            }
            return null;
        }
    }

    //TODO: also allow INS and DEL of \n between non-spacing characters? "abcd" <=> "ab\ncd"
    private static class CCSubOrCorNL extends CCAbstract {

        public CCSubOrCorNL(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            final int y = point[0] + 1;
            final int x = point[1] + 1;
            if (y < recos.size() && x < refs.size() && (isLineBreakReco(y) != isLineBreakRef(x)) && (isSpaceReco(y) != isSpaceRef(x))) {

                final String reco = recos.get(y);
                final String ref = refs.get(x);
                return new DistanceStrStr(
                        isLineBreakReco(y) ? DistanceStrStr.TYPE.MERGE_LINE : DistanceStrStr.TYPE.SPLIT_LINE,
                        0,
                        dist.getCostsAcc(),
                        reco,
                        ref,
                        point,
                        new int[]{y, x});
            }
            return null;
        }
    }

    private static class CCInsNL extends CCAbstract {

        public CCInsNL(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            //should only be done between non-spacing and non-LB position in y-dimension and LB position in x-dimension
            final int y1 = point[0];
            final int y2 = point[0] + 1;
            final int x = point[1] + 1;
            if (y2 < recos.size() && x < refs.size() &&
                    !isLineBreakReco(y1) && !isSpaceReco(y1) &&
                    !isLineBreakReco(y2) && !isSpaceReco(y2) &&
                    isLineBreakRef(x)) {

                final String ref = refs.get(x);
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.SPLIT_LINE,
                        0,
                        dist.getCostsAcc(),
                        null,
                        ref,
                        point,
                        new int[]{y1, x});
            }
            return null;
        }
    }

    private static class CCDelNL extends CCAbstract {

        public CCDelNL(Voter voter) {
            super(voter);
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
            //should only be done between non-spacing and non-LB position in y-dimension and LB position in x-dimension
            final int x1 = point[1];
            final int x2 = point[1] + 1;
            final int y = point[0] + 1;
            if (x2 < refs.size() && y < recos.size() &&
                    !isLineBreakRef(x1) && !isSpaceRef(x1) &&
                    !isLineBreakRef(x2) && !isSpaceRef(x2) &&
                    isLineBreakReco(y)) {

                final String reco = recos.get(y);
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.MERGE_LINE,
                        0,
                        dist.getCostsAcc(),
                        reco,
                        null,
                        point,
                        new int[]{y, x1});
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
                    res.add(new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, 0, dist.getCostsAcc(), recos.get(y), refs.get(x), point, new int[]{target, x}));
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
