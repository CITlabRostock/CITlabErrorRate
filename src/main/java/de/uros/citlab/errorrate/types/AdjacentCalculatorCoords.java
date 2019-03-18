package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.interfaces.IAdjazentCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class AdjacentCalculatorCoords implements IAdjazentCalculator {
    private double thresh;
    private int step = 3;
    Logger LOG = LoggerFactory.getLogger(AdjacentCalculatorCoords.class);

    public AdjacentCalculatorCoords(double thresh) {
        this.thresh = thresh;
    }

    @Override
    public boolean[][] getAdjacent(Polygon[] recos, Polygon[] refs) {
        boolean[][] res = new boolean[recos.length][refs.length];
        for (int i = 0; i < recos.length; i++) {
            Polygon reco = recos[i];
            boolean[] resReco = res[i];
            for (int j = 0; j < refs.length; j++) {
                resReco[j] = intersectionOverUnion(reco, refs[j]) > thresh;
            }
        }
        return res;
    }

    @Override
    public void setThreshold(double thresh) {
        this.thresh = thresh;
    }

    private double intersectionOverUnion(Polygon reco, Polygon ref) {
        int intersect = 0;
        int union = 0;
        Rectangle bounds = ref.getBounds().union(reco.getBounds());
        for (int i = bounds.y; i < bounds.y + bounds.height; i += step) {
            for (int j = bounds.x; j < bounds.x + bounds.width; j += step) {
                if (reco.contains(j, i)) {
                    union++;
                    if (ref.contains(j, i)) {
                        intersect++;
                    }
                } else {
                    if (ref.contains(j, i)) {
                        union++;
                    }
                }
            }
        }
        LOG.trace("intersect = {} union = {} together {}", intersect, union, (union == 0 ? 0 : ((double) intersect) / union));
        return union == 0 ? 0 : ((double) intersect) / union;
    }
}
