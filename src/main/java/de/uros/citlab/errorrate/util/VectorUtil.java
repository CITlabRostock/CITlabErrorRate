package de.uros.citlab.errorrate.util;

public class VectorUtil {

    public static double max(double[] vector) {
        if (vector == null) {
            throw new NullPointerException();
        }
        if (vector.length == 0) {
            throw new IndexOutOfBoundsException("vector has length 0");
        }
        double max = vector[0];
        for (int i = 1; i < vector.length; i++) {
            max = Math.max(max, vector[i]);
        }
        return max;
    }

    public static int max(int[] vector) {
        if (vector == null) {
            throw new NullPointerException();
        }
        if (vector.length == 0) {
            throw new IndexOutOfBoundsException("vector has length 0");
        }
        int max = vector[0];
        for (int i = 1; i < vector.length; i++) {
            max = Math.max(max, vector[i]);
        }
        return max;
    }
}
