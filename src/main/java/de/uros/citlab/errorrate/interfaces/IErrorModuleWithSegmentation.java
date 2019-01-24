package de.uros.citlab.errorrate.interfaces;

import de.uros.citlab.errorrate.types.Metric;

import java.util.List;
import java.util.Map;

public interface IErrorModuleWithSegmentation extends IErrorModule {

    void calculateWithSegmentation(List<ILine> reco, List<ILine> ref);

    List<LineComparison> calculateWithSegmentation(List<ILine> reco, List<ILine> ref, boolean calcLineComarison);

    List<LineComparison> calculate(String reco, String ref, boolean calcLineComarison);

//    List<LineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComarison);

    List<LineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComarison);

    Map<Metric, Double> getMetrics();

    interface LineComparison {
        int getRecoIndex();

        int getRefIndex();

        String getRefText();

        String getRecoText();

        List<IDistance> getPath();
    }

}
