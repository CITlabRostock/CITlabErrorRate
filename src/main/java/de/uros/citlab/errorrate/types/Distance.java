package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.interfaces.IPoint;

public class Distance implements IPoint {


    private final Manipulation manipulation;
    private final String reco;
    private final String ref;

    public Distance(Manipulation manipulation, String reco, String ref) {
        this.manipulation = manipulation;
        this.reco = reco;
        this.ref = ref;
    }

    @Override
    public Manipulation getManipulation() {
        return manipulation;
    }

    @Override
    public String getReco() {
        return reco;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public String toString() {
        return "{" + manipulation + ":'" + (reco == null ? "" : reco) + "'=>'" + (ref == null ? "" : ref) + "'}";
    }
}
