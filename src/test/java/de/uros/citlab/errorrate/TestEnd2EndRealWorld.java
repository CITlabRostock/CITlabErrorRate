/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.Count;
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
        F1_ATR2("ATR_2/page_f1"),
        F2_ATR1("ATR_1/page_f2"),
        F2_ATR2("ATR_2/page_f2"),
        F3_ATR1("ATR_1/page_f3"),
        F3_ATR2("ATR_2/page_f3"),
        GT("ATR_1/page_f1");
        private String path;

        Result(String path) {
            this.path = path;
        }

        File getPath() {
            return new File("src/test/resources/end2end/" + path);
        }
    }

    private static HashMap<ErrorModuleEnd2End.Mode, double[]> expecteds = new HashMap<>();

    static {
        expecteds.put(ErrorModuleEnd2End.Mode.RO, new double[]{0.26434720229555236, 0.24629374904478069, 0.20664998212370397, 0.31553192774723615});
        expecteds.put(ErrorModuleEnd2End.Mode.NO_RO, new double[]{0.18256814921090386, 0.20426409903713894, 0.20629245620307474, 0.21767762203963267});
        expecteds.put(ErrorModuleEnd2End.Mode.RO_SEG, new double[]{0.25125538020086086, 0.24254928931682715, 0.19806936002860207, 0.29450854829940193});
        expecteds.put(ErrorModuleEnd2End.Mode.NO_RO_SEG, new double[]{0.1735151623900951, 0.19782907812261122, 0.18998211091234346, 0.17737243970427827});
    }


    public Map<Count, Long> getCount(boolean upper, boolean word, ErrorModuleEnd2End.Mode mode, boolean letterNumber, String gt, String hyp) {
        System.out.println((" test \"" + gt + "\" vs \"" + hyp + "\"").replace("\n", "\\n"));
        ITokenizer tokenizer = new TokenizerCategorizer(word ? new CategorizerWordMergeGroups() : new CategorizerCharacterDft());
        IStringNormalizer sn = new StringNormalizerDft(Normalizer.Form.NFKC, upper);
        if (letterNumber) {
            sn = new StringNormalizerLetterNumber(sn);
        }
        IErrorModule impl = new ErrorModuleEnd2End(tokenizer, sn, mode, false);
        //TODO: better place to add "\n" to strings?
        if (!hyp.startsWith("\n")) {
            hyp = "\n" + hyp;
        }
        if (!hyp.endsWith("\n")) {
            hyp += "\n";
        }
        if (!gt.startsWith("\n")) {
            gt = "\n" + gt;
        }
        if (!gt.endsWith("\n")) {
            gt += "\n";
        }
        impl.calculate(hyp, gt);
        return impl.getCounter().getMap();
    }

    static String concat(List<Pair<String, Polygon>> lines) {
        StringBuilder sb = new StringBuilder();
        for (Pair<String, Polygon> line : lines) {
            if (!line.getFirst().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line.getFirst());
            }
        }
        return sb.toString();
    }

    public static Polygon getPolygon(org.primaresearch.maths.geometry.Polygon baseline) {
        Polygon p = new Polygon();
        for (int i = 0; i < baseline.getSize(); i++) {
            p.addPoint(baseline.getPoint(i).x, baseline.getPoint(i).y);
        }
        return p;
    }

    public static List<Pair<String, Polygon>> getTranscriptsAndPolyFromLines(String fileName) {
        if (fileName.endsWith(".xml")) {
            Page aPage;
            try {
                List<Pair<String, Polygon>> res = new ArrayList<>();
//                aPage = reader.read(new FileInput(new File(fileName)));
                aPage = org.primaresearch.dla.page.io.xml.PageXmlInputOutput.readPage(fileName);
                if (aPage == null) {
                    System.out.println("Error while parsing xml-File.");
                    return null;
                }
                List<Region> regionsSorted = aPage.getLayout().getRegionsSorted();
                for (Region reg : regionsSorted)
                    if (reg instanceof TextRegion) {
                        TextRegion textregion = (TextRegion) reg;
                        List<LowLevelTextObject> textObjectsSorted = textregion.getTextObjectsSorted();
                        for (LowLevelTextObject line : textObjectsSorted) {
                            if (line instanceof TextLine) {
                                String text = line.getText();
                                Polygon polygon = getPolygon(((TextLine) line).getBaseline());
                                if (text == null || polygon.npoints < 1) {
                                    System.out.println("transciption is '" + text + "' and polygon is '" + polygon + "'");
                                } else {
                                    res.add(new Pair<>(text, polygon));
                                }
                            }
                        }
                    }
                return res;
            } catch (UnsupportedFormatVersionException ex) {
                throw new RuntimeException("Error while parsing xml-file.", ex);
            }
        }
        return null;
    }

    @Test
    public void testAllPages() throws IOException {
        for (ErrorModuleEnd2End.Mode mode : ErrorModuleEnd2End.Mode.values()) {
            double[] doubles = expecteds.get(mode);
            for (int i = 0; i < doubles.length; i++) {
                double expected = doubles[i];
//                if (expected != 0.0) {
//                    continue;
//                }
                double cer = testGermania(mode, i);
                Assert.assertEquals("CER of page " + i + " and mode " + mode + " is wrong", expected, cer, 0.00001);
            }
        }
    }

    @Test
    public void testSingle() throws IOException {
        ErrorModuleEnd2End.Mode mode = ErrorModuleEnd2End.Mode.NO_RO_SEG;
        int i = 2;
        double[] doubles = expecteds.get(mode);
        double expected = doubles[i];
//                if (expected != 0.0) {
//                    continue;
//                }
        double cer = testGermania(mode, i);
        Assert.assertEquals("CER of page " + i + " and mode " + mode + " is wrong", expected, cer, 0.00001);
    }


    public void testGermania0_RO() throws IOException {
        double cer = testGermania(ErrorModuleEnd2End.Mode.RO, 0);
        Assert.assertEquals("CER differs from previous", 0.26434720229555236, cer, 0.00001);
        //0m 04s 195mw -> no debug output
        //0m 18s 022ms -> introduce static arrays in CCAbstract
        //0m 21s 680ms -> with String[] as reco and ref
        //2m 53s 392ms -> with List<String> as reco and ref
    }

    public void testGermania0_RO_SEG() throws IOException {
        double cer = testGermania(ErrorModuleEnd2End.Mode.RO_SEG, 0);
        Assert.assertEquals("CER differs from previous", 0.25125538020086086, cer, 0.00001);
        //0m 24s 564ms -> introduce static arrays in CCAbstract
        //0m 30s 910ms -> with String[] as reco and ref
        //6m 59s 357ms -> with List<String> as reco and ref
    }

    public double testGermania(ErrorModuleEnd2End.Mode mode, int image) throws IOException {
        ErrorModuleEnd2End end2End = new ErrorModuleEnd2End(new CategorizerCharacterDft(), null, mode, false);
//        end2End.setSizeProcessViewer(6000);
//        end2End.setFileDynProg(new File(mode+".png"));
        Result gtResult = Result.F1_ATR1;
        Result hypResult = Result.F3_ATR2;
        File[] gts = new File(gtResult.getPath().getPath()).listFiles();
        File[] hyps = new File(hypResult.getPath().getPath()).listFiles();
        Arrays.sort(gts);
        Arrays.sort(hyps);
//        for (int i = 0; i < 1; i++) {
        File hyp = hyps[image];
        File gt = gts[image];
        List<Pair<String, Polygon>> hypLines = getTranscriptsAndPolyFromLines(hyp.getPath());
        List<Pair<String, Polygon>> gtLines = getTranscriptsAndPolyFromLines(gt.getPath());
        int cnt = 0;
        for (int i = 0; i < gtLines.size(); i++) {
            cnt += gtLines.get(i).getFirst().length();
        }
        System.out.println("test with mode "+mode+ " for "+cnt +" characters");
        end2End.calculate(concat(hypLines), concat(gtLines));
        ObjectCounter<Count> counter = end2End.getCounter();
        System.out.println(((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT));
        System.out.println(counter);
        return ((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT);
//        }
    }

    @Test
    public void testGermania1() throws IOException {
        ErrorModuleEnd2End end2End = new ErrorModuleEnd2End(new CategorizerCharacterDft(), null, ErrorModuleEnd2End.Mode.RO, false);
        Result gtResult = Result.F3_ATR1;
        Result hypResult = Result.F1_ATR2;

        File[] gts = new File(gtResult.getPath().getPath()).listFiles();
        File[] hyps = new File(hypResult.getPath().getPath()).listFiles();
        Arrays.sort(gts);
        Arrays.sort(hyps);
        for (int i = 1; i < 2; i++) {
            File hyp = hyps[i];
            File gt = gts[i];
            List<Pair<String, Polygon>> hypLines = getTranscriptsAndPolyFromLines(hyp.getPath());
            List<Pair<String, Polygon>> gtLines = getTranscriptsAndPolyFromLines(gt.getPath());
            end2End.calculate(concat(hypLines), concat(gtLines));
            ObjectCounter<Count> counter = end2End.getCounter();
            System.out.println(((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT));
            System.out.println(counter);

        }
    }

    @Test
    public void testGermania2() throws IOException {
        ErrorModuleEnd2End end2End = new ErrorModuleEnd2End(new CategorizerCharacterDft(), null, ErrorModuleEnd2End.Mode.RO, false);
        Result gtResult = Result.GT;
        Result hypResult = Result.F1_ATR2;

        File[] gts = new File(gtResult.getPath().getPath()).listFiles();
        File[] hyps = new File(hypResult.getPath().getPath()).listFiles();
        Arrays.sort(gts);
        Arrays.sort(hyps);
        for (int i = 2; i < 3; i++) {
            File hyp = hyps[i];
            File gt = gts[i];
            List<Pair<String, Polygon>> hypLines = getTranscriptsAndPolyFromLines(hyp.getPath());
            List<Pair<String, Polygon>> gtLines = getTranscriptsAndPolyFromLines(gt.getPath());
            end2End.calculate(concat(hypLines), concat(gtLines));
            ObjectCounter<Count> counter = end2End.getCounter();
            System.out.println(((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT));
            System.out.println(counter);

        }
    }

    @Test
    public void testGermania3() throws IOException {
        ErrorModuleEnd2End end2End = new ErrorModuleEnd2End(new CategorizerCharacterDft(), null, ErrorModuleEnd2End.Mode.RO, false);
        Result gtResult = Result.F1_ATR1;
        Result hypResult = Result.F3_ATR2;

        File[] gts = new File(gtResult.getPath().getPath()).listFiles();
        File[] hyps = new File(hypResult.getPath().getPath()).listFiles();
        Arrays.sort(gts);
        Arrays.sort(hyps);

        for (int i = 3; i < 4; i++) {
            File hyp = hyps[i];
            File gt = gts[i];
            List<Pair<String, Polygon>> hypLines = getTranscriptsAndPolyFromLines(hyp.getPath());
            List<Pair<String, Polygon>> gtLines = getTranscriptsAndPolyFromLines(gt.getPath());
            end2End.calculate(concat(hypLines), concat(gtLines));
            ObjectCounter<Count> counter = end2End.getCounter();
            System.out.println(((double) counter.get(Count.ERR)) / (double) counter.get(Count.GT));
            System.out.println(counter);

        }
    }
}
