package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

public abstract class CCAbstract implements PathCalculatorGraph.ICostCalculator<String, String> {

    private PathCalculatorGraph.DistanceMat<String, String> mat;
    protected String[] recos;
    protected String[] refs;
    final private Voter voter;
    protected int[] lineBreaksReco;
    protected int[] lineBreaksRef;
    protected boolean[] isSpaceReco;
    protected boolean[] isSpaceRef;
    protected boolean[] isLineBreakReco;
    protected boolean[] isLineBreakRef;
    //additional costs to force the algorithm to prefer sub-sub instead of ins-cor-del pathes
    protected double offsetIns = 1 + Math.pow(2, -20);
    protected double offsetDel = 1 + Math.pow(2, -18);
    //additional costs to force algorithm to prefer original reading order
    protected double offsetRecoJump = Math.pow(2, -9);

    public CCAbstract(Voter voter) {
        this.voter = voter;
    }

    @Override
    public void init(PathCalculatorGraph.DistanceMat<String, String> mat, String[] recos, String[] refs) {
        this.mat = mat;
        this.recos = recos;
        this.refs = refs;
        lineBreaksReco = getLineBreaks(recos);
        lineBreaksRef = getLineBreaks(refs);
        isSpaceReco = new boolean[recos.length];
        isLineBreakReco = new boolean[recos.length];
        for (int i = 0; i < isSpaceReco.length; i++) {
            isSpaceReco[i] = voter.isSpace(recos[i]);
            isLineBreakReco[i] = voter.isLineBreak(recos[i]);
        }
        isSpaceRef = new boolean[refs.length];
        isLineBreakRef = new boolean[refs.length];
        for (int i = 0; i < isSpaceRef.length; i++) {
            isSpaceRef[i] = voter.isSpace(refs[i]);
            isLineBreakRef[i] = voter.isLineBreak(refs[i]);
        }
    }

    private int[] getLineBreaks(String[] text) {
        int[] tmp = new int[text.length];
        int cnt = 0;
        for (int i = 0; i < text.length; i++) {
            if (voter.isLineBreak(text[i])) {
                tmp[cnt++] = i;
            }
        }
        int[] res = new int[cnt];
        System.arraycopy(tmp, 0, res, 0, cnt);
        return res;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        return (PathCalculatorGraph.IDistance<String, String>) dist;
    }

}
