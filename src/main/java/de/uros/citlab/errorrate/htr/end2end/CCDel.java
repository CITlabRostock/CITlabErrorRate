package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

public class CCDel extends CCAbstract {

    public CCDel(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(final int[] point, final PathCalculatorGraph.DistanceSmall dist) {
        final int y = point[0] + 1;
        if (y >= recos.length || isLineBreakReco[y]) {
            return null;
        }
        final int[] next = new int[]{y, point[1]};
        return new PathCalculatorGraph.DistanceSmall(point, next, dist.costsAcc + offsetDel, this);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        final String part = recos[dist.point[0]];
        return new DistanceStrStr(DistanceStrStr.TYPE.DEL, offsetDel, dist.costsAcc, part, null, dist.pointPrevious, dist.point);
    }
}
