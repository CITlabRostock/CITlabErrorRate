package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCIns extends CCAbstract {

    public CCIns(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int x = point[1] + 1;
        if (x >= refs.length || isLineBreakRef[x]) {
            return null;
        }
        final String part = refs[x];
        int[] next = new int[]{point[0], x};
        return new PathCalculatorGraph.DistanceSmall(point, next,dist.costsAcc+1,this);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        int[] point = dist.pointPrevious;
        final int x = point[1] + 1;
        final String part = refs[x];
        return new DistanceStrStr(DistanceStrStr.TYPE.INS, 1, dist.costsAcc, null, part, point, dist.point);
    }
}
