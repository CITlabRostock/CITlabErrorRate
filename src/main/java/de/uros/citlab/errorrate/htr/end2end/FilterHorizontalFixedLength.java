package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class FilterHorizontalFixedLength implements PathCalculatorGraph.PathFilter<String, String> {
    private TIntDoubleHashMap map = new TIntDoubleHashMap();
    private final double offset;
    private final int grid;

    public FilterHorizontalFixedLength(double offset, int grid) {
        this.offset = offset;
        this.grid = grid;
    }

    @Override
    public void init(String[] strings, String[] strings2) {
        map.clear();
    }

    @Override
    public boolean addNewEdge(PathCalculatorGraph.DistanceSmall newDistance) {
        final int x = newDistance.point[1];
        if (x % grid != 0) {
            return true;
        }
        int key = x / grid;
        if (!map.containsKey(key)) {
            map.put(key, newDistance.costsAcc);
            return true;
        }
        final double before = map.get(key);
        double after = newDistance.costsAcc;
        if (after < before) {
            map.put(key, after);
            return true;
        }
        if (before + offset < after) {
            return false;
        }
        //before + offset > after : gap is too large!
        return true;
    }

    @Override
    public boolean followPathsFromBestEdge(PathCalculatorGraph.DistanceSmall bestDistance) {
        final int x = bestDistance.point[1];
        int key = x / grid + 1;
        if (!map.containsKey(key)) {
            return true;
        }
        final double before = map.get(key);
        double after = bestDistance.costsAcc;
        if (before + offset < after) {
            return false;
        }
        //before + offset > after : gap is too large!
        return true;
    }
}
