package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

public class CCSubOrCorNL extends CCAbstract {

    public CCSubOrCorNL(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int y = point[0] + 1;
        final int x = point[1] + 1;
        if (y < recos.length && x < refs.length && (isLineBreakReco[y] != isLineBreakRef[x]) && (isSpaceReco[y] != isSpaceRef[x])) {
            return new PathCalculatorGraph.DistanceSmall(point, new int[]{y, x}, dist.costsAcc, this);
        }
        return null;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        final int[] point = dist.point;
        final int y = point[0];
        final int x = point[1];
        return new DistanceStrStr(
                isLineBreakReco[y] ? DistanceStrStr.TYPE.MERGE_LINE : DistanceStrStr.TYPE.SPLIT_LINE,
                0,
                dist.costsAcc,
                recos[y],
                refs[x],
                dist.pointPrevious,
                point);
    }
}

