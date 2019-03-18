package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.aligner.BaseLineAligner;
import de.uros.citlab.errorrate.interfaces.IAdjazentCalculator;

import java.awt.*;

public class AdjacentCalculatorBaselines implements IAdjazentCalculator {
    private double thresh;

    public AdjacentCalculatorBaselines(double thresh) {
        this.thresh = thresh;
    }

    @Override
    public boolean[][] getAdjacent(Polygon[] recos, Polygon[] refs) {
        int[][] gtLists = new BaseLineAligner().getGTLists(refs, null, recos, thresh);
        return getMap(gtLists, recos.length, refs.length);

    }

    @Override
    public void setThreshold(double thresh) {
        this.thresh = thresh;
    }

    private boolean[][] getMap(int[][] gtLists, int sizeReco, int sizeRef) {
        boolean[][] res = new boolean[sizeReco][sizeRef];
        for (int i = 0; i < gtLists.length; i++) {
            int[] idxs = gtLists[i];
            for (int k = 0; k < idxs.length; k++) {
                res[i][idxs[k]] = true;
            }
        }
        return res;
    }

}
