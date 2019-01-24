package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class CCInsLine extends CCAbstract {
    public CCInsLine(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int xstart = point[1];
        final int y = point[0];
        if (isLineBreakReco[y] && isLineBreakRef[xstart]) {
            int xend = xstart + 1;
            while (xend < isLineBreakRef.length) {
                if (isLineBreakRef[xend]) {
                    //-1 because \n does not have to be count
                    return new PathCalculatorGraph.DistanceSmall(point, new int[]{y, xend}, dist.costsAcc + (xend - xstart - 1) * offsetIns, this);
                }
                xend++;
            }
        }
        return null;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        int[] point = dist.pointPrevious;
        final int start = point[1];
        int idx = 1;
        while (idx < lineBreaksRef.length) {
            if (lineBreaksRef[idx] > start) {
                break;
            }
            idx++;
        }
        if (idx == lineBreaksRef.length) {
            return null;
        }
        final int end = lineBreaksRef[idx];
        final double costs = (end - start - 1) * offsetIns;
//                String[] strings =new String[costs];
        String[] subList = Arrays.copyOfRange(refs, start + 1, end);
//                        refs.subList(start + 1, end).toArray(new String[0]);
        return new DistanceStrStr(
                DistanceStrStr.TYPE.INS_LINE,
                costs,
                dist.costsAcc,
                null,
                subList,
                point,
                new int[]{point[0], end});
    }
}
