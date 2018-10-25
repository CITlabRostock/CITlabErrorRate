package de.uros.citlab.errorrate.statistic;

import de.uros.citlab.errorrate.interfaces.ICalcStatistic;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class CalcStatTest {
    int n = 50;
    int sum = 1000000;
    double alpha = 0.3;
    int seed = new Random(1234).nextInt();
    double p = 0.34;

    @Test
    public void processBoth() {
        double countTrue1 = 0;
        double countTrue2 = 0;
        Random r = new Random(seed);
        int samples = sum / n;
//        for (int testIdx = 0; testIdx < samples; testIdx++) {
        ICalcStatistic.Testable instance1 = new CalcStatNormalDistribution();
        instance1.setAlpha(alpha);
        instance1.setEndPoints(ICalcStatistic.Endpoints.BOTH);
        ICalcStatistic.Testable instance2 = new CalcStatTDistribution();
        instance2.setAlpha(alpha);
        instance2.setEndPoints(ICalcStatistic.Endpoints.BOTH);
        ICalcStatistic.IStatResult b1 = null;
        ICalcStatistic.IStatResult b2 = null;
        BinomialDistribution b = new BinomialDistribution(n, p);
//        double sum = 0;
        int offsetLeft = 0;
        int offsetRight = offsetLeft;
        for (int i = offsetLeft; i < n + 1 - offsetRight; i++) {
            double probability = b.probability(i);
            if (probability < 1e-9) {
                continue;
            }
            b1 = instance1.processTest(i, n);
            b2 = instance2.processTest(i, n);
//            System.out.println(b);
            if (p < instance1.processTest(i + offsetRight, n).getUpperProbability() && p > instance1.processTest(i - offsetLeft, n).getLowerProbability()) {
                countTrue1 += probability;
            }
            if (p < instance2.processTest(i + offsetRight, n).getUpperProbability() && p > instance2.processTest(i - offsetLeft, n).getLowerProbability()) {
                countTrue2 += probability;
            }
//            sum += probability;
        }
        System.out.println(String.format("%.4f vs. %.4f (range = %.5f vs. %.5f)", ((double) countTrue1), ((double) countTrue2), b1.getUpperProbability() - b1.getLowerProbability(), b2.getUpperProbability() - b2.getLowerProbability()));
//        }
        Assert.assertEquals("statistic of Normal Distribution not work", 1 - alpha, ((double) countTrue1), 0.01);
        Assert.assertEquals("statistic of T-Distribution not work", 1 - alpha, ((double) countTrue2), 0.01);
    }

}