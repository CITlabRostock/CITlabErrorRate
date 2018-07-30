package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.util.ObjectCounter;

import java.util.LinkedHashMap;
import java.util.Map;

public class Result {

    private final Method method;
    private final ObjectCounter<Count> counts = new ObjectCounter<>();
    private final Map<Metric, Double> metrics = new LinkedHashMap<>();
    private boolean isCalculated = false;

    public Result(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public ObjectCounter<Count> getCounts() {
        return counts;
    }

    public Long getCount(Count count) {
        return counts.get(count);
    }

    public void addCounts(ObjectCounter<Count> counts) {
        this.counts.addAll(counts);
        isCalculated = false;
    }

    private void calculate() {
        if (!isCalculated) {
            metrics.clear();
            switch (method) {
                case BOT:
                case BOT_ALNUM:
                    double tp = counts.get(Count.TP);
                    double fp = counts.get(Count.FP);
                    double fn = counts.get(Count.FN);
                    double prec = (fp + tp) == 0 ? 1.0 : tp / (fp + tp);
                    double rec = (fn + tp) == 0 ? 1.0 : tp / (fn + tp);
                    double f = (prec + rec) == 0 ? 0 : 2 * prec * rec / (prec + rec);
                    metrics.put(Metric.REC, rec);
                    metrics.put(Metric.PREC, prec);
                    metrics.put(Metric.F, f);
                    break;
                case CER:
                case CER_ALNUM:
                case WER:
                case WER_ALNUM:
                    double err = counts.get(Count.ERR);
                    double cor = counts.get(Count.COR);
                    double gt = counts.get(Count.GT);
                    metrics.put(Metric.ACC, gt == 0 ? 1.0 : cor / gt);
                    metrics.put(Metric.ERR, gt == 0 ? 0.0 : err / gt);
                    break;
                default:
                    throw new RuntimeException("unknown method '" + method + "'.");
            }
            isCalculated = true;
        }
    }

    public Map<Metric, Double> getMetrics() {
        calculate();
        return metrics;
    }

    public double getMetric(Metric metric) {
        return getMetrics().get(metric);
    }

}
