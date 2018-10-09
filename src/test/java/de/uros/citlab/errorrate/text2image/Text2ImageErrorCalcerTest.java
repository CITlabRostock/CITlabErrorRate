/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.text2image;

import de.uros.citlab.errorrate.Text2ImageError;
import de.uros.citlab.errorrate.htr.ErrorRateCalcer;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

}
