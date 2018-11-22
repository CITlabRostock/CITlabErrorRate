package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class CCDelLine extends CCAbstract {
    public CCDelLine(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(final int[] point, final PathCalculatorGraph.DistanceSmall dist) {
        final int ystart = point[0];
        final int x = point[1];
        if (isLineBreakReco[ystart] && isLineBreakRef[x]) {
            int yend = ystart + 1;
            while (yend < isLineBreakReco.length) {
                if (isLineBreakReco[yend]) {
                    //-1 because \n does not have to be count
                    return new PathCalculatorGraph.DistanceSmall(point, new int[]{yend, x}, dist.costsAcc + (yend - ystart - 1) * offsetDel, this);
                }
                yend++;
            }
        }
        return null;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        final int[] next = dist.point;
        final int[] previous = dist.pointPrevious;
        final int start = previous[0];
        final int end = next[0];
        final String[] subList = Arrays.copyOfRange(recos, start + 1, end);
        final double costs = (end - start - 1) * offsetDel;
        return new DistanceStrStr(
                DistanceStrStr.TYPE.DEL_LINE,
                costs,
                dist.costsAcc + costs,
                subList,
                null,
                previous,
                next);
    }
}

