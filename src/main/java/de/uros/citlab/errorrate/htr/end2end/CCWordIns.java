package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCWordIns extends CCAbstract {

    public CCWordIns(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int x = point[1] + 1;
        if (x >= refs.length || isLineBreakRef[x]) {
            return null;
        }
        int[] next = new int[]{point[0], x};
        return new PathCalculatorGraph.DistanceSmall(point, next, isSpaceRef[x] ? dist.costsAcc : dist.costsAcc + offsetIns, this);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        int[] point = dist.pointPrevious;
        final int x = point[1] + 1;
        final String part = refs[x];
        return new DistanceStrStr(DistanceStrStr.TYPE.INS, isSpaceRef[x] ? 0 : offsetIns, dist.costsAcc, null, part, point, dist.point);
    }
}
