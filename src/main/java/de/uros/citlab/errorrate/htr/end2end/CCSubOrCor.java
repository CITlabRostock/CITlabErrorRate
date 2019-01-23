package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

class CCSubOrCor extends CCAbstract {

    public CCSubOrCor(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int y = point[0] + 1;
        final int x = point[1] + 1;
        if (y < recos.length && x < refs.length) {
            if (recos[y].equals(refs[x])) {
                return new PathCalculatorGraph.DistanceSmall(point, new int[]{y, x}, dist.costsAcc, this);
            }
            if (isLineBreakReco[y] || isLineBreakRef[x]) {
                //if only one of these is a line break: it is not allowed to substitute one character against one line break!
                return null;
            }
            //normal case: characters are unequal
            return new PathCalculatorGraph.DistanceSmall(point, new int[]{y, x}, dist.costsAcc + 1, this);
        }
        return null;
    }

    @Override
    public PathCalculatorGraph.IDistance<String, String> getNeighbour(PathCalculatorGraph.DistanceSmall dist) {
        int[] point = dist.pointPrevious;
        int[] next = dist.point;
        final int y = next[0];
        final int x = next[1];
        final String reco = recos[y];
        final String ref = refs[x];
        final boolean cor = reco.equals(ref);
        if (cor) {
            if (isLineBreakReco[y]) {
                return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, 0, dist.costsAcc, reco, ref, point, next);
            }
            //normal case: characters are equal
            return new DistanceStrStr(DistanceStrStr.TYPE.COR, 0, dist.costsAcc, reco, ref, point, next);
        }
        if (isLineBreakReco[y] || isLineBreakRef[x]) {
            //if only one of these is a line break: it is not allowed to substitute one character against one line break!
            return null;
        }
        return new DistanceStrStr(
                DistanceStrStr.TYPE.SUB,
                1,
                dist.costsAcc,
                reco,
                ref,
                point,
                next);
    }

}
