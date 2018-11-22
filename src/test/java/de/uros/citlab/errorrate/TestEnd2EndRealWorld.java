/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.StopWatch;
import de.uros.citlab.errorrate.util.ExtractUtil;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.primaresearch.dla.page.Page;
import org.primaresearch.dla.page.layout.physical.Region;
import org.primaresearch.dla.page.layout.physical.text.LowLevelTextObject;
import org.primaresearch.dla.page.layout.physical.text.impl.TextLine;
import org.primaresearch.dla.page.layout.physical.text.impl.TextRegion;
import org.primaresearch.io.UnsupportedFormatVersionException;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.List;

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

    private static HashMap<ErrorModuleEnd2End.Mode, double[]> expecteds = new HashMap<>();
    private static HashMap<ErrorModuleEnd2End.Mode, double[]> expectedsSegmentation = new HashMap<>();

    static {
        expecteds.put(ErrorModuleEnd2End.Mode.RO, new double[]{0.2569942611190818, 0.24629374904478069, 0.19306399713979264, 0.311484323083429});
        expecteds.put(ErrorModuleEnd2End.Mode.NO_RO, new double[]{0.1750358680057389, 0.20357634112792297, 0.1927064712191634, 0.21337521899353593});
        expecteds.put(ErrorModuleEnd2End.Mode.RO_SEG, new double[]{0.24802725968436154, 0.24254928931682715, 0.18841616017161245, 0.29124629976439315});
        expecteds.put(ErrorModuleEnd2End.Mode.NO_RO_SEG, new double[]{0.16666666666666666, 0.19785932721712537, 0.18812589413447783, 0.17925728478827163});
        expectedsSegmentation.put(ErrorModuleEnd2End.Mode.RO, new double[]{0.2779770444763271, 0.25981965459269446, 0.19306399713979264, 0.3473086449586178});
        expectedsSegmentation.put(ErrorModuleEnd2End.Mode.NO_RO, new double[]{0.17539454806312768, 0.20693871312853432, 0.1927064712191634, 0.22239004349927502});
        expectedsSegmentation.put(ErrorModuleEnd2End.Mode.RO_SEG, new double[]{0.2749282639885222, 0.25767996331957815, 0.18841616017161245, 0.3361324231257174});
        expectedsSegmentation.put(ErrorModuleEnd2End.Mode.NO_RO_SEG, new double[]{0.16630785791173305, 0.19844060541201652, 0.18812589413447783, 0.18083948227894037});
    }

    @Test
    public void testAllPages() throws IOException {
        for (boolean usePolygons : new boolean[]{true, false}) {
            for (ErrorModuleEnd2End.Mode mode : ErrorModuleEnd2End.Mode.values()) {
                StringBuilder sb = new StringBuilder();
                double[] doubles = (usePolygons ? expectedsSegmentation : expecteds).get(mode);
                sb.append(usePolygons ? "expectedsSegmentation" : "expecteds").append(".put(ErrorModuleEnd2End.Mode.").append(mode.name()).append(", new double[]{");
                for (int i = 0; i < doubles.length; i++) {
                    double expected = doubles[i];
//                if (expected != 0.0) {
//                    continue;
//                }
                    double cer = testGermania(mode, i, usePolygons);
                    sb.append(cer);
                    if (i < doubles.length - 1) {
                        sb.append(",");
                    }
                    Assert.assertEquals("CER of page " + i + " and mode " + mode + " is wrong", expected, cer, 0.00001);
                }
                sb.append("});");
                System.out.println(sb);
            }
        }
    }

    //    @Test
    public void testSingle() throws IOException {
        ErrorModuleEnd2End.Mode mode = ErrorModuleEnd2End.Mode.RO;
        int i = 0;
        boolean usePolygon = true;
        double[] doubles = expecteds.get(mode);
        double expected = doubles[i];
//                if (expected != 0.0) {
//                    continue;
//                }
        double cer = testGermania(mode, i, usePolygon);
        Assert.assertEquals("CER of page " + i + " and mode " + mode + " is wrong", expected, cer, 0.00001);
    }

    public void testGermania0_RO() throws IOException {
        double cer = testGermania(ErrorModuleEnd2End.Mode.RO, 0, false);
        Assert.assertEquals("CER differs from previous", expecteds.get(ErrorModuleEnd2End.Mode.RO)[0], cer, 0.00001);
        //0m 04s 195mw -> no debug output
        //0m 18s 022ms -> introduce static arrays in CCAbstract
        //0m 21s 680ms -> with String[] as reco and ref
        //2m 53s 392ms -> with List<String> as reco and ref
    }

    public void testGermania0_RO_SEG() throws IOException {
        double cer = testGermania(ErrorModuleEnd2End.Mode.RO_SEG, 0, false);
        Assert.assertEquals("CER differs from previous", expecteds.get(ErrorModuleEnd2End.Mode.RO_SEG)[0], cer, 0.00001);
        //0m 05s 688ms -> add pathFilter for jumpReco
        //0m 24s 564ms -> introduce static arrays in CCAbstract
        //0m 30s 910ms -> with String[] as reco and ref
        //6m 59s 357ms -> with List<String> as reco and ref
    }

//    public double testGermania(ErrorModuleEnd2End.Mode mode, int image) throws IOException {
//        return testGermania(mode, image, false);
//    }

    public double testGermania(ErrorModuleEnd2End.Mode mode, int image, boolean usePolygons) throws IOException {
        StopWatch sw = new StopWatch();
        ErrorModuleEnd2End end2End = new ErrorModuleEnd2End(new CategorizerCharacterDft(), null, mode, usePolygons, false);
//        end2End.setSizeProcessViewer(6000);
//        end2End.setFileDynProg(new File(mode + ".png"));
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
        System.out.println("test with mode " + mode + " for " + cnt + " characters");
        end2End.calculateWithSegmentation(hypLines, gtLines);
        ObjectCounter<Count> counter = end2End.getCounter();
        System.out.println(((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT));
        System.out.println(counter);
        sw.stop();
        System.out.println("Stopwatch for mode " + mode + " and image " + image + " = " + sw.toString());
        return ((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT);
//        }
    }
}
