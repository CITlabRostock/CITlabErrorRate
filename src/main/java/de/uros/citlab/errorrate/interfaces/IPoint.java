package de.uros.citlab.errorrate.interfaces;


import de.uros.citlab.errorrate.types.Manipulation;

/**
 * Point of a path, that maps a recognition to a reference (see @{@link ILineComparison#getPath()})
 */
public interface IPoint {
    /**
     * @return Manipulation that had to be done to come from recognition to reference
     */
    Manipulation getManipulation();

    /**
     * @return recognition or null, if manipulation is @{@link Manipulation#INS}
     */
    String getReco();

    /**
     * @return reference or null, if manipulation is @{@link Manipulation#DEL}
     */
    String getRef();
}
