package de.uros.citlab.errorrate.interfaces;

import java.util.List;

/**
 * Result structure of methods @{@link IErrorModuleWithSegmentation#calculate(String, String, boolean)},
 * @{@link IErrorModuleWithSegmentation#calculate(List, List, boolean)}
 * and @{@link IErrorModuleWithSegmentation#calculateWithSegmentation(List, List, boolean)} , if last argument is set to true.
 * It contains information of the mapping of text lines (typically recognition vs. reference) given as first and second
 * argument to the method.
 */
public interface ILineComparison {
    /**
     * @return index of recognition text line
     */
    int getRecoIndex();

    /**
     * @return index of reference text line
     */
    int getRefIndex();
    /**
     * @return index in the recognition text line
     */
    int getInnerRecoIndex();

    /**
     * @return index in the of reference text line
     */
    int getInnerRefIndex();

    /**
     * @return text of recognition
     */
    String getRefText();

    /**
     * @return text of reference
     */
    String getRecoText();

    /**
     * @return List of manipulations that had to be done to come from the recognition to the reference
     */
    List<IPoint> getPath();

}

