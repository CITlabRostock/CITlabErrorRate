package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCDelNL extends CCAbstract {

    public CCDelNL(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        //should only be done between non-spacing and non-LB position in y-dimension and LB position in x-dimension
        final int x1 = point[1];
        final int x2 = x1 + 1;
        final int y = point[0] + 1;
        if (x2 < refs.length && y < recos.length &&
                !isLineBreakRef[x1] && !isSpaceRef[x1] &&
                !isLineBreakRef[x2] && !isSpaceRef[x2] &&
                isLineBreakReco[y]) {

            final String reco = recos[y];
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
