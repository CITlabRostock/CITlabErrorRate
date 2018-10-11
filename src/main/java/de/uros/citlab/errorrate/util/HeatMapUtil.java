package de.uros.citlab.errorrate.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeatMapUtil {
    private static List<Color> colorMap2 = new ArrayList<>();
    private static List<Color> colorMap3 = new ArrayList<>();
    private static List<Color> colorMap4 = new ArrayList<>();
    private static List<Color> colorMap5 = new ArrayList<>();
    private static List<Color> colorMap7 = new ArrayList<>();
//    private static List<Color> colorMap5;

    static {
        int[] black = new int[]{0, 0, 0};//black
        int[] blue = new int[]{0, 0, 1};//blue
        int[] cyan = new int[]{0, 1, 1};//cyan
        int[] green = new int[]{0, 1, 0};//green
        int[] yellow = new int[]{1, 1, 0};//yellow
        int[] red = new int[]{1, 0, 0};//red
        int[] white = new int[]{1, 1, 1};//white
        colorMap7 = getMap(new int[][]{black, blue, cyan, green, yellow, red, white});
        colorMap4 = getMap(new int[][]{black, red, yellow, white});
        colorMap5 = getMap(new int[][]{blue, cyan, green, yellow, red});
        colorMap3 = getMap(new int[][]{blue, green, red});
        colorMap2 = getMap(new int[][]{black, white});
    }

    private static java.util.List<Color> getMap(int[][] colors) {
        java.util.List<Color> res = new ArrayList<>(256 * (colors.length - 1));
        for (int c = 0; c < colors.length - 1; c++) {
            int[] low = colors[c];
            int[] high = colors[c + 1];
            for (int i = 0; i < 256; i++) {
                res.add(new Color(
                        high[0] * i + low[0] * (255 - i),
                        high[1] * i + low[1] * (255 - i),
                        high[2] * i + low[2] * (255 - i)));
            }
        }
        return res;
    }

    public static void save(BufferedImage bi, File out){
        String substring = out.getName().substring(1+out.getName().lastIndexOf('.'));
        try {
            ImageIO.write(bi, substring, out);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save file '"+out.getAbsolutePath()+"'",e);
        }
    }

    public static BufferedImage getHeatMap(double[][] matrix, int colors) {
        return getHeatMap(matrix, colors, false);
    }

    public static BufferedImage getHeatMap(double[][] matrix, int colors, boolean invert) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] ds : matrix) {
            for (int i = 0; i < ds.length; i++) {
                min = Math.min(ds[i], min);
                max = Math.max(ds[i], max);
            }
        }
        return invert ? getHeatMap(matrix, max, min, colors) : getHeatMap(matrix, min, max, colors);
    }

    public static BufferedImage getHeatMap(double[][] matrix, double low, double high, int colors) {
        List<Color> colorMap = null;
        switch (colors) {
            case 2:
                colorMap = colorMap2;
                break;
            case 3:
                colorMap = colorMap3;
                break;
            case 4:
                colorMap = colorMap4;
                break;
            case 5:
                colorMap = colorMap5;
                break;
            case 7:
                colorMap = colorMap7;
                break;
            default:
                throw new RuntimeException("unknown color count " + colors);
        }
        double factor = (colorMap.size() - 1) / (high - low);
        double offset = 0.5 - low * factor;
        BufferedImage bi = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < matrix.length; i++) {
            double[] vec = matrix[i];
            for (int j = 0; j < vec.length; j++) {
                bi.setRGB(j, i, colorMap.get((int) (vec[j] * factor + offset)).getRGB());
            }
        }
        return bi;
    }

}
