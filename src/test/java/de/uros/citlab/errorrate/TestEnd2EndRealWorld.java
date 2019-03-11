/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.interfaces.IPoint;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.StopWatch;
import de.uros.citlab.errorrate.util.ExtractUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Here every one can add groundtruth (GT) and hypothesis (HYP) text. Then some
 * parameters can be given (uppercase, word error rate,bag of words and only
 * letters and numbers). The output is a map with counts on the comparison.
 * Counts are:<br>
 * Dynamic programming (bagoftokens==false):<br>
 * COR=Correct<br>
 * SUB=Substitution<br>
 * INS=Insertion (gt has more tokens)<br>
 * DEL=Deletion (hyp has more tokens)<br>
 * <br>
 * Bag of Tokens (bagoftokens==true):<br>
 * TP=True Positives <br>
 * FN=False Negatives (gt has more tokens)<br>
 * FP=False Posotives (hyp has more tokens)<br>
 *
 * @author gundram
 */
public class TestEnd2EndRealWorld {

    private enum Result {
        F1_ATR1("ATR_1/page_f1"),
        //        F1_ATR2("ATR_2/page_f1"),
//        F2_ATR1("ATR_1/page_f2"),
//        F2_ATR2("ATR_2/page_f2"),
//        F3_ATR1("ATR_1/page_f3"),
        F3_ATR2("ATR_2/page_f3");
        private String path;

        Result(String path) {
            this.path = path;
        }

        File getPath() {
            return new File("src/test/resources/end2end/" + path);
        }
    }

    final static int[] testIndexes = new int[]{0};
    private static final boolean[] trueFalse = new boolean[]{true, false};
    //    private static HashMap<boolean[], double[]> expecteds = new HashMap<>();
    private static double[][][][] expectedsSegmentation = new double[2][2][2][];

    static {
        //restrict reading order - restrict geometry - allow segmentation
        expectedsSegmentation[0][0][0] = new double[]{0.1750358680057389, 0.20900198685618218, 0.19234894529853414, 0.21256797583081571};
        expectedsSegmentation[0][0][1] = new double[]{0.16606886657101866, 0.19715726730857405, 0.1873435824097247, 0.17166163141993956};
        expectedsSegmentation[0][1][0] = new double[]{0.17539454806312768, 0.20395796578967554, 0.19234894529853414, 0.22212137714043687};
        expectedsSegmentation[0][1][1] = new double[]{0.16594904915679942, 0.19749312136961175, 0.1873435824097247, 0.175577597677513};

        expectedsSegmentation[1][0][0] = new double[]{0.2568149210903874, 0.24629374904478069, 0.19234894529853414, 0.3113595166163142};
        expectedsSegmentation[1][0][1] = new double[]{0.24497847919655666, 0.24232003668042182, 0.1873435824097247, 0.2891238670694864};
        expectedsSegmentation[1][1][0] = new double[]{0.2777977044476327, 0.25981965459269446, 0.19234894529853414, 0.3444712990936556};
        expectedsSegmentation[1][1][1] = new double[]{0.2666786226685796, 0.2537062509552193, 0.1873435824097247, 0.32235649546827794};

    }

    @Test
    public void testSegmentBug() {
        File gtFile = new File("src/test/resources/end2end/segment_bug/071_085_002_gt.xml");
        File hypFile = new File("src/test/resources/end2end/segment_bug/071_085_002_hyp.xml");
        List<ILine> linesGT = ExtractUtil.getLinesFromFile(gtFile);
        List<ILine> linesHyp = ExtractUtil.getLinesFromFile(hypFile);
        ErrorModuleEnd2End module = new ErrorModuleEnd2End(false, false, true, false);
        module.setCountManipulations(ErrorModuleEnd2End.CountSubstitutions.ERRORS);
        List<ILineComparison> lineComparisons = module.calculateWithSegmentation(linesHyp, linesGT, true);
        for (ILineComparison cmp : lineComparisons) {
            int cnt = 0;
            for (IPoint distance : cmp.getPath()) {
                switch (distance.getManipulation()) {
                    case INS:
                    case DEL:
                    case SUB:
                        cnt++;
                }
            }
            System.out.println("----------- " + cnt + "/" + cmp.getPath().size() + " -----------");
            System.out.println(cmp.getRecoIndex() + " '" + cmp.getRecoText() + "'");
            System.out.println(cmp.getRefIndex() + " '" + cmp.getRefText() + "'");
        }
    }

    @Test
    public void testRO_Consistance() {
        List<String> linesGT = Arrays.asList("we only want to test", "if a sparate input", "is as good as a", "combined", "input");
        List<String> linesHyp = Arrays.asList("sde onsdy wantdsto tedst", "isd a ssdarate input", "is asssdgood as a", "combdned", "iaput");
        de.uros.citlab.errorrate.types.Result resExpect = new de.uros.citlab.errorrate.types.Result(Method.CER);
        {
            ErrorModuleEnd2End module = new ErrorModuleEnd2End(true, false, false, false);
            for (int i = 0; i < linesGT.size(); i++) {
                module.calculate(linesHyp.get(i), linesGT.get(i));
            }
            de.uros.citlab.errorrate.types.Result res = new de.uros.citlab.errorrate.types.Result(Method.CER);
            resExpect.addCounts(module.getCounter());
        }
        for (boolean restrictReadingOrder : trueFalse) {
            for (boolean allowSegmentationErrors : trueFalse) {
                de.uros.citlab.errorrate.types.Result resActual = new de.uros.citlab.errorrate.types.Result(Method.CER);
                ErrorModuleEnd2End module = new ErrorModuleEnd2End(restrictReadingOrder, false, allowSegmentationErrors, false);
                module.calculate(linesHyp, linesGT);
                resActual.addCounts(module.getCounter());
                Map<Count, Long> countActual = resActual.getCounts().getMap();
                Map<Count, Long> countExpect = resExpect.getCounts().getMap();
                for (Count count : countActual.keySet()) {
                    Assert.assertEquals("for R=" + restrictReadingOrder + ", S=" + allowSegmentationErrors + " and count " + count + " has to be the same for same task", countExpect.get(count), countActual.get(count));
                }
            }
        }
    }

    @Test
    public void testAllPages() throws IOException {
        for (boolean restrictReadingOrder : new boolean[]{true, false}) {
            for (boolean restrictGeometry : new boolean[]{true, false}) {
                for (boolean allowSegmentationErrors : new boolean[]{true, false}) {
//                    StringBuilder sb = new StringBuilder();
                    double[] doubles = expectedsSegmentation[restrictReadingOrder ? 1 : 0][restrictGeometry ? 1 : 0][allowSegmentationErrors ? 1 : 0];
//                    sb.append("expectedsSegmentation").append(".put(new boolean[]{" + restrictReadingOrder + "," + restrictGeometry + "," + allowSegmentationErrors + "}, new double[]{");
                    for (int i =0; i < testIndexes.length; i++) {
                        double expected = doubles[testIndexes[i]];
//                if (expected != 0.0) {
//                    continue;
//                }
                        double cer = testGermania(restrictReadingOrder, restrictGeometry, allowSegmentationErrors, testIndexes[i]);
//                        sb.append(cer);
//                        if (i < doubles.length - 1) {
//                            sb.append(",");
//                        }
                        Assert.assertEquals("CER of page " + i + " and mode R=" + restrictReadingOrder + ", G=" + restrictGeometry + ", S=" + allowSegmentationErrors + " is wrong", expected, cer, 0.001);
                    }
//                    sb.append("});");
//                    System.out.println(sb);
                }
            }
        }
    }

    public void testGermania0_RO() throws IOException {
        double cer = testGermania(true, false, false, 0);
        Assert.assertEquals("CER differs from previous", expectedsSegmentation[1][0][0][0], cer, 0.00001);
        //0m 04s 195mw -> no debug output
        //0m 18s 022ms -> introduce static arrays in CCAbstract
        //0m 21s 680ms -> with String[] as reco and ref
        //2m 53s 392ms -> with List<String> as reco and ref
    }

    public void testGermania0_RO_SEG() throws IOException {
        double cer = testGermania(true, false, true, 0);
        Assert.assertEquals("CER differs from previous", expectedsSegmentation[1][0][1][0], cer, 0.00001);
        //0m 05s 688ms -> add pathFilter for jumpReco
        //0m 24s 564ms -> introduce static arrays in CCAbstract
        //0m 30s 910ms -> with String[] as reco and ref
        //6m 59s 357ms -> with List<String> as reco and ref
    }

//    public double testGermania(ErrorModuleEnd2End.Mode mode, int image) throws IOException {
//        return testGermania(mode, image, false);
//    }

    public double testGermania(boolean restrictReadingOrder, boolean restrictGeometry, boolean allowSegmentationErrors,
                               int image) throws IOException {
        StopWatch sw = new StopWatch();
        ErrorModuleEnd2End end2End = new ErrorModuleEnd2End(restrictReadingOrder, restrictGeometry, allowSegmentationErrors, false);
//        end2End.setThresholdCouverage(0.0);
//        end2End.setSizeProcessViewer(6000);
//        end2End.setFileDynProg(new File("out.png"));
        Result gtResult = Result.F1_ATR1;
        Result hypResult = Result.F3_ATR2;
        File[] gts = new File(gtResult.getPath().getPath()).listFiles();
        File[] hyps = new File(hypResult.getPath().getPath()).listFiles();
        Arrays.sort(gts);
        Arrays.sort(hyps);
//        for (int i = 0; i < 1; i++) {
        File hyp = hyps[image];
        File gt = gts[image];
        List<ILine> hypLines = ExtractUtil.getLinesFromFile(hyp.getPath());
        List<ILine> gtLines = ExtractUtil.getLinesFromFile(gt.getPath());
        int cnt = 0;
        for (int i = 0; i < gtLines.size(); i++) {
            cnt += gtLines.get(i).getText().length();
        }
        System.out.println("test with mode R=" + restrictReadingOrder + ", G=" + restrictGeometry + ", S=" + allowSegmentationErrors + " for " + cnt + " characters");
        end2End.calculateWithSegmentation(hypLines, gtLines);
        ObjectCounter<Count> counter = end2End.getCounter();
        System.out.println(((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT));
        System.out.println(counter);
        sw.stop();
        System.out.println("Stopwatch for mode R=" + restrictReadingOrder + ", G=" + restrictGeometry + ", S=" + allowSegmentationErrors + " and image " + image + " = " + sw.toString());
        return ((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT);
//        }
    }
}
