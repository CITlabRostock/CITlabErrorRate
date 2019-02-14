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
        this(recos, null, refs, null, null);
    }

    private AlignmentTask(String[] recos, int[] recoLineMap, String[] refs, int[] refLineMap, boolean[][] adjazent) {
        this.recos = recos;
        this.recoLineMap = recoLineMap == null ? getDefaultLineMap(recos) : recoLineMap;
        this.refs = refs;
        this.refLineMap = refLineMap == null ? getDefaultLineMap(refs) : refLineMap;
        this.adjazent = adjazent;
        this.useFilter = adjazent != null;

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

    public AlignmentTask(Pair<String[], int[]> recos, Pair<String[], int[]> refs, boolean[][] adjazent) {
        this(recos.getFirst(), recos.getSecond(), refs.getFirst(), refs.getSecond(), adjazent);
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

    public AlignmentTask(List<ILine> reco, List<ILine> ref, ITokenizer tokenizer, IStringNormalizer sn, double thresholdCouverage) {
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

        Pair<String[], int[]> recoTokens = getTokensAndLineIndex(reco, tokenizer, sn);
        this.recoLineMap = recoTokens.getSecond();
        this.recos = recoTokens.getFirst();
        Pair<String[], int[]> refTokens = getTokensAndLineIndex(ref, tokenizer, sn);
        this.refLineMap = refTokens.getSecond();
        this.refs = refTokens.getFirst();
    }

    private Pair<String[], int[]> getTokensAndLineIndex(List<ILine> lines, ITokenizer tokenizer, IStringNormalizer sn) {
        LinkedList<String> tokens = new LinkedList<>();
        LinkedList<Integer> indexes = new LinkedList<>();
        tokens.add("\n");
        indexes.add(-1);
        for (int i = 0; i < lines.size(); i++) {
            String text = sn == null ? lines.get(i).getText() : sn.normalize(lines.get(i).getText());
            for (String token : tokenizer.tokenize(text)) {
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
