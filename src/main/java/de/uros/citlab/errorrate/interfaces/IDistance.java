package de.uros.citlab.errorrate.interfaces;


import de.uros.citlab.errorrate.types.Manipulation;

public interface IDistance {
    Manipulation getManipulation();

    String getReco();

    String getRef();
}
