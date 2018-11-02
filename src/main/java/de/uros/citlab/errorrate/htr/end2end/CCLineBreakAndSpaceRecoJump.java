package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class CCLineBreakAndSpaceRecoJump extends CCAbstract implements PathCalculatorGraph.ICostCalculatorMulti<String, String>, PathCalculatorGraph.PathFilter<String, String> {
    private int xMax = 0;

    public CCLineBreakAndSpaceRecoJump(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        throw new NotImplementedException();
    }

    @Override
    public List<PathCalculatorGraph.IDistance<String, String>> getNeighbours(int[] point, PathCalculatorGraph.IDistance<String, String> dist) {
        final int x = point[1] + 1;
        final int y = point[0] + 1;
        if (x >= refs.length || y >= recos.length) {
            return null;
        }
        final boolean lineBreakReco = isLineBreakReco[y];
        final boolean spaceRef = isSpaceRef[x];
        if (lineBreakReco && spaceRef) {
            List<PathCalculatorGraph.IDistance<String, String>> res = new LinkedList<>();
            //jump to linebreaks of reco
            for (int i = 0; i < lineBreaksReco.length; i++) {
                final int target = lineBreaksReco[i];
                if (target != y) {
                    res.add(new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, 0, dist.getCostsAcc(), (String) null, null, point, new int[]{target, x}));
                }
            }
            return res;
        }
        final boolean lineBreakRef = isLineBreakRef[x];
        final boolean spaceReco = isSpaceReco[y];
        if (lineBreakRef && (lineBreakReco || spaceReco)) {
            List<PathCalculatorGraph.IDistance<String, String>> res = new LinkedList<>();
            //jump to spaces and linebreaks of reco
            for (int i = 1; i < recos.length; i++) {
                if ((isLineBreakReco[i] || isSpaceReco[i]) && i != y) {
                    res.add(new DistanceStrStr(DistanceStrStr.TYPE.JUMP_RECO, 0, dist.getCostsAcc(), null, (String) null, point, new int[]{i, x}));
                }
            }
            return res;
        }
        return null;
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
        int x = bestEdge.getPoint()[1];
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
