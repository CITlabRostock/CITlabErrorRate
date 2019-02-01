/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.interfaces;

import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.util.ObjectCounter;

import java.awt.*;
import java.util.List;

/**
 *
 * @author gundram
 */
public interface IErrorModule {

    void calculate(String reco, String ref);

    void calculate(List<String> reco, List<String> ref);

    List<String> getResults();

    ObjectCounter<Count> getCounter();

    void reset();
}

