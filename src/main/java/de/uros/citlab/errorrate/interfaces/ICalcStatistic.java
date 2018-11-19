package de.uros.citlab.errorrate.interfaces;


/**
 * With classes implementing this interface it is possible to calculate confidence intervals for a CER calculated from
 * text line samples.<br/>
 * First, {@link #setAlpha(double)} have to be set to configure the confidence level.
 * Typical values are 0.05 or 0.01.<br/>
 * Second, the {@link #setEndPoints(Endpoints)} have to be set. If one want to have a statement
 * "With the probability of (1-alpha) the CER of the given recognition is in the interval [lower, upper]",
 * set {@link Endpoints#BOTH} and use [{@link IStatResult#getLowerProbability()},{@link IStatResult#getUpperProbability()}].
 * For a statement "With the probability of (1-alpha) the CER is lower than VALUE, set  {@link Endpoints#UPPER} and use
 * VALUE={@link IStatResult#getUpperProbability()}<br/>
 * After having initialized these both parameters, the method {@link #process(String, String)} have to be applied.
 * The return structure {@link IStatResult} can be used to further process the result.
 */
public interface ICalcStatistic {

    /**
     * sets the confidence level that the real probability is in this interval for the given processed data
     *
     * @param alpha
     */
    void setAlpha(double alpha);

    /**
     * calculates the error between recognition and ground truth and returns a result file.
     *
     * @param reco  recognition / hypothesis of HTR for a line (not null)
     * @param truth ground truth / reference for a line (not null)
     * @return result structure
     */
    IStatResult process(String reco, String truth);

    /**
     * set end points for confidence intervall.
     *
     * @param endPoints
     */
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
        ,
        LOWER/**
         * algorithm return a confidence interval with bounds on both sides (p_lower < p < p_upper)
         */
        ,
        BOTH
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
         * @return true if statistic is large enough
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
}
