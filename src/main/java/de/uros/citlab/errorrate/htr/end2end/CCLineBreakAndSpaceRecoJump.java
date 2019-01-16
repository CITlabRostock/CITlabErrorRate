package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.LinkedList;
import java.util.List;

class CCLineBreakAndSpaceRecoJump extends CCAbstract implements PathCalculatorGraph.ICostCalculatorMulti<String, String>, PathCalculatorGraph.PathFilter<String, String> {
    private int xMax = 0;

    public CCLineBreakAndSpaceRecoJump(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<PathCalculatorGraph.DistanceSmall> getNeighboursSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int x = point[1];
        final int y = point[0];
        if (x >= refs.length || y >= recos.length) {
            return null;
        }
        final boolean lineBreakReco = isLineBreakReco[y];
        final boolean spaceRef = isSpaceRef[x];
        if (lineBreakReco && spaceRef) {
            List<PathCalculatorGraph.DistanceSmall> res = new LinkedList<>();
            //jump to linebreaks of reco
            for (int i = 0; i < lineBreaksReco.length; i++) {
                final int target = lineBreaksReco[i];
                if (target != y) {
                    res.add(new PathCalculatorGraph.DistanceSmall(point, new int[]{target, x}, dist.costsAcc+ offsetRecoJump, this));
                }
            }
            return res;
        }
        final boolean lineBreakRef = isLineBreakRef[x];
        final boolean spaceReco = isSpaceReco[y];
        if (lineBreakRef && (lineBreakReco || spaceReco)) {
            List<PathCalculatorGraph.DistanceSmall> res = new LinkedList<>();
            //jump to spaces and linebreaks of reco
            for (int i = 1; i < recos.length; i++) {
                if ((isLineBreakReco[i] || isSpaceReco[i]) && i != y) {
                    res.add(new PathCalculatorGraph.DistanceSmall(point, new int[]{i, x}, dist.costsAcc+ offsetRecoJump, this));
                }
            }
            return res;
        }
        return null;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        return new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, offsetRecoJump, dist.costsAcc+ offsetRecoJump, (String) null, null, dist.pointPrevious, dist.point);
    }

    @Override
    public void init(String[] strings, String[] strings2) {
        xMax = 0;
    }

    @Override
    public boolean addNewEdge(PathCalculatorGraph.DistanceSmall newEdge) {
        return newEdge.point[1] >= xMax;
    }

    @Override
    public boolean followPathsFromBestEdge(PathCalculatorGraph.DistanceSmall bestEdge) {
        int x = bestEdge.point[1];
        if (x < refs.length && isLineBreakRef[x]) {
            if (xMax > x) {
                return false;//already better alternative
            }
            xMax = x;
            return true;
        }
        return x >= xMax;
    }
}
