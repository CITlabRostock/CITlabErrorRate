/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.interfaces.IErrorModuleWithSegmentation;
import de.uros.citlab.errorrate.interfaces.ILine;
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
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ErrorModuleEnd2End implements IErrorModuleWithSegmentation {
    private final ObjectCounter<Count> counter = new ObjectCounter<>();
    private final ObjectCounter<RecoRef> substitutionCounter = new ObjectCounter<>();
    private final ITokenizer tokenizer;
    private final Boolean detailed;
    private final IStringNormalizer stringNormalizer;
    private final PathCalculatorGraph<String, String> pathCalculator = new PathCalculatorGraph<>();
    private final Mode mode;
    private final Voter voter = new Voter();
    private static final Logger LOG = LoggerFactory.getLogger(ErrorModuleEnd2End.class);
    private int sizeProcessViewer = -1;
    private File fileDynProg = null;
    private PathFilterBaselineMatch filter = null;
    private boolean usePolygons;
    private double thresholdCouverage = 0.0;


    public ErrorModuleEnd2End(ICategorizer categorizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, Boolean detailed) {
        this(new TokenizerCategorizer(categorizer), stringNormalizer, mode, usePolygons, detailed, 100);
    }

    public ErrorModuleEnd2End(ICategorizer categorizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, Boolean detailed, double filterOffset) {
        this(new TokenizerCategorizer(categorizer), stringNormalizer, mode, usePolygons, detailed, filterOffset);
    }

    public enum Mode {
        RO,
        NO_RO,
        RO_SEG,
        NO_RO_SEG,
    }

    public void setThresholdCouverage(double thresholdCouverage) {
        this.thresholdCouverage = thresholdCouverage;
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, Boolean detailed) {
        this(tokenizer, stringNormalizer, mode, usePolygons, detailed, 100);
    }

    public ErrorModuleEnd2End(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Mode mode, boolean usePolygons, Boolean detailed, double filterOffset) {
        this.usePolygons = usePolygons;
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
        PathCalculatorGraph.PathFilter filter = null;
        switch (mode) {
            case NO_RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
                CCLineBreakAndSpaceRecoJump ccLineBreakAndSpaceRecoJump = new CCLineBreakAndSpaceRecoJump(voter);
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) ccLineBreakAndSpaceRecoJump);
                if (filterOffset > 0.0) {
                    filter = ccLineBreakAndSpaceRecoJump;
                }
                break;
            case NO_RO:
                CCLineBreakRecoJump jumper = new CCLineBreakRecoJump(voter);
                pathCalculator.addCostCalculator((PathCalculatorGraph.ICostCalculatorMulti<String, String>) jumper);
                if (filterOffset > 0.0) {
                    filter = jumper;
                }
                break;
            case RO_SEG:
                pathCalculator.addCostCalculator(new CCSubOrCorNL(voter));
            case RO:
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

    public void setSizeProcessViewer(int sizeImage) {
        sizeProcessViewer = sizeImage;
    }

    public void setFileDynProg(File file) {
        fileDynProg = file;
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
        public void init(String[] strings, String[] strings2) {
            map.clear();
        }

        @Override
        public boolean addNewEdge(PathCalculatorGraph.DistanceSmall newDistance) {
            final int x = newDistance.point[1];
            if (x % grid != 0) {
                return true;
            }
            int key = x / grid;
            if (!map.containsKey(key)) {
                map.put(key, newDistance.costsAcc);
                return true;
            }
            final double before = map.get(key);
            double after = newDistance.costsAcc;
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
        public boolean followPathsFromBestEdge(PathCalculatorGraph.DistanceSmall bestDistance) {
            final int x = bestDistance.point[1];
            int key = x / grid + 1;
            if (!map.containsKey(key)) {
                return true;
            }
            final double before = map.get(key);
            double after = bestDistance.costsAcc;
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
            return ("\"" + sbReco + "\"=>\"" + sbRef + "\"").replace("\n", "\\n");
        }
    }

    @Override
    public String toString() {
        return "ErrorModuleEnd2End{" +
                "counter=" + counter +
                ", substitutionCounter=" + substitutionCounter +
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
        List<String> recoList = tokenizer.tokenize(reco);
        recoList.add(0, "\n");
        recoList.add("\n");
        String[] recos = recoList.toArray(new String[0]);
        List<String> refList = tokenizer.tokenize(ref);
        refList.add(0, "\n");
        refList.add("\n");
        String[] refs = refList.toArray(new String[0]);
        AlignmentTask result = new AlignmentTask(recos, refs);
        calculate(result, sizeProcessViewer, fileDynProg);
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
                    sb1.append(String.format(" %2d", (int) (dist == null ? 0 : dist.costsAcc + 0.5)));
                    outV[j] = dist == null ? 0 : dist.costsAcc;
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


    @Override
    public void calculateWithSegmentation(List<ILine> reco, List<ILine> ref) {
        AlignmentTask lmr = new AlignmentTask(reco, ref, tokenizer, stringNormalizer, thresholdCouverage);
        calculate(lmr, sizeProcessViewer, fileDynProg);

    }

    private void calculate(AlignmentTask alignmentTask, int sizeProcessViewer, File out) {
        Pair<ObjectCounter<Count>, ObjectCounter<RecoRef>> pathCount = getPathCount(alignmentTask, sizeProcessViewer, out);
        counter.addAll(pathCount.getFirst());
        if (detailed == null || detailed) {
            substitutionCounter.addAll(pathCount.getSecond());
        }
        counter.add(Count.ERR, counter.get(Count.INS) + counter.get(Count.DEL) + counter.get(Count.SUB));
    }

    private Pair<ObjectCounter<Count>, ObjectCounter<RecoRef>> getPathCount(AlignmentTask alignmentTask, int sizeProcessViewer, File out) {
        //use dynamic programming to calculate the cheapest path through the dynamic programming tabular
//        calcBestPathFast(recos, refs);
        ObjectCounter<Count> res1 = new ObjectCounter<>();
        ObjectCounter<RecoRef> res2 = new ObjectCounter<>();
        if (filter != null) {
            filter.setAlignmentTask(alignmentTask);
        }
        pathCalculator.setUpdateScheme(PathCalculatorGraph.UpdateScheme.LAZY);
        pathCalculator.setSizeProcessViewer(sizeProcessViewer);
        pathCalculator.setFileDynMat(out);
        String[] recos = alignmentTask.getRecos();
        String[] refs = alignmentTask.getRefs();
        List<String> recoList = Arrays.asList(recos);
        List<String> refList = Arrays.asList(refs);
        PathCalculatorGraph.DistanceMat<String, String> mat = pathCalculator.calcDynProg(recoList, refList);
//        pathCalculator.calcBestPath(mat);
        List<PathCalculatorGraph.IDistance<String, String>> calcBestPath = pathCalculator.calcBestPath(mat);
        log(mat, calcBestPath, recos, refs);
        if (calcBestPath == null) {
            //TODO: return maximal error or throw RuntimeException?
            LOG.warn("cannot find path between \n" + recoList.toString().replace("\n", "\\n") + " and \n" + refList.toString().replace("\n", "\\n"));
            throw new RuntimeException("cannot find path (see Logger.warn) for more information");
        }
        for (Count c : Count.values()) {
            counter.add(c, 0L);
        }
        int[] usedReco = getUsedRecos(recos, calcBestPath);
        int max = VectorUtil.max(usedReco);
        //if any reference is used, the resulting array "usedReco" should only conatain zeros.
        if (isRO() || !(max > 1 || countUnusedChars(usedReco, recos) > 0)) {
            return getPathCounts(calcBestPath, detailed);
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
                    case MERGE_LINE:
                    case SPLIT_LINE:
                        switch (typeBefore) {
                            case COR:
                            case INS:
                            case DEL:
                            case SUB:
                            case DEL_LINE:
                            case MERGE_LINE:
                            case SPLIT_LINE:
                                return true;
                        }
                        return false;
                    case INS_LINE:
                    case JUMP_RECO:
//                    case SPLIT_LINE:
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
                    case SPLIT_LINE:
                        return true;
                    case JUMP_RECO:
                    case DEL_LINE:
                    case INS_LINE:
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
            ErrorModuleEnd2End fallback = new ErrorModuleEnd2End(tokenizer, null, modeFallback, usePolygons, detailed, 0.0);
            File outSubProblem = null;
            if (out != null) {
                String path = out.getPath();
                path = path.substring(0, path.lastIndexOf(".")) + "_" + path.substring(path.lastIndexOf("."));
                outSubProblem = new File(path);
            }
            return fallback.getPathCount(alignmentTask, sizeProcessViewer, outSubProblem);
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
            LOG.debug("add count of subpath {}", toDeletePath);
            Pair<ObjectCounter<Count>, ObjectCounter<RecoRef>> pathCounts = getPathCounts(toDeletePath.path, detailed);
            res1.addAll(pathCounts.getFirst());
            res2.addAll(pathCounts.getSecond());
//            ObjectCounter<Count> count = count(toDeletePath.path, usedReco, recos, refs);
        }
        String[] refShorter = getSubProblem(refs, maskRef, null).getFirst();
        if (countChars(refShorter) == 0) {
            String[] recosShorter = getSubProblem(recos, maskReco, null).getFirst();
            for (int i = 0; i < recosShorter.length; i++) {
                String s = recosShorter[i];
                if (!voter.isLineBreak(s)) {
                    if (isSeg() && voter.isSpace(s)) {
                        //allow any partition of text - then it is better to substitute spaces by newlines. Do not count spaces.
                        continue;
                    }
                    res1.add(Count.HYP);
                    res1.add(Count.DEL);
                    res2.add(new RecoRef(new String[]{s}, new String[0]));
                }
            }
            return new Pair<>(res1, res2);
        }
        File outSubProblem = null;
        if (out != null) {
            String path = out.getPath();
            path = path.substring(0, path.lastIndexOf(".")) + "_" + path.substring(path.lastIndexOf("."));
            outSubProblem = new File(path);
        }
        AlignmentTask subProblem = getSubProblem(alignmentTask, maskRef, maskReco);
        Pair<ObjectCounter<Count>, ObjectCounter<RecoRef>> pathCountSubProblem = getPathCount(subProblem, sizeProcessViewer, outSubProblem);
        res1.addAll(pathCountSubProblem.getFirst());
        res2.addAll(pathCountSubProblem.getSecond());
        return new Pair<>(res1, res2);
    }

    private AlignmentTask getSubProblem(AlignmentTask original, boolean[] maskRef, boolean[] maskReco) {
        AlignmentTask res = new AlignmentTask(
                getSubProblem(original.getRecos(), maskReco, original.getRecoLineMap()),
                getSubProblem(original.getRefs(), maskRef, original.getRefLineMap()),
                original.getAdjazent());
        return res;
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

    private Pair<String[], int[]> getSubProblem(String[] transcripts, boolean[] toDelete, int[] lineIdxs) {
        LinkedList<String> res = new LinkedList<>();
        LinkedList<Integer> res2 = new LinkedList<>();
        boolean lastWasLineBreak = false;
        for (int i = 0; i < transcripts.length; i++) {
            if (!toDelete[i]) {
                //prevent adding double-linebreaks
                boolean isLineBreak = voter.isLineBreakOrSpace(transcripts[i]);
                if (!lastWasLineBreak || !isLineBreak) {
                    res.add(transcripts[i]);
                    if (lineIdxs != null) {
                        res2.add(lineIdxs[i]);
                    }
                }
                lastWasLineBreak = isLineBreak;
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

    private boolean isSeg() {
        return mode.equals(Mode.RO_SEG) || mode.equals(Mode.NO_RO_SEG);
    }

    private boolean isRO() {
        return mode.equals(Mode.RO) || mode.equals(Mode.RO_SEG);
    }

    private Pair<ObjectCounter<Count>, ObjectCounter<RecoRef>> getPathCounts(List<PathCalculatorGraph.IDistance<String, String>> path, Boolean detailed) {
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
                    if (detailed == null || detailed.booleanValue()) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    res.add(Count.HYP);
                    res.add(Count.DEL);
                    break;
                case INS:
                    if (detailed == null || detailed.booleanValue()) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    res.add(Count.GT);
                    res.add(Count.INS);
                    break;
                case SUB:
                    if (detailed == null || detailed.booleanValue()) {
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
                    if (detailed != null && detailed.booleanValue()) {
                        res2.add(new RecoRef(dist.getRecos(), dist.getReferences()));
                    }
                    break;
                case INS_LINE:
                    res.add(Count.GT, dist.getReferences().length);
                    res.add(Count.INS, dist.getReferences().length);
                    if (detailed == null || (detailed != null && detailed.booleanValue())) {
                        for (int i = 0; i < dist.getReferences().length; i++) {
                            res2.add(new RecoRef(dist.getRecos(), new String[]{dist.getReferences()[i]}));
                        }
                    }
                    break;
                case DEL_LINE:
                    for (int i = 0; i < dist.getRecos().length; i++) {
                        String reco = dist.getRecos()[i];
                        if (voter.isLineBreak(reco)) {
                            //do not count spaces, if whole line should be deleted - spaces can be substituted by newline.
                            continue;
                        }
                        res.add(Count.HYP);
                        res.add(Count.DEL);
                        if (detailed == null || (detailed != null && detailed.booleanValue())) {
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
                    if (detailed != null && detailed.booleanValue()) {
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
        return new Pair<>(res, res2);
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
        if (detailed == null || detailed) {
            for (Pair<RecoRef, Long> pair : substitutionCounter.getResultOccurrence()) {
                RecoRef first = pair.getFirst();
                String[] recos = first.recos;
                String key1;
                if (recos == null || recos.length == 0) {
                    key1 = "";
                } else if (recos.length == 1) {
                    key1 = recos[0];
                } else {
                    key1 = Arrays.toString(first.recos);
                }
                String[] refs = first.refs;
                String key2;
                if (refs == null || refs.length == 0) {
                    key2 = "";
                } else if (refs.length == 1) {
                    key2 = refs[0];
                } else {
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
