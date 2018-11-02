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
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        throw new NotImplementedException();
    }

    @Override
    public List<PathCalculatorGraph.IDistance<String, String>> getNeighbours(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        int x = point[1] + 1;
        if (x >= refs.length || !isLineBreakRef[x]) {
            return null;
        }
        List<PathCalculatorGraph.IDistance<String, String>> res = new LinkedList<>();
        String ref = refs[x];
        int y = point[0] + 1;
        for (int i = 1; i < recos.length; i++) {
            if (y == i) {
                continue;
            }
            res.add(new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, 0, dist.getCostsAcc(), null, ref, point, new int[]{i, x}));
        }
        return res;
    }

    @Override
    public void setComparator(Comparator<PathCalculatorGraph.IDistance<String, String>> comparator) {
    }

    @Override
    public void init(String[] strings, String[] strings2) {
        xMax = 0;
    }

    @Override
    public boolean addNewEdge(PathCalculatorGraph.IDistance<String, String> newEdge) {
        return newEdge.getPoint()[1] >= xMax;
    }

    @Override
    public boolean followPathsFromBestEdge(PathCalculatorGraph.IDistance<String, String> bestEdge) {
        final int x = bestEdge.getPoint()[1];
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
