package de.uros.citlab.errorrate.statistic;

import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.interfaces.ICalcStatistic;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Map;

/**
 * approximate distribution by a normal distribution with mean #errors and std
 * deviation std=SQRT{(#gt-#err)*#err/#gt}. Use alpha=0.01 as default.
 * <p>
 * <p>
 * Since 19.10.2018
 *
 * @author Gundram <gundram.leifert@uni-rostock.de>
 */
public class CalcStatNormalDistribution implements ICalcStatistic, ICalcStatistic.Testable {

    ErrorModuleDynProg instance;
    private double alpha;
    private Endpoints endpoints;


    public CalcStatNormalDistribution(double alpha) {
        this.alpha = alpha;
        instance = new ErrorModuleDynProg(new CategorizerCharacterDft(), new StringNormalizerDft(), Boolean.FALSE);
    }

    @Override
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public CalcStatNormalDistribution() {
        this(0.01);
    }

    private long getValue(Map<Count, Long> map, Count val) {
        Long get = map.get(val);
        return get == null ? 0 : get;
    }

    private long getSumErrors(Map<Count, Long> map) {
        return getValue(map, Count.INS) + getValue(map, Count.DEL) + getValue(map, Count.SUB);
    }

    @Override
    public ICalcStatistic.IStatResult process(String reco, String truth) {
        instance.calculate(reco, truth);
        Map<Count, Long> map = instance.getCounter().getMap();
        long n = getValue(map, Count.GT);
        long k = getSumErrors(map);
        return processTest(k, n);
    }

    @Override
    public IStatResult processTest(long k, long n) {
        double p = ((double) k) / n;
        if (n == 0 || n * p < 5 || n * (1 - p) < 5) {
            return new ICalcStatistic.StatResult(false, 1, 1, 1, String.format("no statistic available - truth approx %.0f characters - %d done so far.", 10 / (p + 0.01) / (1.01 - p), n));
        }
        NormalDistribution nd = new NormalDistribution(k, Math.sqrt(n * n / (((double) n) - 1D) * p * (1D - p)));
        double min = endpoints.equals(Endpoints.LOWER) ? Double.NaN : getProb(nd, n, endpoints.equals(Endpoints.BOTH) ? alpha / 2 : alpha);
        double max = endpoints.equals(Endpoints.UPPER) ? Double.NaN : getProb(nd, n, endpoints.equals(Endpoints.BOTH) ? 1 - alpha / 2 : 1 - alpha);
        String out = String.format("CER=%s with %5d characters CER in [%s,%s]", getProbString(p), n, getProbString(min), getProbString(max));
        return new ICalcStatistic.StatResult(true, min, max, nd.getMean(), out);
    }

    @Override
    public void setEndPoints(Endpoints endPoints) {
        this.endpoints = endPoints;
    }

    private double getProb(NormalDistribution nd, double n, double alpha) {
        return nd.inverseCumulativeProbability(alpha) / n;
    }

    private String getProbString(double p) {
        return String.format("%6.3f%%", p * 100);
    }

    public static void main(String[] args) {
        long n = 100;
        long k = 30;
        double p = ((double) k) / n;
        NormalDistribution nd = new NormalDistribution(k, Math.sqrt(n * p * (1 - p)));
        for (double d : new double[]{0.001, 0.01, 0.025, 0.05, 0.1, 0.5, 0.9, 0.95, 0.975, 0.99, 0.999}) {
            System.out.println(String.format("%.3f: %.1f", d, nd.inverseCumulativeProbability(d)));
        }
    }
}
