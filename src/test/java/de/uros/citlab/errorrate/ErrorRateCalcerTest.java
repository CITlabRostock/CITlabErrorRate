/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.ErrorRateCalcer;
import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author gundram
 */
public class ErrorRateCalcerTest {

    private static final File folderGT = new File("src/test/resources/gt");
    private static final File folderBot = new File("src/test/resources/hyp_bot");
    private static final File folderErr = new File("src/test/resources/hyp_err");

    private static File[] listGT;
    private static File[] listBot;
    private static File[] listErr;

    public ErrorRateCalcerTest() {
    }

    @BeforeClass
    public static void setUp() {
        listGT = setUpFolder(folderGT);
        listErr = setUpFolder(folderErr);
        listBot = setUpFolder(folderBot);
    }

    private static File[] setUpFolder(File folder) {
        assertTrue("cannot find resources in " + folder.getPath(), folder.exists());
        File[] res = FileUtils.listFiles(folder, "xml".split(" "), true).toArray(new File[0]);
        Arrays.sort(res);
        return res;
    }

    @AfterClass
    public static void tearDown() {
    }

    private void printResult(Result result) {
        System.out.println(result.getMethod() + ":" + result.getCounts());
        System.out.println(result.getMethod() + ":" + result.getMetrics());
        System.out.println("");
    }

    @Test
    public void testLA_HTR() {
        System.out.println("testLA_HTR");
        ErrorRateCalcer instance = new ErrorRateCalcer();
        Map<Method, Result> results = instance.process(listBot, listGT, Method.values());
        for (Result result : results.values()) {
            printResult(result);
        }
    }

    @Test
    public void testHTR() {
        System.out.println("testHTR");
        ErrorRateCalcer instance = new ErrorRateCalcer();
        Map<Method, Result> results = instance.process(listErr, listGT, Method.values());
        for (Result result : results.values()) {
            printResult(result);
        }
    }

    private File[] getSubList(File[] list,int idx){
        if(idx<0){
            return list;
        }
        return new File[]{list[idx]};
    }

    @Test
    public void testCompareErrorRateAndEnd2EndRO() {
        System.out.println("testHTR");
        File[] hypSmall = getSubList(listErr, 0);
        File[] gTSmall = getSubList(listGT, 0);
        ErrorRateCalcer instance = new ErrorRateCalcer();
        instance.setUseEnd2End(false);
        Result stdMethod = instance.process(hypSmall, gTSmall, Method.CER);
        instance.setUseEnd2End(true);
        Result end2EndMethod = instance.process(hypSmall, gTSmall, Method.CER);
        System.out.println("Std:");
        printResult(stdMethod);
        System.out.println("End2End:");
        printResult(end2EndMethod);
        Map<Metric, Double> metrics = stdMethod.getMetrics();
        for (Metric m : metrics.keySet()) {
        double cntStd = stdMethod.getMetric(m);
        double cntEnd2End = end2EndMethod.getMetric(m);
            Assert.assertEquals("different results for metric '"+m+"' for same tasks - probably end2end is wrong.",cntStd,cntEnd2End,0.00001);
        }
    }

    @Test
    public void testTranskribusUsage() {
        System.out.println("testTranskribusUsage");
        ErrorRateCalcer instance = new ErrorRateCalcer();
        Result resultWer = instance.process(listErr, listGT, Method.WER);
        System.out.println("WORD ERROR RATE");
        System.out.println(String.format("Number of Words =%5d in Ground Truth", resultWer.getCount(Count.GT)));
        System.out.println(String.format("Number of Words =%5d in Hypothesis", resultWer.getCount(Count.HYP)));
        System.out.println(String.format("WER = %5.2f %% (=%5d) (all word errors)", resultWer.getMetric(Metric.ERR) * 100, resultWer.getCount(Count.ERR)));
        System.out.println("... which can be splitted into categories...");
        System.out.println(String.format("SUB = %5.2f %% (=%5d) (wrong words)", resultWer.getCount(Count.SUB) * 100.0 / resultWer.getCount(Count.GT), resultWer.getCount(Count.SUB)));
        System.out.println(String.format("INS = %5.2f %% (=%5d) (missed words / under segmentation)", resultWer.getCount(Count.INS) * 100.0 / resultWer.getCount(Count.GT), resultWer.getCount(Count.INS)));
        System.out.println(String.format("DEL = %5.2f %% (=%5d) (too many words / over segemention)", resultWer.getCount(Count.DEL) * 100.0 / resultWer.getCount(Count.GT), resultWer.getCount(Count.DEL)));
        System.out.println("");
        System.out.println("CHARACTER ERROR RATE");
        Result resultCer = instance.process(listErr, listGT, Method.CER);
        System.out.println(String.format("Number of Characters =%5d in Ground Truth", resultCer.getCount(Count.GT)));
        System.out.println(String.format("Number of Characters =%5d in Hypothesis", resultCer.getCount(Count.HYP)));
        System.out.println(String.format("CER = %5.2f %% (=%5d) (all character errors)", resultCer.getMetric(Metric.ERR) * 100, resultCer.getCount(Count.ERR)));
        System.out.println("... which can be splitted into categories...");
        System.out.println(String.format("SUB = %5.2f %% (=%5d) (wrong characters)", resultCer.getCount(Count.SUB) * 100.0 / resultCer.getCount(Count.GT), resultCer.getCount(Count.SUB)));
        System.out.println(String.format("INS = %5.2f %% (=%5d) (missed characters)", resultCer.getCount(Count.INS) * 100.0 / resultCer.getCount(Count.GT), resultCer.getCount(Count.INS)));
        System.out.println(String.format("DEL = %5.2f %% (=%5d) (too many characters)", resultCer.getCount(Count.DEL) * 100.0 / resultCer.getCount(Count.GT), resultCer.getCount(Count.DEL)));
    }

    @Test
    public void testBestCase() {
        System.out.println("testBestCase");
        ErrorRateCalcer instance = new ErrorRateCalcer();
        Map<Method, Result> results = instance.process(listGT, listGT, Method.values());
        for (Result value : results.values()) {
            Map<Metric, Double> metrics = value.getMetrics();
            for (Metric metric : metrics.keySet()) {
                double val = metrics.get(metric);
                switch (metric) {
                    case ACC:
                    case F:
                    case PREC:
                    case REC:
                        assertEquals("wrong value of metric " + metric + " for a perfect system.", 1.0, val, 0.0);
                        break;
                    default:
                        assertEquals("wrong value of metric " + metric + " for a perfect system.", 0.0, val, 0.0);
                }
            }
        }
    }

    @Test
    public void testPagewise() {
        System.out.println("testPagewise");
        ErrorRateCalcer instance = new ErrorRateCalcer();
        Map<Method, ErrorRateCalcer.ResultPagewise> results = instance.processPagewise(listErr, listGT, Method.values());
        for (ErrorRateCalcer.ResultPagewise result : results.values()) {
            ObjectCounter<Count> counts = result.getCounts();
            ObjectCounter<Count> countsPagewise = new ObjectCounter<>();
            for (Result resultPagewise : result.getPageResults()) {
                countsPagewise.addAll(resultPagewise.getCounts());
            }
            for (Count count : counts.getResult()) {
                assertEquals("sum of pagecounts have to be same as overall result", counts.get(count), countsPagewise.get(count));
            }
        }
    }

}
