/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.interfaces.IErrorModuleWithSegmentation;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.interfaces.IPoint;
import de.uros.citlab.errorrate.types.*;
import de.uros.citlab.errorrate.util.GroupUtil;
import de.uros.citlab.errorrate.util.HeatMapUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.errorrate.util.VectorUtil;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Module, which uses the {@link PathCalculatorGraph} to calculateIntern the error
 * rates between tokens. Some other classes are needed, to calculateIntern the error
 * rate. See {@link ITokenizer} and
 * {@link IStringNormalizer} for more details.
 *
 * @author gundram
 */
public class ErrorModuleEnd2End implements IErrorModuleWithSegmentation {
    private final ObjectCounter<Count> counter = new ObjectCounter<>();
    private final ObjectCounter<RecoRef> substitutionCounter = new ObjectCounter<>();
    private final ITokenizer tokenizer;
    private final CountSubstitutions countManipulations;
    private final IStringNormalizer stringNormalizer;
    private final PathCalculatorGraph<String, String> pathCalculator = new PathCalculatorGraph<>();
    private final Mode mode;
    private final double filterOffset;
    private final Voter voter = new Voter();
    private static final Logger LOG = LoggerFactory.getLogger(ErrorModuleEnd2End.class);
    private int sizeProcessViewer = -1;
    private File fileDynProg = null;
    private PathFilterBaselineMatch filter = null;
    private boolean usePolygons;
    private double thresholdCouverage = 0.0;

    public enum CountSubstitutions {
        OFF(false, false),
        ERRORS(false, true),
        ALL(true, true);
        public boolean countAll;
        public boolean countSubstitutions;

        CountSubstitutions(boolean countAll, boolean countSubstitutions) {
            this.countAll = countAll;
            this.countSubstitutions = countSubstitutions;
        }
    }

    public enum Mode {
        RO(false, false),
        NO_RO(true, false),
        RO_SEG(false, true),
        NO_RO_SEG(true, true);

        private boolean ignoreReadingOrder;
        private boolean ignoreSegmentation;

        Mode(boolean ignoreReadingOrder, boolean ignoreSegmentation) {
            this.ignoreReadingOrder = ignoreReadingOrder;
            this.ignoreSegmentation = ignoreSegmentation;
        }

        public boolean ignoreReadingOrder() {
            return ignoreReadingOrder;
        }

        public boolean ignoreSegmentation() {
            return ignoreSegmentation;
        }

    }


    /**
     * @param categorizer
     * @param stringNormalizer
     * @param mode
     * @param usePolygons
     * @param countManipulations
     */
    public ErrorModuleEnd2End(ICategorizer categorizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, CountSubstitutions countManipulations) {
        this(new TokenizerCategorizer(categorizer), stringNormalizer, mode, usePolygons, countManipulations, 100);
    }

    public ErrorModuleEnd2End(Mode mode, boolean usePolygons, CountSubstitutions countManipulations) {
        this(new CategorizerCharacterDft(), null, mode, usePolygons, countManipulations);
    }

    public ErrorModuleEnd2End(ICategorizer categorizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, CountSubstitutions countManipulations, double filterOffset) {
        this(new TokenizerCategorizer(categorizer), stringNormalizer, mode, usePolygons, countManipulations, filterOffset);
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, CountSubstitutions countManipulations) {
        this(tokenizer, stringNormalizer, mode, usePolygons, countManipulations, 100);
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, CountSubstitutions countManipulations, double filterOffset) {
        this.usePolygons = usePolygons;
        this.filterOffset = filterOffset;
        if (countManipulations == null) {
            throw new RuntimeException("countManipulations have to be one of " + Arrays.asList(CountSubstitutions.values()));
        }
        this.countManipulations = countManipulations;
        this.mode = mode;
        if (tokenizer == null) {
            throw new RuntimeException("no tokenizer given (is null)");
        }
        this.tokenizer = tokenizer;
        this.stringNormalizer = stringNormalizer;
        pathCalculator.addCostCalculator(new CCDel(voter));
        pathCalculator.addCostCalculator(new CCIns(voter));
        pathCalculator.addCostCalculator(new CCSubOrCor(voter));
        int grid = 20;
        PathCalculatorGraph.PathFilter filter = null;
        switch (mode) {
            case NO_RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
                if (usePolygons) {
                    pathCalculator.addCostCalculator(new CCInsLine(voter));
                }
                CCLineBreakAndSpaceRecoJump ccLineBreakAndSpaceRecoJump = new CCLineBreakAndSpaceRecoJump(voter);
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) ccLineBreakAndSpaceRecoJump);
                if (filterOffset > 0.0) {
                    filter = ccLineBreakAndSpaceRecoJump;
                }
                break;
            case NO_RO:
                CCLineBreakRecoJump jumper = new CCLineBreakRecoJump(voter);
                if (usePolygons) {
                    pathCalculator.addCostCalculator(new CCInsLine(voter));
                }
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) jumper);
                if (filterOffset > 0.0) {
                    filter = jumper;
                }
                break;
            case RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
            case RO:
                pathCalculator.addCostCalculator(new CCInsLine(voter));
                pathCalculator.addCostCalculator(new CCDelLine(voter, mode.equals(Mode.RO)));//this cost calculator is not needed for IGNORE_READINGORDER because CCLineBreakRecoJump is cheaper anyway
                if (filterOffset > 0.0) {
                    filter = new FilterHorizontalFixedLength(filterOffset, grid);
                }
                break;
            default:
                throw new RuntimeException("not implemented yet");
        }
        if (usePolygons) {
            this.filter = new PathFilterBaselineMatch(filter);
            pathCalculator.setFilter(this.filter);
        } else {
            this.filter = null;
            pathCalculator.setFilter(filter);
        }
    }

    /**
     * If base lines are used, two text lines can be assigned to each other, if their couverage is above this threshold.
     * default: 0.0.
     *
     * @param thresholdCouverage threshold [0.0,1.1)
     */
    public void setThresholdCouverage(double thresholdCouverage) {
        this.thresholdCouverage = thresholdCouverage;
    }

    /**
     * show dynamic programming heat map
     *
     * @param sizeImage -1 = off, >0 size of image
     */
    public void setSizeProcessViewer(int sizeImage) {
        sizeProcessViewer = sizeImage;
    }

    /**
     * if heat map of dynamic programming should be saved
     *
     * @param file output dir (for subproblems an "_" is appended)
     */
    public void setFileDynProg(File file) {
        fileDynProg = file;
    }


    @Override
    public String toString() {
        return "ErrorModuleEnd2End{" +
                "counter=" + counter +
                ", substitutionCounter=" + substitutionCounter +
                ", tokenizer=" + tokenizer +
                ", countManipulations=" + countManipulations +
                ", stringNormalizer=" + stringNormalizer +
                ", pathCalculator=" + pathCalculator +
                ", mode=" + mode +
                ", voter=" + voter +
                '}';
    }

    @Override
    public Map<Metric, Double> getMetrics() {
        Result res = new Result(Method.CER);
        res.addCounts(counter);
        return res.getMetrics();
    }

    @Override
    public List<ILineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComarison) {
        return calculate(toOneLine(reco), toOneLine(ref), calcLineComarison);
    }

    @Override
    public void calculateWithSegmentation(List<ILine> reco, List<ILine> ref) {
        calculateWithSegmentation(reco, ref, false);
    }

    @Override
    public void calculate(String reco, String ref) {
        calculate(reco, ref, false);
    }

    @Override
    public void calculate(List<String> reco, List<String> ref) {
        calculate(toOneLine(reco), toOneLine(ref), false);
    }

    @Override
    public List<ILineComparison> calculateWithSegmentation(List<ILine> reco, List<ILine> ref, boolean calcLineComarison) {
        AlignmentTask lmr = new AlignmentTask(reco, ref, tokenizer, stringNormalizer, thresholdCouverage);
        return calculateIntern(lmr, sizeProcessViewer, fileDynProg, calcLineComarison);
    }

    /**
     * normalize and tokenize both inputs. Afterwards find the cheapest cost to
     * manipulate the recognition tokens to come to the reference tokens. Count
     * the manipulation which had to be done.
     *
     * @param reco hypothesis
     * @param ref  reference
     */
    @Override
    public List<ILineComparison> calculate(String reco, String ref, boolean calcLineComparison) {
//        final String recoOrig = reco;
//        final String refOrig = ref;
        //use string normalizer, if set
        if (stringNormalizer != null) {
            reco = stringNormalizer.normalize(reco);
            ref = stringNormalizer.normalize(ref);
        }

        //tokenize both strings
        List<String> recoList = tokenizer.tokenize(reco);
        recoList.add(0, "\n");
        recoList.add("\n");
        String[] recos = recoList.toArray(new String[0]);
        List<String> refList = tokenizer.tokenize(ref);
        refList.add(0, "\n");
        refList.add("\n");
        String[] refs = refList.toArray(new String[0]);
        AlignmentTask result = new AlignmentTask(recos, refs);
        return calculateIntern(result, sizeProcessViewer, fileDynProg, calcLineComparison);
    }

    private List<ILineComparison> calculateIntern(AlignmentTask alignmentTask, int sizeProcessViewer, File out, boolean calcLineComparison) {
        PathCountResult pathCountResult = getPathCount(alignmentTask, sizeProcessViewer, out, calcLineComparison);
        ObjectCounter<Count> countActual = pathCountResult.oc;
        counter.addAll(countActual);
        if (pathCountResult.recoref != null) {
            substitutionCounter.addAll(pathCountResult.recoref);
        }
        counter.set(Count.ERR, counter.get(Count.INS) + counter.get(Count.DEL) + counter.get(Count.SUB));
        return pathCountResult.lineComparisons;
    }

    private static class PathQuality {
        private double error;
        private int startReco, endReco;
        private int startRef, endRef;
        private List<PathCalculatorGraph.IDistance<String, String>> path;
        private final String manipulation;

        public PathQuality(double error, int startReco, int endReco, int startRef, int endRef, List<PathCalculatorGraph.IDistance<String, String>> path) {
            this.error = error;
            this.startReco = startReco;
            this.endReco = endReco;
            this.startRef = startRef;
            this.endRef = endRef;
            this.path = path;
            manipulation = path.get(0).getManipulation();
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
            return ("\"" + sbReco + "\"=>\"" + sbRef + "\"").replace("\n", "\\n");
        }

        private boolean isSplit() {
            return path.size() == 1 && path.get(0).getManipulation().equals(DistanceStrStr.TYPE.SPLIT_LINE.toString());
        }

        private boolean isMerge() {
            return path.size() == 1 && path.get(0).getManipulation().equals(DistanceStrStr.TYPE.MERGE_LINE.toString());
        }

        private boolean isSplitOrMerge() {
            if (path.size() != 1) {
                return false;
            }
            String manipulation = path.get(0).getManipulation();
            return manipulation.equals(DistanceStrStr.TYPE.MERGE_LINE.toString()) || manipulation.equals(DistanceStrStr.TYPE.SPLIT_LINE);
        }

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
                    PathCalculatorGraph.DistanceSmall dist = mat.get(i, j);
                    sb1.append(String.format(" %2d", (int) (dist == null ? -1 : dist.costsAcc + 0.99)));
                    outV[j] = dist == null ? -1 : dist.costsAcc;
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
        //TODO: clean handling of \n and INS
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


    private static class PathCountResult {
        private ObjectCounter<Count> oc;
        private ObjectCounter<RecoRef> recoref;
        private List<ILineComparison> lineComparisons;

        public PathCountResult() {
            this(new ObjectCounter<>(), new ObjectCounter<>(), new LinkedList<>());
        }

        public PathCountResult(ObjectCounter<Count> oc, ObjectCounter<RecoRef> recoref, List<ILineComparison> lineComparisons) {
            this.oc = oc;
            this.recoref = recoref;
            this.lineComparisons = lineComparisons;
        }

        void add(RecoRef recoRef, Count... count) {
            this.recoref.add(recoRef);
            for (Count c : count) {
                oc.add(c);
            }
        }

        void add(ILineComparison lineComparison) {
            lineComparisons.add(lineComparison);
        }

        void addAll(PathCountResult toAdd) {
            oc.addAll(toAdd.oc);
            recoref.addAll(toAdd.recoref);
            if (toAdd.lineComparisons != null) {
                lineComparisons.addAll(toAdd.lineComparisons);
            }
        }
    }

    // 1. group path into line-line-path
    // 2. sort paths according quality
    // 3. count paths, which have a unique reco
    // 4. all other path/recos have to go into the next round!
    private List<PathQuality> getAndSortGroups(List<PathCalculatorGraph.IDistance<String, String>> path) {
        List<PathQuality> grouping = GroupUtil.getGrouping(path, new GroupUtil.Joiner<PathCalculatorGraph.IDistance<String, String>>() {
            @Override
            public boolean isGroup(List<PathCalculatorGraph.IDistance<String, String>> group, PathCalculatorGraph.IDistance<String, String> element) {
                switch (DistanceStrStr.TYPE.valueOf(element.getManipulation())) {
                    case COR:
                    case INS:
                    case DEL:
                    case SUB:
//                    case DEL_LINE:
                    {
                        DistanceStrStr.TYPE typeBefore = DistanceStrStr.TYPE.valueOf(group.get(group.size() - 1).getManipulation());
                        switch (typeBefore) {
                            case COR:
                            case INS:
                            case DEL:
                            case SUB:
//                            case DEL_LINE:
                                return true;
                        }
                        return false;
                    }
                    case INS_LINE:
                    case DEL_LINE:
                        //{
//                        return DistanceStrStr.TYPE.valueOf(group.get(group.size() - 1).getManipulation()).equals(DistanceStrStr.TYPE.INS_LINE);
//                    }
                    case MERGE_LINE:
                    case SPLIT_LINE:
                    case JUMP_RECO:
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
                    case MERGE_LINE:
                    case INS_LINE:
                    case SPLIT_LINE:
                    case DEL_LINE:
                        return true;
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
                switch (DistanceStrStr.TYPE.valueOf(elements.get(0).getManipulation())) {
                    case INS_LINE: {
                        PathCalculatorGraph.IDistance<String, String> el = elements.get(0);
                        return new PathQuality(
                                (el.getCosts()),
                                el.getPoint()[0] - 1,
                                el.getPoint()[0] - 1,
                                el.getPointPrevious()[1],//+1-1=0
                                el.getPoint()[1] - 1,
                                elements);
                    }
                    case DEL_LINE: {
                        PathCalculatorGraph.IDistance<String, String> el = elements.get(0);
                        return new PathQuality(
                                (el.getCosts()),
                                el.getPointPrevious()[0],//+1-1=0
                                el.getPoint()[0] - 1,
                                el.getPoint()[1] - 1,
                                el.getPoint()[1] - 1,
                                elements);
                    }
                    default:
                        int startRef = -1;
                        int startReco = -1;
                        int endRef = -1;
                        int endReco = -1;
                        for (PathCalculatorGraph.IDistance<String, String> e : elements) {
                            if (!e.getManipulation().equals(DistanceStrStr.TYPE.INS.toString())) {//then SUB;COR;DEL;SPLIT;MERGE
                                if (startReco < 0) {
                                    startReco = e.getPoint()[0] - 1;
                                }
                                endReco = e.getPoint()[0] - 1;
                            }
                            if (!e.getManipulation().equals(DistanceStrStr.TYPE.DEL.toString())) {//then SUB;COR;INS;SPLIT;MERGE
                                if (startRef < 0) {
                                    startRef = e.getPoint()[1] - 1;
                                }
                                endRef = e.getPoint()[1] - 1;
                            }
                        }
                        if (startReco < 0 || startRef < 0 || endReco < 0 || endRef < 0) {
                            throw new RuntimeException("path " + elements + " only contains INS and DEL");
                        }
                        return new PathQuality(
                                (elements.get(elements.size() - 1).getCostsAcc() - elements.get(0).getCostsAcc() + elements.get(0).getCosts()),
                                startReco,
                                endReco,
                                startRef,
                                endRef,
                                elements);
                }
            }
        });
        grouping.sort(new Comparator<PathQuality>() {
            @Override
            public int compare(PathQuality o1, PathQuality o2) {
                //TODO: better function here - maybe dependent on ref-length or on path-length
                return o1.isSplitOrMerge() != o2.isSplitOrMerge() ?
                        o1.isSplitOrMerge() ?
                                1 :
                                -1 :
                        Double.compare((o1.error + 0.01) / Math.max(1, o1.path.size()), (o2.error + 0.01) / Math.max(1, o2.path.size()));
            }
        });
        return grouping;
    }

    private static class GroupCountResult {
        private PathCountResult res;
        private boolean[] maskReco;
        private boolean[] maskRef;

        public GroupCountResult(PathCountResult res, boolean[] maskReco, boolean[] maskRef) {
            this.res = res;
            this.maskReco = maskReco;
            this.maskRef = maskRef;
        }
    }

    private GroupCountResult getGroupCount(List<PathQuality> grouping, AlignmentTask alignmentTask, boolean countLineInsertions, boolean calcLineComparison) {
        PathCountResult res = new PathCountResult();
        boolean[] maskReco = new boolean[alignmentTask.getRecos().length];
        boolean[] maskRef = new boolean[alignmentTask.getRefs().length];
        for (PathQuality toDeletePath : grouping) {
            switch (DistanceStrStr.TYPE.valueOf(toDeletePath.manipulation)) {
                case MERGE_LINE: {
                    int startRef = toDeletePath.startRef;
                    if (maskRef[startRef]) {
                        throw new RuntimeException("subpath " + toDeletePath + " should be used only 1 time in bestPath");
                    }
                    int nextLeft = startRef - 1;
                    int nextRight = startRef + 1;
                    while (nextLeft >= 0 && maskRef[nextLeft]) {
                        nextLeft--;
                    }
                    while (nextRight < maskReco.length && maskRef[nextRight]) {
                        nextRight++;
                    }
                    //only if there is one line break remaining after deleting: delete this one to avoid multiply spaces and line breaks
                    if (voter.isLineBreakOrSpace(alignmentTask.getRefs()[nextLeft]) || voter.isLineBreakOrSpace(alignmentTask.getRefs()[nextRight])) {
                        if (!reduceMask(maskRef, toDeletePath.startRef, toDeletePath.endRef)) {
                            throw new RuntimeException("reference should be used only 1 time in bestPath");
                        } else {
                            LOG.trace("do not delete merge-path because it seperates two words");
                        }
                        res.addAll(getPathCounts(toDeletePath.path, alignmentTask, false));
                    }
                    break;
                }
                case SPLIT_LINE:
                    //nothing to do: Splits only occur if HYP=" " and GT="\n" -> nothing have to be count.
                    break;
                case DEL_LINE:
                    if (!reduceMask(maskReco, toDeletePath.startReco, toDeletePath.endReco)) {
                        LOG.debug("skip count of subpath {}, add for next round", toDeletePath);
                        continue;
                    }
                    LOG.debug("add count of subpath {}", toDeletePath);
                    res.addAll(getPathCounts(toDeletePath.path, alignmentTask, calcLineComparison));
                    break;
                case INS_LINE:
                    if (mode.ignoreReadingOrder()) {
                        // nothing to do here. This is only the best solution, if recos are empty.
                        // Otherwise 1 SUB is better than 1 INS + 1 DEL
                        if (!countLineInsertions) {
                            break;
                        }
                    }
                default:
                    if (!reduceMask(maskReco, toDeletePath.startReco, toDeletePath.endReco)) {
                        LOG.debug("skip count of subpath {}, add for next round", toDeletePath);
                        continue;
                    }
                    if (!reduceMask(maskRef, toDeletePath.startRef, toDeletePath.endRef)) {
                        throw new RuntimeException("reference should be used only 1 time in bestPath");
                    }
                    LOG.debug("add count of subpath {}", toDeletePath);
                    res.addAll(getPathCounts(toDeletePath.path, alignmentTask, calcLineComparison));
            }
        }
        return new GroupCountResult(res, maskReco, maskRef);

    }

    private PathCountResult getPathCount(AlignmentTask alignmentTask, int sizeProcessViewer, File out, boolean calcLineComparison) {
        //use dynamic programming to calculateIntern the cheapest path through the dynamic programming tabular
//        calcBestPathFast(recos, refs);
        String[] recos = alignmentTask.getRecos();
        String[] refs = alignmentTask.getRefs();
        if (countChars(recos) == 0) {
            PathCountResult pathCountResult = new PathCountResult();
            for (int i = 0; i < refs.length; i++) {
                if (!voter.isLineBreak(refs[i])) {
                    pathCountResult.add(new RecoRef("", refs[i]), Count.GT, Count.INS);
                    if (calcLineComparison) {
                        pathCountResult.add(
                                getLineComparison(
                                        -1,
                                        alignmentTask.getRefLineMap()[i],
                                        "",
                                        refs[i],
                                        Arrays.asList(new Distance(Manipulation.INS, "", refs[i]))
                                )
                        );
                    }
                }
            }
            return pathCountResult;
        }
        if (filter != null) {
            filter.setAlignmentTask(alignmentTask);
        }
        pathCalculator.setUpdateScheme(PathCalculatorGraph.UpdateScheme.LAZY);
        pathCalculator.setSizeProcessViewer(sizeProcessViewer);
        pathCalculator.setFileDynMat(out);
        PathCalculatorGraph.DistanceMat<String, String> mat = pathCalculator.calcDynProg(recos, refs);
//        pathCalculator.calcBestPath(mat);
        List<PathCalculatorGraph.IDistance<String, String>> calcBestPath = pathCalculator.calcBestPath(mat);
        log(mat, calcBestPath, recos, refs);
        if (calcBestPath == null) {
            //TODO: return maximal error or throw RuntimeException?
            LOG.error("cannot find path between \n" + Arrays.toString(recos).replace("\n", "\\n") + " and \n" + Arrays.toString(refs).replace("\n", "\\n"));
            throw new RuntimeException("cannot find path (see Logger.warn) for more information");
        }
        List<PathQuality> grouping = getAndSortGroups(calcBestPath);
        //if any reference is used, the resulting array "usedReco" should only conatain ones and zeros.
        int[] usedReco = getUsedRecos(recos, calcBestPath);
        int max = VectorUtil.max(usedReco);
        //no post process have to be done - everything is already used
        boolean recoHasUnusedChars = countUnusedChars(usedReco, recos) != 0;
        if (!mode.ignoreReadingOrder() || (max <= 1 && !recoHasUnusedChars)) {
            return getGroupCount(grouping, alignmentTask, !recoHasUnusedChars, calcLineComparison).res;
        }
        GroupCountResult groupCount = getGroupCount(grouping, alignmentTask, false, calcLineComparison);
        boolean[] maskReco = groupCount.maskReco;
        boolean[] maskRef = groupCount.maskRef;
        PathCountResult res = groupCount.res;
        if (grouping.isEmpty() || res.oc.isEmpty()) {
            File outSubProblem = null;
            if (out != null) {
                String path = out.getPath();
                path = path.substring(0, path.lastIndexOf(".")) + "_" + path.substring(path.lastIndexOf("."));
                outSubProblem = new File(path);
            }
            res.addAll(getPathCountFallback(alignmentTask, sizeProcessViewer, outSubProblem, calcLineComparison));
            return res;
        }
        Pair<String[], int[]> refSubProblemTranscription = getSubProblemTranscription(refs, maskRef, alignmentTask.getRefLineMap());
        Pair<String[], int[]> recoSubProblemTranscription = getSubProblemTranscription(recos, maskReco, alignmentTask.getRecoLineMap());
        //if reference length of subproblem is 0, any recognition have to be deleted.
        // Count as HYP and DEL - but skip spaces, if mode is ignoreSegmentation (artificially segment at spaces => no count)
        String[] refSubProblem = refSubProblemTranscription.getFirst();
        String[] recoSubProblem = recoSubProblemTranscription.getFirst();
        if (countChars(refSubProblem) == 0) {
            for (int i = 0; i < recoSubProblem.length; i++) {
                String s = recoSubProblem[i];
                if (!voter.isLineBreak(s)) {
                    if (mode.ignoreSegmentation() && voter.isSpace(s)) {
                        //allow any partition of text - then it is better to substitute spaces by newlines. Do not count spaces.
                        continue;
                    }
                    res.add(new RecoRef(new String[]{s}, new String[0]), Count.HYP, Count.DEL);
                }
            }
            return res;
        }
        //if subproblem is not trivial, solve it recursively.
        File outSubProblem = null;
        AlignmentTask subProblem = new AlignmentTask(recoSubProblemTranscription, refSubProblemTranscription, alignmentTask.getAdjazent());
        if (LOG.isTraceEnabled()) {
            LOG.trace("calculate subproblem - original problem:");
            {
                StringBuilder sbi = new StringBuilder();
                StringBuilder sbt = new StringBuilder();
                StringBuilder sbd = new StringBuilder();
                int length = refs.length < 100 ? 3 : refs.length < 1000 ? 4 : 5;
                for (int i = 0; i < refs.length; i++) {
                    sbi.append(String.format("%" + length + "d", i));
                    sbt.append(String.format("%" + length + "s", refs[i].replace("\n", "\\n")));
                    sbd.append(String.format("%" + length + "s", maskRef[i] ? "t" : "f"));
                }
                LOG.trace("REFERENCE:");
                LOG.trace(sbi.toString());
                LOG.trace(sbt.toString());
                LOG.trace(sbd.toString());
            }
            {
                StringBuilder sbi = new StringBuilder();
                StringBuilder sbt = new StringBuilder();
                StringBuilder sbd = new StringBuilder();
                int length = recos.length < 100 ? 3 : recos.length < 1000 ? 4 : 5;
                for (int i = 0; i < recos.length; i++) {
                    sbi.append(String.format("%" + length + "d", i));
                    sbt.append(String.format("%" + length + "s", recos[i].replace("\n", "\\n")));
                    sbd.append(String.format("%" + length + "s", maskReco[i] ? "t" : "f"));
                }
                LOG.trace("RECOGNITION:");
                LOG.trace(sbi.toString());
                LOG.trace(sbt.toString());
                LOG.trace(sbd.toString());
            }
        }

        PathCountResult pathCountResult = getPathCount(subProblem, sizeProcessViewer, out, calcLineComparison);
        res.addAll(pathCountResult);
        return res;
    }

    private PathCountResult getPathCountFallback(AlignmentTask alignmentTask, int sizeProcessViewer, File out, boolean calcLineComparison) {
        Mode modeFallback = null;
        switch (this.mode) {
            case NO_RO:
                modeFallback = Mode.RO;
                break;
            case NO_RO_SEG:
                modeFallback = Mode.RO_SEG;
                break;
            default:
                throw new RuntimeException("no fallback for mode having reading order");
        }
        ErrorModuleEnd2End fallback = new ErrorModuleEnd2End((ICategorizer) null, null, modeFallback, usePolygons, countManipulations, filterOffset);
        File outSubProblem = null;
        if (out != null) {
            String path = out.getPath();
            path = path.substring(0, path.lastIndexOf(".")) + "_" + path.substring(path.lastIndexOf("."));
            outSubProblem = new File(path);
        }
        return fallback.getPathCount(alignmentTask, sizeProcessViewer, outSubProblem, calcLineComparison);

    }

    private AlignmentTask getSubProblemAlignmentTask(AlignmentTask original, boolean[] maskRef, boolean[] maskReco) {
        AlignmentTask res = new AlignmentTask(
                getSubProblemTranscription(original.getRecos(), maskReco, original.getRecoLineMap()),
                getSubProblemTranscription(original.getRefs(), maskRef, original.getRefLineMap()),
                original.getAdjazent());
        return res;
    }

    private boolean reduceMask(boolean[] mask, int start, int end) {
        for (int i = start; i < end + 1; i++) {
            if (mask[i]) {
                return false;
            }
        }
        for (int i = start; i < end + 1; i++) {
            mask[i] = true;
        }
        return true;
    }
//    private int cnt = 0;

    private Pair<String[], int[]> getSubProblemTranscription(String[] transcripts, boolean[] toDelete, int[] lineIdxs) {
        LinkedList<String> res = new LinkedList<>();
        LinkedList<Integer> res2 = new LinkedList<>();
//        cnt = 0;
        for (int i = 0; i < transcripts.length; i++) {
            if (!toDelete[i] || voter.isLineBreak(transcripts[i])) {
                res.add(transcripts[i]);
                if (lineIdxs != null) {
                    res2.add(lineIdxs[i]);
                }
            }
        }
        for (int i = res.size() - 1; i > 0; i--) {
            if (voter.isLineBreakOrSpace(res.get(i)) && voter.isLineBreakOrSpace(res.get(i - 1))) {
                int idx = voter.isSpace(res.get(i)) ? i : i - 1;
//                String s = res.get(idx);
//                if(voter.isSpace(s)){
//                    cnt++;
//                }
                res.remove(idx);
                if (lineIdxs != null) {
                    res2.remove(idx);
                }
            }
        }
        if (lineIdxs == null) {
            return new Pair<>(res.toArray(new String[0]), null);
        }
        int[] idxs = new int[res2.size()];
        for (int i = 0; i < res2.size(); i++) {
            idxs[i] = res2.get(i);
        }
        return new Pair<>(res.toArray(new String[0]), idxs);
    }

    private ILineComparison getLineComparison(int recoIndex, int refIndex, String recoText, String refText, List<IPoint> path) {
        return new ILineComparison() {
            @Override
            public int getRecoIndex() {
                return recoIndex;
            }

            @Override
            public int getRefIndex() {
                return refIndex;
            }

            @Override
            public String getRefText() {
                return refText;
            }

            @Override
            public String getRecoText() {
                return recoText;
            }

            @Override
            public List<IPoint> getPath() {
                return path;
            }

            @Override
            public String toString() {
                return String.format("[%2d,%2d]: '%s'=>'%s' %s", recoIndex, refIndex, recoText == null ? "" : recoText, refText == null ? "" : refText, path);
            }
        };

    }

    private PathCountResult getPathCounts(List<PathCalculatorGraph.IDistance<String, String>> path, AlignmentTask alignmentTask, boolean calcLineComparison) {
        ObjectCounter<Count> res = new ObjectCounter<>();
        ObjectCounter<RecoRef> res2 = new ObjectCounter<>();
//        int cnt = 0;
        for (PathCalculatorGraph.IDistance<String, String> dist : path) {
            String m = dist.getManipulation();
            if (m == null) {
                continue;
            }
            switch (DistanceStrStr.TYPE.valueOf(m)) {
                case DEL:
                    if (countManipulations.countSubstitutions) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    res.add(Count.HYP);
                    res.add(Count.DEL);
                    break;
                case INS:
                    if (countManipulations.countSubstitutions) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    res.add(Count.GT);
                    res.add(Count.INS);
                    break;
                case SUB:
                    if (countManipulations.countSubstitutions) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    res.add(Count.HYP);
                    res.add(Count.GT);
                    res.add(Count.SUB);
                    break;
                case COR:
                    res.add(Count.HYP);
                    res.add(Count.GT);
                    res.add(Count.COR);
                    if (countManipulations.countAll) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    break;
                case INS_LINE:
                    if (path.size() > 1) {
                        throw new RuntimeException("path 'INS_LINE' should only have length 1");
                    }
                    res.add(Count.GT, dist.getReferences().length);
                    res.add(Count.INS, dist.getReferences().length);
                    if (countManipulations.countSubstitutions) {
                        for (int i = 0; i < dist.getReferences().length; i++) {
                            res2.add(new RecoRef(dist.getRecos(), new String[]{dist.getReferences()[i]}));
                        }
                    }
                    break;
                case DEL_LINE:
                    if (path.size() > 1) {
                        throw new RuntimeException("path 'DEL_LINE' should only have length 1");
                    }
                    for (int i = 0; i < dist.getRecos().length; i++) {
                        String reco = dist.getRecos()[i];
                        if (voter.isLineBreak(reco)) {
                            //do not count spaces, if whole line should be deleted - spaces can be substituted by newline.
                            continue;
                        }
                        res.add(Count.HYP);
                        res.add(Count.DEL);
                        if (countManipulations.countSubstitutions) {
                            res2.add(new RecoRef(new String[]{reco}, dist.getReferences()));
                        }
                    }
                    break;
                case MERGE_LINE:
                    //special case for merging lines:
                    // a \n will be interpreted as space ==> Hypothesis gets 1 character longer!
                    res.add(Count.HYP);
                    res.add(Count.GT);
                    res.add(Count.COR);
                    if (countManipulations.countAll) {
                        //BOTH GET REFERENCE AS VALUE
                        res2.add(new RecoRef(dist.getReferences(), dist.getReferences()));
                    }
                    break;
                //all other cases will not be count
                case JUMP_RECO:
                case COR_LINEBREAK:
                case SPLIT_LINE:
                    break;
                default:
                    throw new RuntimeException("found type '" + dist.getManipulation() + "'");
            }
        }
        ILineComparison lc = null;
        if (calcLineComparison) {
            StringBuilder refBuilder = new StringBuilder();
            StringBuilder recoBuilder = new StringBuilder();
            final List<IPoint> manipulations = new LinkedList<>();
            for (PathCalculatorGraph.IDistance<String, String> point : path) {
                if (point.getManipulation().equals("INS_LINE")) {
                    for (int i = 0; i < point.getReferences().length; i++) {
                        String refPart = point.getReferences()[i];
                        refBuilder.append(refPart);
                        manipulations.add(new IPoint() {
                            @Override
                            public Manipulation getManipulation() {
                                return Manipulation.INS;
                            }

                            @Override
                            public String getReco() {
                                return "";
                            }

                            @Override
                            public String getRef() {
                                return refPart;
                            }
                        });
                    }
                    int i = path.get(0).getPoint()[1];
//                    if (alignmentTask.getRefLineMap()[i-2] <0) {
//                        i--;
//                    }
                    lc = getLineComparison(
                            -1,
                            alignmentTask.getRefLineMap()[i - 2],
                            "",
                            refBuilder.toString(),
                            manipulations);
                    return new PathCountResult(res, res2, lc == null ? null : Arrays.asList(lc));
                }
                if (point.getManipulation().equals("DEL_LINE")) {
                    for (int i = 0; i < point.getRecos().length; i++) {
                        String recoPart = point.getRecos()[i];
                        recoBuilder.append(recoPart);
                        manipulations.add(new IPoint() {
                            @Override
                            public Manipulation getManipulation() {
                                return Manipulation.DEL;
                            }

                            @Override
                            public String getReco() {
                                return recoPart;
                            }

                            @Override
                            public String getRef() {
                                return "";
                            }
                        });
                    }
                    int i = path.get(0).getPoint()[0];
                    lc = getLineComparison(
                            alignmentTask.getRecoLineMap()[i - 2],
                            -1,
                            recoBuilder.toString(),
                            "",
                            manipulations);
                    return new PathCountResult(res, res2, lc == null ? null : Arrays.asList(lc));
                }
                final String reco = point.getRecos() == null ? "" : point.getRecos()[0];
                final String ref = point.getReferences() == null ? "" : point.getReferences()[0];
                recoBuilder.append(reco);
                refBuilder.append(ref);
                final Manipulation manipulation = Manipulation.valueOf(point.getManipulation());
                manipulations.add(new Distance(Manipulation.valueOf(point.getManipulation()), reco, ref));
            }
            lc = getLineComparison(
                    alignmentTask.getRecoLineMap()[path.get(0).getPoint()[0] - 1],
                    alignmentTask.getRefLineMap()[path.get(0).getPoint()[1] - 1],
                    recoBuilder.toString(),
                    refBuilder.toString(),
                    manipulations);
        }
        return new PathCountResult(res, res2, lc == null ? null : Arrays.asList(lc));
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
        substitutionCounter.reset();
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
        if (countManipulations.countSubstitutions) {
            for (Pair<RecoRef, Long> pair : substitutionCounter.getResultOccurrence()) {
                RecoRef first = pair.getFirst();
                String reco = first.getReco();
                String ref = first.getRef();
                String key1 = reco == null ? "" : reco;
                String key2 = ref == null ? "" : ref;
                res.addFirst("[" + key1 + "=>" + key2 + "]=" + pair.getSecond());
            }
        }
        List<Pair<Count, Long>> resultOccurrence = getCounter().getResultOccurrence();
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


}
