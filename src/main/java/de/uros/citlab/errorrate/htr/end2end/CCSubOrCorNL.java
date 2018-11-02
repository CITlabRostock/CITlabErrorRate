package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

//TODO: also allow INS and DEL of \n between non-spacing characters? "abcd" <=> "ab\ncd"
class CCSubOrCorNL extends CCAbstract {

    public CCSubOrCorNL(Voter voter) {
        super(voter);
    }

    @Override
    public PathCalculatorGraph.DistanceSmall getNeighbourSmall(int[] point, PathCalculatorGraph.DistanceSmall dist) {
        final int y = point[0] + 1;
        final int x = point[1] + 1;
        if (y < recos.length && x < refs.length && (isLineBreakReco[y] != isLineBreakRef[x]) && (isSpaceReco[y] != isSpaceRef[x])) {

            final String reco = recos[y];
            final String ref = refs[x];
            return new DistanceStrStr(
                    isLineBreakReco[y] ? DistanceStrStr.TYPE.MERGE_LINE : DistanceStrStr.TYPE.SPLIT_LINE,
                    0,
                    dist.costsAcc,
                    reco,
                    ref,
                    point,
                    new int[]{y, x});
        }
        return null;
    }
}

