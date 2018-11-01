package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

import java.util.Arrays;

class DistanceStrStr implements PathCalculatorGraph.IDistance<String, String> {

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
    private final double costAcc;
    private final String[] recos;
    private final String[] refs;
    private int[] pointPrevious;
    private int[] point;
    private boolean marked;

    @Override
    public String toString() {
        return ("DistanceStrStr{" +
                "type=" + type +
                ", costs=" + costs +
                ", costAcc=" + costAcc +
                ", recos=" + Arrays.toString(recos) +
                ", refs=" + Arrays.toString(refs) +
                ", pointPrevious=" + Arrays.toString(pointPrevious) +
                ", point=" + Arrays.toString(point) +
                '}').replace("\n", "\\n");
    }

    public DistanceStrStr(TYPE type, double costs, double costAcc, String[] recos, String[] refs, int[] pointPrevious, int[] point) {
        this.type = type;
        this.costs = costs;
        this.costAcc = costAcc;
        this.recos = recos;
        this.refs = refs;
        this.pointPrevious = pointPrevious;
        this.point = point;
    }

    @Override
    public double getCosts() {
        return costs;
    }

    @Override
    public double getCostsAcc() {
        return costAcc;
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

    @Override
    public int[] getPointPrevious() {
        return pointPrevious;
    }

    @Override
    public int[] getPoint() {
        return point;
    }

    @Override
    public boolean equals(PathCalculatorGraph.IDistance<String, String> obj) {
        return Arrays.equals(point, obj.getPoint()) && getManipulation().equals(obj.getManipulation()) && Arrays.equals(pointPrevious, obj.getPointPrevious());
    }

    @Override
    public boolean isMarked() {
        return marked;
    }

    @Override
    public boolean mark(boolean mark) {
        return this.marked = mark;
    }

    @Override
    public void dispose() {
        point = null;
        pointPrevious = null;
    }

    @Override
    public int compareTo(PathCalculatorGraph.IDistanceSmall<String, String> o) {
        return Double.compare(getCostsAcc(), o.getCostsAcc());
    }
}
