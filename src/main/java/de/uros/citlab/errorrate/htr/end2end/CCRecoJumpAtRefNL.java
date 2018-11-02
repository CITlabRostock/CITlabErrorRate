package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class CCRecoJumpAtRefNL extends CCAbstract implements PathCalculatorGraph.ICostCalculatorMulti<String, String>, PathCalculatorGraph.PathFilter<String, String> {
    private int xMax = 0;

    public CCRecoJumpAtRefNL(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        throw new NotImplementedException();
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        throw new NotImplementedException();
    }

    @Override
    public List<PathCalculatorGraph.DistanceSmall> getNeighboursSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        int x = point[1] + 1;
        if (x >= refs.length || !isLineBreakRef[x]) {
            return null;
        }
        List<PathCalculatorGraph.DistanceSmall> res = new LinkedList<>();
        String ref = refs[x];
        int y = point[0] + 1;
        for (int i = 1; i < recos.length; i++) {
            if (y == i) {
                continue;
            }
            res.add(new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, 0, dist.costsAcc, null, ref, point, new int[]{i, x}));
        }
        return res;
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
        final int x = bestEdge.point[1];
        if (x < refs.length && isLineBreakRef[x]) {
            if (xMax > x) {
                return false;//already better alternative
            }
            xMax = x;
            return true;
        }
        //otherwise return addNewEdge(.)
        return x >= xMax;
    }
}
