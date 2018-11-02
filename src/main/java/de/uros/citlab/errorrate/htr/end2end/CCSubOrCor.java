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
            final String reco = recos[y];
            final String ref = refs[x];
            final boolean cor = reco.equals(ref);
            int[] next = new int[]{y, x};
            if (cor) {
                return new PathCalculatorGraph.DistanceSmall(point, next, dist.costsAcc, this);
            }
            if (isLineBreakReco[y] || isLineBreakRef[x]) {
                //if only one of these is a line break: it is not allowed to substitute one character against one line break!
                return null;
            }
            //normal case: characters are unequal
            return new PathCalculatorGraph.DistanceSmall(point, next, dist.costsAcc+1, this);
//            return new DistanceStrStr(
//                    DistanceStrStr.TYPE.SUB,
//                    1,
//                    dist.costsAcc + 1,
//                    reco,
//                    ref,
//                    point,
//                    next);
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
                //line break characters are no real characters - so they do not have to be count!
                return new DistanceStrStr(DistanceStrStr.TYPE.COR_LINEBREAK, 0, dist.costsAcc, (String) null, null, point, next);
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
