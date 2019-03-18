package de.uros.citlab.errorrate.interfaces;

import java.awt.*;

public interface IAdjazentCalculator {
    boolean[][] getAdjacent(Polygon[] reco, Polygon[] ref);

    void setThreshold(double thresh);
}
