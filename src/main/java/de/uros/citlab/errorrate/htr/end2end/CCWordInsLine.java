package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.LinkedList;

class CCWordInsLine extends CCAbstract {
    public CCWordInsLine(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int xstart = point[1];
        final int y = point[0];
        if (isLineBreakReco[y] && isLineBreakRef[xstart]) {
            int xend = xstart + 1;
            int cnt = 0;
            while (xend < isLineBreakRef.length) {
                if (isLineBreakRef[xend]) {
                    //-1 because \n does not have to be count
                    return new PathCalculatorGraph.DistanceSmall(point, new int[]{y, xend}, dist.costsAcc + cnt * offsetIns + offsetRecoJump, this);
                }
                if (!isSpaceRef[xend]) {
                    cnt++;//do not count spaces
                }
                xend++;
            }
        }
        return null;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        int[] point = dist.pointPrevious;
        final int xstart = point[1];
        final int y = point[0];
        int xend = xstart + 1;
        int cnt = 0;
        LinkedList<String> sub = new LinkedList<>();
        while (xend < isLineBreakRef.length) {
            if (isLineBreakRef[xend]) {
                //-1 because \n does not have to be count
                return new DistanceStrStr(
                        DistanceStrStr.TYPE.INS_LINE,
                        cnt * offsetIns + offsetRecoJump,
                        dist.costsAcc,
                        null,
                        sub.toArray(new String[0]),
                        point, dist.point
                );
            }
            if (!isSpaceRef[xend]) {
                cnt++;//do not count spaces
            }
            sub.add(refs[xend]);
            xend++;
        }
        throw new RuntimeException("should result in found line break");
    }

}
