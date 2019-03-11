package de.uros.citlab.errorrate.interfaces;

import de.uros.citlab.errorrate.types.Metric;

import java.util.List;
import java.util.Map;

public interface IErrorModuleWithSegmentation extends IErrorModule {

    /**
     * Calculate INS, DEL, SUB, COR to map recognition/hypothesis to reference/ground truth
     *
     * @param reco recognistion/hypothesis (empty List possible)
     * @param ref  reference/ground truth (empty List possible)
     */
    void calculateWithSegmentation(List<ILine> reco, List<ILine> ref);

    /**
     * @param reco               recognistion/hypothesis (empty List possible)
     * @param ref                reference/ground truth (emtpy List possible)
     * @param calcLineComparison if line comparations should be calculated
     * @return null, if calcLineComparison=false, otherwise result structure
     */
    List<ILineComparison> calculateWithSegmentation(List<ILine> reco, List<ILine> ref, boolean calcLineComparison);

    /**
     * @param reco               recognistion/hypothesis (multiply lines separated by \n)
     * @param ref                reference/ground truth (multiply lines separated by \n)
     * @param calcLineComparison if line comparations should be calculated
     * @return null, if calcLineComparison=false, otherwise result structure
     */
    List<ILineComparison> calculate(String reco, String ref, boolean calcLineComparison);

    /**
     * @param reco               recognistion/hypothesis (multiply lines separated by \n)
     * @param ref                reference/ground truth (multiply lines separated by \n)
     * @param calcLineComparison if line comparations should be calculated
     * @return null, if calcLineComparison=false, otherwise result structure
     */
    List<ILineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComparison);

    /**
     * @return all possible metricies for the counts that are summed up using the calculate-methods.
     */
    Map<Metric, Double> getMetrics();

}
