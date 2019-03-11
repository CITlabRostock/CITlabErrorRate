package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class CCDelLine extends CCAbstract {
    private boolean countSpaces;

    public CCDelLine(Voter voter, boolean countSpaces) {
        super(voter);
        this.countSpaces = countSpaces;
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(final int[] point, final PathCalculatorGraph.DistanceSmall dist) {
        final int ystart = point[0];
        final int x = point[1];
        if (isLineBreakReco[ystart] && isLineBreakRef[x]) {
            int yend = ystart + 1;
            int cntErrors = 0;
            while (yend < isLineBreakReco.length) {
                if (isLineBreakReco[yend]) {
                    //-1 because \n does not have to be count
                    return new PathCalculatorGraph.DistanceSmall(point, new int[]{yend, x}, dist.costsAcc + cntErrors * offsetDel, this);
                }
                //do not count spaces if any segmentation is allowed or mode is WER
                if (countSpaces || !isSpaceReco[yend]) {
                    cntErrors++;
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
        final String[] subList;
//        if (countSpaces) {
            subList = Arrays.copyOfRange(recos, start + 1, end);
//        } else {
//            subList = new String[end - start - 1];
//            int k=0;
//            for (int i = start + 1; i < end; i++) {
//                if (isSpaceReco[i]) {
//                    subList[k]="\n";
//                }else{
//                    subList[k]=recos[i];
//                }
//                k++;
//            }
//        }
        final int ystart = dist.pointPrevious[0];
        int yend = ystart + 1;
        int cntErrors = 0;
        while (yend < isLineBreakReco.length) {
            if (isLineBreakReco[yend]) {
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.DEL_LINE,
                        cntErrors * offsetDel,
                        dist.costsAcc,
                        subList,
                        null,
                        previous,
                        next);
            }
            //do not count spaces if any segmentation is allowed
            if (countSpaces || !isSpaceReco[yend]) {
                cntErrors++;
            }
            yend++;
        }
        throw new RuntimeException("should not end here wihtout finding a linebreak");
    }
}

