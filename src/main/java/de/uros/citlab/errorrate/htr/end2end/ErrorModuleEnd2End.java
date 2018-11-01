/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.errorrate.util.GroupUtil;
import de.uros.citlab.errorrate.util.HeatMapUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.errorrate.util.VectorUtil;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import eu.transkribus.languageresources.extractor.pagexml.PAGEXMLExtractor;
import gnu.trove.TIntDoubleHashMap;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

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
    private double filterOffset;

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
        this(tokenizer, stringNormalizer, mode, detailed, 300);
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, Boolean detailed, double filterOffset) {
        this.filterOffset = filterOffset;
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
        int grid = 20;
        switch (mode) {
            case NO_RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
                pathCalculator.addCostCalculator(new CCInsNL(voter));
                pathCalculator.addCostCalculator(new CCDelNL(voter));
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) new CCLineBreakRecoJump(voter));
//                if (filterOffset > 0.0) {
//                    pathCalculator.setFilter(new FilterHorizontalFixedLength(20, grid));
//                }
                break;
            case NO_RO:
                CCLineBreakRecoJump jumper = new CCLineBreakRecoJump(voter);
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) jumper);
                if (filterOffset > 0.0) {
                    pathCalculator.setFilter(jumper);
                }
                break;
            case RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
                pathCalculator.addCostCalculator(new CCInsNL(voter));
                pathCalculator.addCostCalculator(new CCDelNL(voter));
            case RO:
                pathCalculator.addCostCalculator(new CCDelLine(voter));//this cost calculator is not needed for IGNORE_READINGORDER because CCLineBreakRecoJump is cheaper anyway
                if (filterOffset > 0.0) {
                    pathCalculator.setFilter(new FilterHorizontalFixedLength(filterOffset, grid));
                }
                break;
            default:
                throw new RuntimeException("not implemented yet");
        }
    }

    private static class FilterHorizontal implements PathCalculatorGraph.PathFilter<String, String> {
        private HashMap<Integer, Double> map = new LinkedHashMap<>();
        private final double offset;

        private FilterHorizontal(double offset) {
            this.offset = offset;
        }

        @Override
        public void setComparator(Comparator<PathCalculatorGraph.IDistance<String, String>> comparator) {
            map.clear();
        }

        @Override
        public void init(String[] strings, String[] strings2) {
        }

        @Override
        public boolean addNewEdge(PathCalculatorGraph.IDistance<String, String> newDistance) {
            String[] refs = newDistance.getReferences();
            if (refs == null || refs.length < 1 || !(refs[0].equals(" ") || refs[0].equals("\n"))) {
                return true;
            }
            final int x = newDistance.getPoint()[1];
            Double aDouble = map.get(x);
            final double after = newDistance.getCostsAcc();
            if (aDouble == null) {
                map.put(x, after);
                return true;
            }
            final double before = aDouble.doubleValue();
            if (after < before) {
                map.put(x, after);
                return true;
            }
            if (before + offset < after) {
                return true;
            }
            //before + offset > after : gap is too large!
            return false;
        }

        @Override
        public boolean followPathsFromBestEdge(PathCalculatorGraph.IDistance<String, String> bestDistance) {
            String[] refs = bestDistance.getReferences();
            if (refs == null || refs.length < 1 || !(refs[0].equals(" ") || refs[0].equals("\n"))) {
                return true;
            }
            final int x = bestDistance.getPoint()[1];
            Double aDouble = map.get(x);
            final double after = bestDistance.getCostsAcc();
            if (aDouble == null) {
                map.put(x, after);
                return true;
            }
            final double before = aDouble.doubleValue();
            if (after < before) {
                map.put(x, after);
                return true;
            }
            return after < before + offset;
        }
    }

    public void setProcessViewer(boolean show) {
        pathCalculator.useProgressBar(show);
    }

    public void setFileDynProg(File file) {
        pathCalculator.setFileDynMat(file);
    }

    private static class FilterHorizontalFixedLength implements PathCalculatorGraph.PathFilter<String, String> {
        private TIntDoubleHashMap map = new TIntDoubleHashMap();
        private final double offset;
        private final int grid;

        private FilterHorizontalFixedLength(double offset, int grid) {
            this.offset = offset;
            this.grid = grid;
        }

        @Override
        public void setComparator(Comparator<PathCalculatorGraph.IDistance<String, String>> comparator) {
            map.clear();
        }

        @Override
        public void init(String[] strings, String[] strings2) {

        }

        @Override
        public boolean addNewEdge(PathCalculatorGraph.IDistance<String, String> newDistance) {
            final int x = newDistance.getPoint()[1];
            if (x % grid != 0) {
                return true;
            }
            int key = x / grid;
            if (!map.containsKey(key)) {
                map.put(key, newDistance.getCostsAcc());
                return true;
            }
            final double before = map.get(key);
            double after = newDistance.getCostsAcc();
            if (after < before) {
                map.put(key, after);
                return true;
            }
            if (before + offset < after) {
                return false;
            }
            //before + offset > after : gap is too large!
            return true;
        }

        @Override
        public boolean followPathsFromBestEdge(PathCalculatorGraph.IDistance<String, String> bestDistance) {
            final int x = bestDistance.getPoint()[1];
            int key = x / grid + 1;
            if (!map.containsKey(key)) {
                return true;
            }
            final double before = map.get(key);
            double after = bestDistance.getCostsAcc();
            if (before + offset < after) {
                return false;
            }
            //before + offset > after : gap is too large!
            return true;
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
        calculate(recos, refs, false, null);
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

    private void calculate(String[] recos, String[] refs, boolean showProgressBar, File out) {
        //use dynamic programming to calculate the cheapest path through the dynamic programming tabular
//        calcBestPathFast(recos, refs);
        pathCalculator.setUpdateScheme(PathCalculatorGraph.UpdateScheme.LAZY);
        pathCalculator.useProgressBar(showProgressBar);
        pathCalculator.setFileDynMat(out);
        PathCalculatorGraph.DistanceMat<String, String> mat = pathCalculator.calcDynProg(Arrays.asList(recos), Arrays.asList(refs));
//        pathCalculator.calcBestPath(mat);
        List<PathCalculatorGraph.IDistance<String, String>> calcBestPath = pathCalculator.calcBestPath(mat);
        log(mat, calcBestPath, recos, refs);
        if (calcBestPath == null) {
            //TODO: return maximal error? or Thorow RuntimeException?
            LOG.warn("cannot find path between \n" + Arrays.toString(recos).replace("\n", "\\n") + " and \n" + Arrays.toString(refs).replace("\n", "\\n"));
            throw new RuntimeException("cannot find path (see Logger.warn) for more information");
        }
        for (Count c : Count.values()) {
            counter.add(c, 0L);
        }
        int[] usedReco = getUsedRecos(recos, calcBestPath);
        int max = VectorUtil.max(usedReco);
        //if any reference is used, the resulting array "usedReco" should only conatain zeros.
        if (mode.equals(Mode.RO) || mode.equals(Mode.RO_SEG) || !(max > 1 || countUnusedChars(usedReco, recos) > 0)) {
            ObjectCounter<Count> count = count(calcBestPath, usedReco, countChars(recos), countChars(refs));
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
                        (elements.get(elements.size() - 1).getCostsAcc() - elements.get(0).getCostsAcc() + elements.get(0).getCosts()),
                        elements.get(0).getPointPrevious()[0],
                        elements.get(elements.size() - 1).getPoint()[0],
                        elements.get(0).getPointPrevious()[1],
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
            ErrorModuleEnd2End fallback = new ErrorModuleEnd2End(tokenizer, null, modeFallback, detailed, 0.0);
            File outSubProblem = null;
            if (out != null) {
                String path = out.getPath();
                path = path.substring(0, path.lastIndexOf(".")) + "_" + path.substring(path.lastIndexOf("."));
                outSubProblem = new File(path);
            }
            fallback.calculate(recos, refs, showProgressBar, outSubProblem);
            counter.addAll(fallback.getCounter());
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
            int i = countChars(recos, toDeletePath.startReco, toDeletePath.endReco);
            int j = toDeletePath.endReco - toDeletePath.startReco;
            System.out.println("cntChars = "+i+" toDelete-diff = "+j);
            ObjectCounter<Count> count = count(toDeletePath.path, usedReco, countChars(recos, toDeletePath.startReco, toDeletePath.endReco), countChars(refs, toDeletePath.startRef, toDeletePath.endRef));
//            ObjectCounter<Count> count = count(toDeletePath.path, usedReco, recos, refs);
            LOG.debug("count {} for sub-task {}", count, toDeletePath);
            counter.addAll(count);
        }
        String[] refShorter = getSubProblem(refs, maskRef);
        String[] recosShorter = getSubProblem(recos, maskReco);
        if (countChars(refShorter) == 0) {
            int i = countChars(recosShorter);
            LOG.debug("add {} times DEL, ERR and HYP to count because ref == \"\"", i);
            counter.add(Count.DEL, i);
            counter.add(Count.ERR, i);
            counter.add(Count.HYP, i);
            return;
        }
        File outSubProblem = null;
        if (out != null) {
            String path = out.getPath();
            path = path.substring(0, path.lastIndexOf(".")) + "_" + path.substring(path.lastIndexOf("."));
            outSubProblem = new File(path);
        }

        calculate(recosShorter, refShorter, showProgressBar, outSubProblem);
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

    private ObjectCounter<Count> count(List<PathCalculatorGraph.IDistance<String, String>> path, int[] usedReco, int lengthRecos, int lengthRefs) {
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
        res.add(Count.GT, lengthRefs);
        res.add(Count.HYP, lengthRecos);

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
        return countChars(out, 0, out.length);
    }

    private int countChars(String[] out, int start, int endExcl) {
        int count = 0;
        for (int i = start; i < endExcl; i++) {
            if (!voter.isLineBreak(out[i])) count++;
        }
        return count;
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
        calculate(toOneLine(reco), toOneLine(ref));
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
