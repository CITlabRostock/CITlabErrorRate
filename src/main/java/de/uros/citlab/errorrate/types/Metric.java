/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.types;

/**
 * @author gundram
 */
public enum Metric {

    /**
     * Error [0,infinity) (0 = perfect). Mostly in [0,1]<br/>
     * ERR = (#INS + #DEL + #SUB) / #GT
     */
    ERR,
    /**
     * Accuracy [0,1], (1 = perfect).<br/>
     * The Accuracy can be seen as the intersection of the sets GT and HYP<br/>
     * ACC = #COR / (#GT + #HYP - #COR)
     */
    ACC,
    /**
     * For retrieval measures:<br/>
     * Precision [0,1], (1 = perfect)<br/>
     * PREC = #TP / (#FP + #TP)<br/>
     * <br/>
     * For error rate measures:<br/>
     * Precision [0,1], (1 = perfect)
     * PREC = #COR / #HYP
     */
    PREC,
    /**
     * For retrieval measures:<br/>
     * Recall [0,1], (1 = perfect)<br/>
     * REC = #TP / (#FN + #TP)
     * <br/>
     * For error rate measures:<br/>
     * Recall [0,1], (1 = perfect)
     * REC = #COR / #GT
     */
    REC,
    /**
     * For retrieval measures:<br/>
     * F-measure [0,1], (1 = perfect)<br/>
     * F = 2 * #TP / (2 * #TP + #FP + #TP)
     * For error rate measures:<br/>
     * F-measure [0,1], (1 = perfect)<br/>
     * F = 2 * #COR / (#GT + #HYP)
     */
    F
}
