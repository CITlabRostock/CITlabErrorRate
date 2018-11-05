package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class CCInsLine extends CCAbstract {
    public CCInsLine(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int start = point[1];
        if (isLineBreakReco[point[0]] && isLineBreakRef[start]) {
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
            return new PathCalculatorGraph.DistanceSmall(point, new int[]{point[0], end}, dist.costsAcc + (end - start - 1) * offsetInsDel, this);
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
        final double costs = (end - start - 1) * offsetInsDel;
//                String[] strings =new String[costs];
        String[] subList = Arrays.copyOfRange(refs, start + 1, end);
//                        refs.subList(start + 1, end).toArray(new String[0]);
        return new DistanceStrStr(
                DistanceStrStr.TYPE.INS_LINE,
                costs,
                dist.costsAcc + costs,
                null,
                subList,
                point,
                new int[]{point[0], end});
    }
}
