package de.uros.citlab.errorrate.statistic;

import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.interfaces.ICalcStatistic;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class CalcStatTDistribution implements ICalcStatistic, ICalcStatistic.Testable {
    ErrorModuleDynProg instance = new ErrorModuleDynProg(new CategorizerCharacterDft(), null, Boolean.FALSE);
    private double alpha;
    private Endpoints endpoints;


    @Override
    public void setAlpha(double alpha) {
        this.alpha = alpha;
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
        instance.calculate(reco, truth);
        Map<Count, Long> map = instance.getCounter().getMap();
        long n = getValue(map, Count.GT);
        long k = getSumErrors(map);
        double p = ((double) k) / n;
        if (n <= 2) {
            return new StatResult(false, 1, 1, 1, String.format("no statistic available - truth approx %.0f characters - %d done so far.", 10 / (p + 0.01) / (1.01 - p), n));
        }
        double sSquare = 1D / ((double) n - 1) * (k * (1 - p) * (1 - p) + (n - k) * p * p);
        double s = Math.sqrt(sSquare);
        TDistribution distribution = new TDistribution(null, n - 1);
//        double t = distribution.inverseCumulativeProbability(1-alpha / 2);
//        double min = endpoints.equals(Endpoints.LOWER) ? Double.NaN : distribution.inverseCumulativeProbability(endpoints.equals(Endpoints.BOTH) ? alpha / 2 : alpha);
        double max = distribution.inverseCumulativeProbability(endpoints.equals(Endpoints.BOTH) ? 1 - alpha / 2 : 1 - alpha);
//
        AbstractRealDistribution gauss = new NormalDistribution();
        //evtl N-1??
        double upper = endpoints.equals(Endpoints.UPPER) ? Double.NaN : p + max * s / Math.sqrt(n);
        double lower = endpoints.equals(Endpoints.LOWER) ? Double.NaN : p - max * s / Math.sqrt(n);


//        double t = T.tTest(0.4, statisticalSummary);
//        System.out.println(t);
//        NormalDistribution nd = new NormalDistribution(k, Math.sqrt(n * p * (1 - p)));
        return new StatResult(true, lower, upper, p, "only a test");
    }

    @Override
    public IStatResult processTest(long k, long n) {
        double p = ((double) k) / n;
        if (n <= 2) {
            return new StatResult(false, 1, 1, 1, String.format("no statistic available - truth approx %.0f characters - %d done so far.", 10 / (p + 0.01) / (1.01 - p), n));
        }
        double sSquare = 1D / ((double) n - 1) * (k * (1 - p) * (1 - p) + (n - k) * p * p);
        double s = Math.sqrt(sSquare);
        TDistribution distribution = new TDistribution(null, n - 1);
        double max = distribution.inverseCumulativeProbability(endpoints.equals(Endpoints.BOTH) ? 1 - alpha / 2 : 1 - alpha);
//
        AbstractRealDistribution gauss = new NormalDistribution();
        //evtl N-1??
        double upper = endpoints.equals(Endpoints.UPPER) ? Double.NaN : p + max * s / Math.sqrt(n);
        double lower = endpoints.equals(Endpoints.LOWER) ? Double.NaN : p - max * s / Math.sqrt(n);
        return new StatResult(true, lower, upper, p, "only a test");
    }

    @Override
    public void setEndPoints(Endpoints endPoints) {
        this.endpoints = endPoints;
    }

    public static void main(String[] args) {
        int n = 10;
        TDistribution d10 = new TDistribution(null, 9);
        TDistribution d100 = new TDistribution(null, 99);
        TDistribution d1000 = new TDistribution(null, 999);
        TDistribution d10000 = new TDistribution(null, 9999);
        TDistribution d100000 = new TDistribution(null, 99999);
        AbstractRealDistribution gauss = new NormalDistribution();

        TreeMap<String, AbstractRealDistribution> dists = new TreeMap<>();
        dists.put("D10", d10);
        dists.put("D100", d100);
        dists.put("D1000", d1000);
        dists.put("D10000", d10000);
        dists.put("D100000", d100000);
        dists.put("Gauß", gauss);
        double alpha = 0.05;
        for (String dist : dists.keySet()) {
            System.out.println(String.format("%10s : %s", dist, dists.get(dist).inverseCumulativeProbability(0.975)));
        }
        System.exit(-1);

        StringBuilder reco = new StringBuilder();
        StringBuilder ref = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 200; i++) {
            int refVal = r.nextInt(10);
            int recoVal = r.nextDouble() < 0.2 ? (refVal + r.nextInt(9)) % 10 : refVal;
            ref.append((char) ('0' + refVal));
            reco.append((char) ('0' + recoVal));
        }
        CalcStatTDistribution instance = new CalcStatTDistribution();
        IStatResult process = instance.process(reco.toString(), ref.toString());
        System.out.println(process);
//        long n = 100;
//        long k = 30;
//        double p = ((double) k) / n;
//        NormalDistribution nd = new NormalDistribution(k, Math.sqrt(n * p * (1 - p)));
//        for (double d : new double[]{0.001, 0.01, 0.025, 0.05, 0.1, 0.5, 0.9, 0.95, 0.975, 0.99, 0.999}) {
//            System.out.println(String.format("%.3f: %.1f", d, nd.inverseCumulativeProbability(d)));
//        }
    }
}
