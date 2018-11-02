package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class DistanceStrStr extends PathCalculatorGraph.DistanceSmall implements PathCalculatorGraph.IDistance<String, String> {

    DistanceStrStr(TYPE type, double costs, double costAcc, String reco, String ref, int[] pointPrevious, int[] point) {
        this(type, costs, costAcc, reco == null ? null : new String[]{reco}, ref == null ? null : new String[]{ref}, pointPrevious, point);
    }


    enum TYPE {
        INS, DEL, SUB, COR,
        JUMP_RECO, COR_LINEBREAK,
        INS_LINE, DEL_LINE,
        SPLIT_LINE, MERGE_LINE
    }

    private final TYPE type;
    private final double costs;
    private final String[] recos;
    private final String[] refs;
    private boolean marked;

    @Override
    public String toString() {
        return ("DistanceStrStr{" +
                "type=" + type +
                ", costs=" + costs +
                ", costAcc=" + super.costsAcc +
                ", recos=" + Arrays.toString(recos) +
                ", refs=" + Arrays.toString(refs) +
                ", pointPrevious=" + Arrays.toString(super.pointPrevious) +
                ", point=" + Arrays.toString(super.point) +
                '}').replace("\n", "\\n");
    }

    public DistanceStrStr(TYPE type, double costs, double costAcc, String[] recos, String[] refs, int[] pointPrevious, int[] point) {
        super(pointPrevious,point,costAcc,null);
        this.type = type;
        this.costs = costs;
        this.recos = recos;
        this.refs = refs;
    }

    @Override
    public int[] getPointPrevious() {
        return super.pointPrevious;
    }

    @Override
    public int[] getPoint() {
        return super.point;
    }

    @Override
    public double getCostsAcc() {
        return super.costsAcc;
    }

    @Override
    public double getCosts() {
        return costs;
    }

    @Override
    public String[] getRecos() {
        return recos;
    }

    @Override
    public String[] getReferences() {
        return refs;
    }

    @Override
    public String getManipulation() {
        return type == null ? null : type.toString();
    }

    public boolean isMarked() {
        return marked;
    }

    public void mark(boolean mark) {
        this.marked = mark;
    }

    @Override
    public PathCalculatorGraph.IDistance getLargeDistance() {
        return this;
    }
}
