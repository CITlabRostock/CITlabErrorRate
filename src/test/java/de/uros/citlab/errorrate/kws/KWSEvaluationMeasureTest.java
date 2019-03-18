/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.kws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.panayotis.gnuplot.JavaPlot;
import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.aligner.BaseLineAligner;
import de.uros.citlab.errorrate.kws.measures.IRankingStatistic;
import de.uros.citlab.errorrate.types.KWS;
import de.uros.citlab.errorrate.types.KWS.Line;
import de.uros.citlab.errorrate.types.KWS.Page;
import de.uros.citlab.errorrate.types.KWS.Result;
import de.uros.citlab.errorrate.types.KWS.Word;
import de.uros.citlab.errorrate.util.PlotUtil;
import java.awt.Polygon;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gundram
 */
public class KWSEvaluationMeasureTest {

    public KWSEvaluationMeasureTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        listGT = setUpFolder(folderGT);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getMeanMearsure method, of class KWSEvaluationMeasure.
     */
    @Test
    public void testGetMeanMearsure() {
        System.out.println("getMeanMearsure");

//        test(measure, 1.0, 0.0, 0.0);
    }

    /**
     * Test of getGlobalMearsure method, of class KWSEvaluationMeasure.
     */
//    @Test
    public void testGetGlobalMearsure() {
        System.out.println("getGlobalMearsure");
        KWSEvaluationMeasure measure = new KWSEvaluationMeasure(new BaseLineAligner());

        test(measure, 1.0, 0.0);
        test(measure, 0.8, 0.0);
        test(measure, 0.5, 0.0);
        test(measure, 0.0, 0.0);

        test(measure, 1.0, 0.2);
        test(measure, 1.0, 1.0);
    }

    private void test(KWSEvaluationMeasure measure, double corrRatio, double fnRatio) {
        HashMap<String, KWS.Word> words = new HashMap<>();
        List<KWS.Page> pages = new LinkedList<>();
        int numOfPages = 1;
        int numOfQuerries = 10;
        int totalFn = 0;
        int totalcorr = 0;
        Random rnd = new Random(1234);

        LinkedList<String> keys = new LinkedList<String>();

        for (int querryId = 0; querryId < numOfQuerries; querryId++) {
            String querryWord = "word" + querryId;
            KWS.Word word = new KWS.Word(querryWord);
            words.put(word.getKeyWord(), word);
        }

        LinkedList<Map.Entry<String, KWS.Word>> keyAndWord = new LinkedList<>(words.entrySet());

        for (int pageId = 0; pageId < numOfPages; pageId++) {
            LinkedList<Line> lines = new LinkedList<>();
            int numOfLines = 10;
            for (int lineId = 0; lineId < numOfLines; lineId++) {
                int numOfMatches = 5;// muss kleiner sein als numOfQuerries
                int numOfcorr = (int) Math.ceil(corrRatio * numOfMatches);
                int numOfFp = numOfMatches - numOfcorr;
                int numOfFn = (int) Math.ceil(numOfMatches * fnRatio);
//                totalFp += numOfFp;
                totalcorr += numOfcorr;
                totalFn += numOfFn;
                Polygon p = new Polygon(new int[]{lineId * 50, lineId * 50}, new int[]{0, 100}, 2);
                Line line = new Line(p);

                int cnt = -1;
                Collections.shuffle(keyAndWord, rnd);
                for (Map.Entry<String, KWS.Word> entry : keyAndWord) {
                    if (cnt < 0) {
                        // false negatives 
                        for (int i = 0; i < numOfFn; i++) {
                            addGtWord(line, entry.getKey(), p, "page" + pageId);
                        }
                    } else {
                        // false positives and corrects
                        addMatch(entry.getValue(), cnt < numOfFp ? 0.0 : 1.0, p, "page" + pageId);
                        if (cnt >= numOfFp) {
                            addGtWord(line, entry.getKey(), p, "page" + pageId);
                        }
                    }
                    cnt++;
                    if (cnt >= numOfMatches) {
                        break;
                    }
                }

                lines.add(line);
            }
            pages.add(new Page("page" + pageId, lines));
        }

        Result res = new Result(new HashSet<>(words.values()));
        KWS.GroundTruth gt = new KWS.GroundTruth(pages);

        measure.setGroundtruth(gt);
        measure.setResults(res);
        LinkedList<IRankingMeasure.Measure> ms = new LinkedList<>();
        ms.add(IRankingMeasure.Measure.GAP);

        double globalMearsure = measure.getMeasure(ms).get(IRankingMeasure.Measure.GAP);
        assertEquals(corrRatio == 0.0 ? 1.0 : (double) totalcorr / (totalcorr + totalFn), globalMearsure, 1e-5);
        System.out.println("measure: " + globalMearsure);
    }

    private void addMatch(Word word, double conf, Polygon p, String pageId) {
        word.add(new KWS.Entry(conf, "", p, pageId));
    }

    private void addGtWord(Line line, String keyword, Polygon p, String pageId) {
        line.addKeyword(keyword, p);
    }
    private static final File folderGT = new File("src/test/resources/gt");
    private static File[] listGT;

    private static File[] setUpFolder(File folder) {
        assertTrue("cannot find resources in " + folder.getPath(), folder.exists());
        File[] res = FileUtils.listFiles(folder, "xml".split(" "), true).toArray(new File[0]);
        Arrays.sort(res);
        return res;
    }

    private String[] getStringList(File[] files) {
        String[] res = new String[files.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = files[i].getPath();
        }
        return res;
    }

    private static Result getResult(File path) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            return gson.fromJson(new FileReader(path), Result.class);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Result filter(Result result, List<String> kw) {
        Set<Word> words = new LinkedHashSet<>();
        for (Word keyword : result.getKeywords()) {
            if (kw.contains(keyword.getKeyWord())) {
                words.add(keyword);
            }
        }
        return new Result(words);
    }

    private KWS.MatchList merge(List<KWS.MatchList> mls) {
        List<KWS.Match> matches = new LinkedList<>();
        for (KWS.MatchList ml : mls) {
            matches.addAll(ml.matches);
        }
        KWS.MatchList res = new KWS.MatchList(matches);
        res.sort();
        return res;
    }

    @Test
    public void testMatchList() throws IOException {
        System.out.println("testMatchList");
        List<String> readLines = FileUtils.readLines(new File("src/test/resources/kw.txt"), Charset.defaultCharset());
//        List<String> readLines = Arrays.asList("seyn");
        KeywordExtractor kwe = new KeywordExtractor();
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(getStringList(listGT), null, readLines);
        KWSEvaluationMeasure kem = new KWSEvaluationMeasure(new BaseLineAligner());
        kem.setGroundtruth(keywordGroundTruth);
        LinkedList<KWS.MatchList> mls = new LinkedList<>();
        for (int i : new int[]{50, 20, 10, 5}) {
            Result res = getResult(new File(String.format("src/test/resources/kws_htr/out_%02d.json", i)));
            res = filter(res, readLines);
            kem.setResults(res);
            mls.add(merge(kem.getMatchList()));
        }
        for (int i = 1; i < mls.size(); i++) {
            if (mls.get(i - 1).matches.size() > mls.get(i).matches.size()) {
                fail("larger threshold have to make list smaller");
            }
        }
        boolean run = true;
        int idxList = 0;
        int sizeList = mls.size();
//        int sizeMatch = mls.get(mls.size() - 2).matches.size();
        for (int idxList2 = 1; idxList2 < sizeList; idxList2++) {
            KWS.MatchList listSmall = mls.get(idxList2 - 1);
            KWS.MatchList listLarge = mls.get(idxList2);
            for (int idxMatch = 0; idxMatch < listSmall.matches.size(); idxMatch++) {
                KWS.Match matchSmallList = listSmall.matches.get(idxMatch);
                KWS.Match matchLargeList = listLarge.matches.get(idxMatch);
                if (matchSmallList.conf != matchLargeList.conf && matchSmallList.type != KWS.Type.FALSE_NEGATIVE) {
                    assertTrue("confidences differ on idxMatch " + idxMatch + " and between indexes " + (idxList2 - 1) + " and " + idxList2 + ".", matchSmallList.conf == matchLargeList.conf
                    );
                }
            }
        }

    }

    @Test
    public void testRealScenario() throws IOException {
        System.out.println("testRealScenario");
        List<String> readLines = FileUtils.readLines(new File("src/test/resources/kw.txt"),Charset.defaultCharset());
//        List<String> readLines = Arrays.asList("seyn");
        KeywordExtractor kwe = new KeywordExtractor();
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(getStringList(listGT), null, readLines);
        KWSEvaluationMeasure kem = new KWSEvaluationMeasure(new BaseLineAligner());
        kem.setGroundtruth(keywordGroundTruth);
        IRankingMeasure.Measure[] ms = new IRankingMeasure.Measure[]{
            IRankingMeasure.Measure.GAP, IRankingMeasure.Measure.MAP,
            IRankingMeasure.Measure.R_PRECISION, IRankingMeasure.Measure.PRECISION,
            IRankingMeasure.Measure.RECALL, IRankingMeasure.Measure.PRECISION_AT_10, IRankingMeasure.Measure.WMAP};
        for (IRankingMeasure.Measure m : ms) {
            for (int i : new int[]{5, 10, 20, 50}) {
                Result res = getResult(new File(String.format("src/test/resources/kws_htr/out_%02d.json", i)));
                res = filter(res, readLines);
                kem.setResults(res);
                Map<IRankingMeasure.Measure, Double> measure = kem.getMeasure(m);
                System.out.println("#### i = " + i + " ####");
                for (IRankingMeasure.Measure measure1 : measure.keySet()) {
                    System.out.println(measure1.toString() + " = " + measure.get(measure1));
                }
            }
        }

    }

    private static double[] append1(double[] vec) {
        double[] res = new double[vec.length + 1];
        res[0] = 1.0;
        System.arraycopy(vec, 0, res, 1, vec.length);
        return res;
    }

    @Test
    public void testStatistic() throws IOException {
        System.out.println("testStatistic");
        List<String> readLines = FileUtils.readLines(new File("src/test/resources/kw.txt"),Charset.defaultCharset());
//        List<String> readLines = Arrays.asList("sein");
        KeywordExtractor kwe = new KeywordExtractor();
        KWS.GroundTruth keywordGroundTruth = kwe.getKeywordGroundTruth(getStringList(listGT), null, readLines);
        KWSEvaluationMeasure kem = new KWSEvaluationMeasure(new BaseLineAligner());
        kem.setGroundtruth(keywordGroundTruth);
        List<double[]> data = new LinkedList<>();
        String[] names = new String[8];
        int idx = 0;
        int maxAnz = 0;
        for (int i : new int[]{5, 50}) {
            int cnt = 0;
            File filename = new File(String.format("src/test/resources/kws_htr/out_%02d.json", i));
            Result res = getResult(filename);
            res = filter(res, readLines);
            kem.setResults(res);
            List<IRankingStatistic.Statistic> asList = Arrays.asList(IRankingStatistic.Statistic.W_PR_CURVE, IRankingStatistic.Statistic.PR_CURVE);
            Map<IRankingStatistic.Statistic, double[]> stats = kem.getStats(asList);
            System.out.println("#### i = " + i + " ####");
            for (IRankingStatistic.Statistic measure1 : stats.keySet()) {
                System.out.println(measure1.toString() + "========================= " + Arrays.toString(stats.get(measure1)));
                data.add(stats.get(measure1));
                maxAnz = Math.max(stats.get(measure1).length, maxAnz);
                names[idx++] = (measure1.toString() + "_" + filename.getName()).replace("_", "\\_");
            }
        }
//        Consumer<JavaPlot> defaultTerminal = PlotUtil.getDefaultTerminal();
//        Consumer<JavaPlot> imgTerminal = PlotUtil.getImageFileTerminal(new File("/home/gundram/test.png"), 2000, 1000);
//        defaultTerminal.accept(PlotUtil.getPRCurves(data, Arrays.asList(names)));
    }

}
