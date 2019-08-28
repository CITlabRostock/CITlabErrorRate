package de.uros.citlab.errorrate.t2i;

import de.uros.citlab.errorrate.interfaces.IErrorModuleWithSegmentation;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.util.ObjectCounter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ErrorModuleT2I implements IErrorModuleWithSegmentation {

    private ErrorModuleT2IInner moduleCER;
    private ErrorModuleT2IInner moduleREC;
    int cor = 0;
    int hyp = 0;

    public ErrorModuleT2I(boolean allowSegmentationErrors) {
        moduleCER = new ErrorModuleT2IInner(allowSegmentationErrors);
        moduleREC = new ErrorModuleT2IInner(allowSegmentationErrors);
    }

    @Override
    public void calculateWithSegmentation(List<ILine> reco, List<ILine> ref) {
        calculateWithSegmentation(reco, ref, true);
    }

    @Override
    public List<ILineComparison> calculateWithSegmentation(List<ILine> reco, List<ILine> ref, boolean calcLineComparison) {
        List<ILineComparison> iLineComparisons = moduleCER.calculateWithSegmentation(ref, reco, true);
        hyp += reco.size();
        for (ILineComparison l : iLineComparisons) {
            if (l.getRecoText() != null && !l.getRecoText().isEmpty() && l.getRecoText().equals(l.getRefText())) {
                cor++;
            }
        }
        return moduleREC.calculateWithSegmentation(reco, ref, calcLineComparison);
    }

    @Override
    public List<ILineComparison> calculate(String reco, String ref, boolean calcLineComparison) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public List<ILineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComparison) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public Map<Metric, Double> getMetrics() {
        Map<Metric, Double> res = new HashMap<>();
        res.put(Metric.ERR, moduleCER.getMetrics().get(Metric.ERR));
        res.put(Metric.PREC, ((double) cor) / hyp);
        res.put(Metric.REC, ((double) moduleREC.getCounter().get(Count.COR)) / moduleREC.getCounter().get(Count.GT));
        return res;
    }

    @Override
    public void calculate(String reco, String ref) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public void calculate(List<String> reco, List<String> ref) {
        throw new UnsupportedOperationException("need baselines to compare");
    }

    @Override
    public List<String> getResults() {
        List<String> res = new LinkedList<>();
        Map<Metric, Double> metrics = getMetrics();
        for (Metric metric : metrics.keySet()) {
            res.add(metric.toString() + "=" + metrics.get(metric));
        }
        return res;
    }

    @Override
    public ObjectCounter<Count> getCounter() {
        return null;
    }

    @Override
    public void reset() {
        moduleCER.reset();
        moduleREC.reset();
        cor = 0;
        hyp = 0;
    }
}
