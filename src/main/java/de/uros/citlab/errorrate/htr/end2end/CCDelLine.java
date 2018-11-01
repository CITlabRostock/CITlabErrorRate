package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class CCDelLine extends CCAbstract {
    public CCDelLine(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(final int[] point, final PathCalculatorGraph.IDistance<String, String> dist) {
        final int start = point[0];
        if (isLineBreakReco[start] && isLineBreakRef[point[1]]) {
            int idx = 0;
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
            final String[] subList = Arrays.copyOfRange(recos, start + 1, end);
//                String[] strings = recos.subList(start + 1, end).toArray(new String[0]);
            final int costs = end - start - 1;
            return new DistanceStrStr(
                    DistanceStrStr.TYPE.DEL_LINE,
                    costs,
                    dist.getCostsAcc() + costs,
                    subList,
                    null,
                    point,
                    new int[]{end, point[1]});
        }
        return null;
    }
}

