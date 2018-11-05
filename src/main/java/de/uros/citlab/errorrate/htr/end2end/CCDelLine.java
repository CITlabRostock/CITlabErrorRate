package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class CCDelLine extends CCAbstract {
    public CCDelLine(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(final int[] point, final PathCalculatorGraph.DistanceSmall dist) {
        final int start = point[0];
        if (isLineBreakReco[start] && isLineBreakRef[point[1]]) {
            int idx = 1;
            while (idx < lineBreaksReco.length) {
                if (lineBreaksReco[idx] > start) {
                    break;
                }
                idx++;
            }
            if (idx == lineBreaksReco.length) {
                return null;
            }
            final int end = lineBreaksReco[idx];
            return new PathCalculatorGraph.DistanceSmall(point, new int[]{end, point[1]}, dist.costsAcc + (end - start - 1) * offsetInsDel, this);
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
        final double costs = (end - start - 1) * offsetInsDel;
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

