package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCIns extends CCAbstract {

    public CCIns(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        final int x = point[1] + 1;
        if (x >= refs.length || isLineBreakRef[x]) {
            return null;
        }
        final String part = refs[x];
        int[] next = new int[]{point[0], x};
        return new DistanceStrStr(DistanceStrStr.TYPE.INS, 1, dist.getCostsAcc() + 1, null, part, point, next);
    }
}
