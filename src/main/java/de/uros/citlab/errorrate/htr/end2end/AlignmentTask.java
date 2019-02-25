package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.aligner.BaseLineAligner;
import de.uros.citlab.errorrate.interfaces.ILine;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class AlignmentTask {

    private final int[] recoLineMap;
    private final int[] refLineMap;
    private final boolean[][] adjazent;
    private final String[] recos;
    private final String[] refs;
    private final boolean useFilter;

    public String[] getRecos() {
        return recos;
    }

    public String[] getRefs() {
        return refs;
    }

    public boolean useFilter() {
        return useFilter;
    }

    public boolean isSameLine(int idxReco, int idxRef) {
        if (idxReco < 0 || idxRef < 0) {
            return true;
        }
        final int i = recoLineMap[idxReco];
        if (i < 0) {
            return true;
        }
        int j = refLineMap[idxRef];
        if (j < 0) {
            return true;
        }
        return adjazent[i][j];
    }

    public AlignmentTask(String[] recos, String[] refs) {
        this(recos, refs, null);
    }

    public AlignmentTask(String[] recos, String[] refs, ITokenizer splitBOW) {
        this(recos, null, refs, null, null, splitBOW);
    }

    private AlignmentTask(String[] recos, int[] recoLineMap, String[] refs, int[] refLineMap, boolean[][] adjazent) {
        this(recos, recoLineMap, refs, refLineMap, adjazent, null);
    }

    private AlignmentTask(String[] recos, int[] recoLineMap, String[] refs, int[] refLineMap, boolean[][] adjazent, ITokenizer splitBOW) {
        if (splitBOW != null) {
            Pair<String[], int[]> pair = splitLines(recos, recoLineMap, splitBOW);
            this.recos = pair.getFirst();
            this.recoLineMap = pair.getSecond();
            Pair<String[], int[]> pair2 = splitLines(refs, refLineMap, splitBOW);
            this.refs = pair2.getFirst();
            this.refLineMap = pair2.getSecond();
        } else {
            this.recos = recos;
            this.recoLineMap = recoLineMap == null ? getDefaultLineMap(recos) : recoLineMap;
            this.refs = refs;
            this.refLineMap = refLineMap == null ? getDefaultLineMap(refs) : refLineMap;
        }
        this.adjazent = adjazent;
        this.useFilter = adjazent != null;

    }

    Pair<String[], int[]> splitLines(String[] lines, int[] lineMap, ITokenizer tokenizer) {
        List<String> words = new LinkedList<>();
        List<Integer> lineIdxs = new LinkedList<>();
//        words.add("\n");
//        lineIdxs.add(-1);
        int cntLB = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int idx = lineMap == null ? i : lineMap[i];
//            List<String> tokens = tokenizer.tokenize(line);
//            for (String token : tokens) {
            if (line.equals("\n")) {
                words.add(line);
                lineIdxs.add(-1);
                cntLB++;
            } else if (line.equals(" ")) {
                words.add("\n");
                lineIdxs.add(-1);
            } else {
                words.add(line);
                lineIdxs.add(cntLB);
            }
//                words.add("\n");
//                lineIdxs.add(-1);
//            }
        }
        int[] res = new int[words.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = lineIdxs.get(i);
        }
        return new Pair<>(words.toArray(new String[0]), res);
    }

    public AlignmentTask(Pair<String[], int[]> recos, Pair<String[], int[]> refs, boolean[][] adjazent) {
        this(recos.getFirst(), recos.getSecond(), refs.getFirst(), refs.getSecond(), adjazent);
    }

    public AlignmentTask(List<ILine> reco, List<ILine> ref, ITokenizer wordTokenizer, IStringNormalizer sn, double thresholdCouverage) {
        this(reco, ref, wordTokenizer, sn, thresholdCouverage, false);
    }

    public AlignmentTask(List<ILine> reco, List<ILine> ref, ITokenizer wordTokenizer, IStringNormalizer sn, double thresholdCouverage, boolean splitBOW) {
        Polygon[] recos = new Polygon[reco.size()];
        Polygon[] refs = new Polygon[ref.size()];
        Boolean useFilter = null; //only use filter, if baselines are given everywhere
        for (int i = 0; i < reco.size(); i++) {
            recos[i] = reco.get(i).getBaseline();
            if (useFilter != null) {
                if (useFilter.booleanValue() != (recos[i] != null)) {
                    throw new RuntimeException("only some lines have baseline - either no or all polygons have to contain baselines");
                }
            } else {
                useFilter = recos[i] != null;
            }
        }
        for (int i = 0; i < ref.size(); i++) {
            refs[i] = ref.get(i).getBaseline();
            if (useFilter != null) {
                if (useFilter.booleanValue() != (refs[i] != null)) {
                    throw new RuntimeException("only some lines have baseline - either no or all polygons have to contain baselines");
                }
            } else {
                useFilter = refs[i] != null;
            }
        }
        this.useFilter = useFilter;

        //TODO: tolerances are ignored anyway and will be calculated properly only on expanded polygons
//        double[] doubles = baseLineAligner.calcTolerances(refs);
        int[][] gtLists = new BaseLineAligner().getGTLists(refs, null, recos, thresholdCouverage);
        this.adjazent = getMap(gtLists, recos.length, refs.length);

        Pair<String[], int[]> recoTokens = getTokensAndLineIndex(reco, wordTokenizer, sn, splitBOW);
        this.recoLineMap = recoTokens.getSecond();
        this.recos = recoTokens.getFirst();
        Pair<String[], int[]> refTokens = getTokensAndLineIndex(ref, wordTokenizer, sn, splitBOW);
        this.refLineMap = refTokens.getSecond();
        this.refs = refTokens.getFirst();
    }

    private final int[] getDefaultLineMap(String[] line) {
        int[] res = new int[line.length];
        int idx = -1;
        for (int i = 0; i < line.length; i++) {
            if ("\n".equals(line[i])) {
                idx++;
                res[i] = -1;
            } else {
                res[i] = idx;
            }
        }
        return res;
    }


    public boolean[][] getAdjazent() {
        return adjazent;
    }

    public int[] getRecoLineMap() {
        return recoLineMap;
    }

    public int[] getRefLineMap() {
        return refLineMap;
    }

    private Pair<String[], int[]> getTokensAndLineIndex(List<ILine> lines, ITokenizer wordTokenizer, IStringNormalizer sn, boolean splitBOW) {
        LinkedList<String> tokens = new LinkedList<>();
        LinkedList<Integer> indexes = new LinkedList<>();
        tokens.add("\n");
        indexes.add(-1);
        for (int i = 0; i < lines.size(); i++) {
            String text = sn == null ? lines.get(i).getText() : sn.normalize(lines.get(i).getText());
            for (String token : wordTokenizer.tokenize(text)) {
                tokens.add(token);
                indexes.add(i);
            }
            tokens.add("\n");
            indexes.add(-1);
        }
        int[] res2 = new int[indexes.size()];
        for (int i = 0; i < res2.length; i++) {
            res2[i] = indexes.get(i);
        }
        return new Pair<>(tokens.toArray(new String[0]), res2);
    }

    private boolean[][] getMap(int[][] gtLists, int sizeReco, int sizeRef) {
        boolean[][] res = new boolean[sizeReco][sizeRef];
        for (int i = 0; i < gtLists.length; i++) {
            int[] idxs = gtLists[i];
            for (int k = 0; k < idxs.length; k++) {
                res[i][idxs[k]] = true;
            }
        }
        return res;
    }

    private int[] toNativeArray(List<Integer> list) {
        int[] res = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i);
        }
        return res;
    }

}
