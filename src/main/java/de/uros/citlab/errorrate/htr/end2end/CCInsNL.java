package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCInsNL extends CCAbstract {

    public CCInsNL(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        //should only be done between non-spacing and non-LB position in y-dimension and LB position in x-dimension
        final int y1 = point[0];
        final int y2 = point[0] + 1;
        final int x = point[1] + 1;
        if (y2 < recos.length && x < refs.length &&
                !isLineBreakReco[y1] && !isSpaceReco[y1] &&
                !isLineBreakReco[y2] && !isSpaceReco[y2] &&
                isLineBreakRef[x]) {

            final String ref = refs[x];
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
