package de.uros.citlab.errorrate.interfaces;


public interface ICalcStatistic {

    void setAlpha(double alpha);

    IStatResult process(String reco, String truth);

    void setEndPoints(Endpoints endPoints);

    /**
     * which endpoints of confidence interval should be calculated?
     */
    public enum Endpoints {

        /**
         * algorithm return a confidence interval with bound on the left side (p > p_lower)
         */
        UPPER
        /**
         * algorithm return a confidence interval with bound on the right side (p < p_upper)
         */
        , LOWER/**
         * algorithm return a confidence interval with bounds on both sides (p_lower < p < p_upper)
         */
        , BOTH
    }

    interface IStatResult {

        /**
         * upper bound for confidence interval with significance level alpha
         *
         * @return upper bound if EndPoints is UPPER or BOTH, Double.NaN otherwise
         */
        double getUpperProbability();

        /**
         * lower bound for confidence interval with significance level alpha
         *
         * @return lower bound if EndPoints is LOWER or BOTH, Double.NaN otherwise
         */
        double getLowerProbability();

        /**
         * @return mean of samples
         */
        double getMean();

        /**
         * @return a human-readable string that contains the main information of the actual statistic
         */
        String getText();

        /**
         * @return true is statistic is large enough
         */
        boolean isValid();

    }

    class StatResult implements IStatResult {

        private boolean isValid;
        private double minProp;
        private double maxProp;
        private double mean;
        private String text;

        public StatResult(boolean isValid, double minProp, double maxProp, double mean, String text) {
            this.isValid = isValid;
            this.minProp = minProp;
            this.maxProp = maxProp;
            this.mean = mean;
            this.text = text;
        }

        @Override
        public double getUpperProbability() {
            return maxProp;
        }

        @Override
        public double getLowerProbability() {
            return minProp;
        }

        @Override
        public double getMean() {
            return mean;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isValid() {
            return isValid;
        }

        @Override
        public String toString() {
            return "StatResult{" +
                    "isValid=" + isValid +
                    ", minProp=" + minProp +
                    ", maxProp=" + maxProp +
                    ", mean=" + mean +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    interface Testable extends ICalcStatistic {

        IStatResult processTest(long k, long n);
    }
}
