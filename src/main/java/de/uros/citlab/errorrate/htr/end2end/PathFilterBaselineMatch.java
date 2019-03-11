package de.uros.citlab.errorrate.htr.end2end;

import de.uros.citlab.errorrate.types.PathCalculatorGraph;

public class PathFilterBaselineMatch implements PathCalculatorGraph.PathFilter<String, String> {

    private AlignmentTask alignmentTask = null;
    private PathCalculatorGraph.PathFilter intern = null;

    public PathFilterBaselineMatch(PathCalculatorGraph.PathFilter intern) {
        this.intern = intern;
    }

    public void setAlignmentTask(AlignmentTask alignmentTask) {
        this.alignmentTask = alignmentTask;
    }

    @Override
    public void init(String[] strings, String[] strings2) {
        if (intern != null) {
            intern.init(strings, strings2);
        }
    }

    @Override
    public boolean addNewEdge(PathCalculatorGraph.DistanceSmall newDistance) {
        int[] point = newDistance.point;
        //-1 because empty element is added at the beginning of reco and ref
        if (alignmentTask.isSameLine(point[0] - 1, point[1] - 1)) {
            return intern == null ? true : intern.addNewEdge(newDistance);
        }
//        if(newDistance.costCalculator instanceof CCInsLine||newDistance.costCalculator instanceof CCDelLine){
//            System.out.println(newDistance.costCalculator.getClass().getName());
//            System.out.println("stop");
//        }
        return false;
    }

    @Override
    public boolean followPathsFromBestEdge(PathCalculatorGraph.DistanceSmall bestDistance) {
        int[] point = bestDistance.point;
        if (alignmentTask.isSameLine(point[0] - 1, point[1] - 1)) {
            return intern == null ? true : intern.addNewEdge(bestDistance);
        }
//        if(bestDistance.costCalculator instanceof CCInsLine||bestDistance.costCalculator instanceof CCDelLine){
//            System.out.println(bestDistance.costCalculator.getClass().getName());
//            System.out.println("stop");
//            boolean sameLine = alignmentTask.isSameLine(point[0], point[1]);
//        }
        return false;
    }
}
