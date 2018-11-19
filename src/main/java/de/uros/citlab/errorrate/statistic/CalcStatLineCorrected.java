////////////////////////////////////////////////
/// File:       StatsDummy.java
/// Created:    19.06.2016  02:08:38
/// Encoding:   UTF-8
////////////////////////////////////////////////
package de.uros.citlab.errorrate.statistic;

import java.util.Map;

import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.interfaces.ICalcStatistic;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * approximate distribution by a normal distribution as joined work with
 * Klaus-Thomas and Tobias. Use alpha=0.01 as default.
 * <p>
 * <p>
 * Since 19.06.2016
 *
 * @author Tobias <tobias.strauss@uni-rostock.de>
 */
public class CalcStatLineCorrected implements ICalcStatistic {

    private ErrorModuleDynProg instance;
    private double alpha;
    private COV cov = new COV();
    private int Cnt_T, Cnt_Y;
    private Endpoints endpoints = Endpoints.BOTH;

    public CalcStatLineCorrected(double alpha) {
        this.alpha = alpha;
        instance = new ErrorModuleDynProg(new CategorizerCharacterDft(), new StringNormalizerDft(), Boolean.FALSE);
    }

    @Override
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public CalcStatLineCorrected() {
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
    public IStatResult process(String reco, String truth) {
        instance = new ErrorModuleDynProg(new CategorizerCharacterDft(), new StringNormalizerDft(), Boolean.FALSE);
        instance.calculate(reco, truth);
        Map<Count, Long> map = instance.getCounter().getMap();
        long t = getValue(map, Count.GT);
        long y = getSumErrors(map);
        Cnt_T += t;
        Cnt_Y += y;

        double p = ((double) Cnt_Y) / Cnt_T;
        cov.add(y, t);
        boolean isValid = !(cov.getSumY() * p * (1 - p) < 10);
        if (!isValid) {
            return new StatResult(false, 1, 1, 1, String.format("no statistic available - truth approx %.0f characters - %d done so far.", 10 / (p + 0.01) / (1 - p + 0.01), cov.getSumY()));
        }
        final double factor = cov.getEX() / cov.getEY();
        double sAll = cov.getVarX()
                - 2 * cov.getCOV() * factor
                + cov.getVarY() * factor * factor;
        sAll /= cov.getEY() * cov.getEY();
        NormalDistribution nd = new NormalDistribution(p, (Math.sqrt(sAll / cov.n)));
        double min = endpoints.equals(Endpoints.LOWER) ? Double.NaN : getProb(nd, endpoints.equals(Endpoints.BOTH) ? alpha / 2 : alpha);
        double max = endpoints.equals(Endpoints.UPPER) ? Double.NaN : getProb(nd, endpoints.equals(Endpoints.BOTH) ? 1 - alpha / 2 : 1 - alpha);
        String out = String.format("CER=%s with %5d characters CER in [%s,%s]", getProbString(p), Cnt_T, getProbString(min), getProbString(max));

        return new StatResult(true, min, max, nd.getMean(), out);
    }

    @Override
    public void setEndPoints(Endpoints endPoints) {
        this.endpoints = endPoints;
    }

    private double getProb(NormalDistribution nd, double alpha) {
        return nd.inverseCumulativeProbability(alpha);
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

    private class COV {

        private long sumX;
        private long sumY;
        private long sumXY;
        private long sumXX;
        private long sumYY;
        private int n;

        private void add(long x, long y) {
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
            sumYY += y * y;
            n++;
        }

        private double getCOV() {
            return 1.0 / (n * (n - 1)) * (sumXY * n - sumX * sumY);
        }

        private double getVarX() {
            return 1.0 / (n * (n - 1)) * (sumXX * n - sumX * sumX);
        }

        private double getVarY() {
            return 1.0 / (n * (n - 1)) * (sumYY * n - sumY * sumY);
        }

        private double getEY() {
            return ((double) sumY) / n;
        }

        private double getEX() {
            return ((double) sumX) / n;
        }

        public long getSumY() {
            return sumY;
        }

    }
}
