/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.text2image;

import de.uros.citlab.errorrate.Text2ImageError;
import de.uros.citlab.errorrate.aligner.BaseLineAligner;
import de.uros.citlab.errorrate.htr.ErrorRateCalcer;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import de.uros.citlab.errorrate.types.Result;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author gundram
 */
public class Text2ImageErrorCalcerTest {

    private static final File folderGT = new File("src/test/resources/gt");
    private static final File folderBot = new File("src/test/resources/hyp_bot");
    private static final File folderErr = new File("src/test/resources/hyp_err");

    private static File[] listGT;
    private static File[] listBot;
    private static File[] listErr;

    public Text2ImageErrorCalcerTest() {
    }

    @BeforeClass
    public static void setUp() {
        listGT = setUpFolder(folderGT);
        listErr = setUpFolder(folderErr);
        listBot = setUpFolder(folderBot);
    }

    private static File[] setUpFolder(File folder) {
        assertTrue("cannot find resources in " + folder.getPath(), folder.exists());
        List<File> files = new ArrayList<>(FileUtils.listFiles(folder, "xml".split(" "), true));
        files.removeIf(file -> file.getName().equals("doc.xml"));
        File[] res = files.toArray(new File[0]);
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
    public void testHTR() {
        System.out.println("testHTR");
        Text2ImageError instance = new Text2ImageError();
        Map<String, Double> run = instance.run("-p -t 0.9".split(" "), listGT, listErr);
        List<String> keys = new LinkedList<>(run.keySet());
        Collections.sort(keys);
        for (String string : keys) {
            System.out.println(String.format("%10s: %.4f", string, run.get(string)));
        }
    }
    @Test
    public void testLAHTR() {
        System.out.println("testLAHTR");
        Text2ImageError instance = new Text2ImageError();
        Map<String, Double> run = instance.run("-p -t 0.9".split(" "), listGT, listBot);
        List<String> keys = new LinkedList<>(run.keySet());
        Collections.sort(keys);
        for (String string : keys) {
            System.out.println(String.format("%10s: %.4f", string, run.get(string)));
        }
    }

    @Test
    public void testCouverage(){
        Polygon gt = new Polygon(new int[]{0,10,20,30},new int[]{3,3,3,3},4);
        Polygon hyp = new Polygon(new int[]{0,10,20,30},new int[]{7,7,7,7},4);
        double couverage = BaseLineAligner.couverage(gt, hyp, 5);
        System.out.println(couverage);
    }

}
