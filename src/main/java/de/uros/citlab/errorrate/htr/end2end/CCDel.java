package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCDel extends CCAbstract {

    public CCDel(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(final int[] point, final PathCalculatorGraph.IDistance<String, String> dist) {
        final int y = point[0] + 1;
        if (y >= recos.length || isLineBreakReco[y]) {
            return null;
        }
        final String part = recos[y];
        final int[] next = new int[]{y, point[1]};
        return new DistanceStrStr(DistanceStrStr.TYPE.DEL, 1, dist.getCostsAcc() + 1, part, null, point, next);
    }
}
