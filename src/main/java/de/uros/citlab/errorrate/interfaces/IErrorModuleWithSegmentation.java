package de.uros.citlab.errorrate.interfaces;

import java.util.List;

public interface IErrorModuleWithSegmentation extends IErrorModule {

    void calculateWithSegmentation(List<ILine> reco, List<ILine> ref);
}
