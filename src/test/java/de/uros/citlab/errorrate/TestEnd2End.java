/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.junit.Assert;
import org.junit.Test;

import java.text.Normalizer;
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
public class TestEnd2End {


    @Test
    public void testLineBreak() {
        Assert.assertEquals(new Long(4), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "\n this is \n with\n linebreaks\n ", "this is with linebreaks").get(Count.COR));
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(new Long(1), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "this is text ", "is this text").get(Count.COR));
//        Assert.assertEquals(new Long(3), getCount(false, true, true, false, "this is text ", "is this text").get(Count.TP));
    }

    @Test
    public void testComposition() {
        Assert.assertEquals(new Long(1), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "sa\u0308ße", "säße").get(Count.COR));
        Assert.assertEquals(new Long(1), getCount(true, true, ErrorModuleEnd2End.Mode.RO, false, "SA\u0308SSE", "säße").get(Count.COR));
    }

    @Test
    public void testTokenizer() {
        Assert.assertEquals(new Long(1), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "it's wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(2), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "its wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(3), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "its, wrong", "its, wrong").get(Count.COR));
//        Assert.assertEquals(2, get(false, true, false, true, "COR", "it's wrong", "its wrong"));
    }

    @Test
    public void testErrorType() {
        Assert.assertEquals(new Long(1), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "its, wrong", "its wrong").get(Count.INS));
        Assert.assertEquals(new Long(1), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "its, wrong", "its. wrong").get(Count.SUB));//substitution
        Assert.assertEquals(new Long(2), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "its, wrong", "its. wrong").get(Count.COR));//correct
//        Assert.assertEquals(new Long(2), getCount(false, true, true, false, "its, wrong", "its. wrong").get(Count.TP));//true positive
//        Assert.assertEquals(new Long(1), getCount(false, true, true, false, "its, wrong", "its. wrong").get(Count.FN));//false negative
//        Assert.assertEquals(new Long(1), getCount(false, true, true, false, "its, wrong", "its. wrong").get(Count.FP));//false positive
        Assert.assertEquals(new Long(2), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "wrong", "its, wrong").get(Count.DEL));
//        Assert.assertEquals(new Long(2), getCount(false, true, true, false, "wrong", "its, wrong").get(Count.FP));
//        Assert.assertEquals(2, get(false, true, false, true, "COR", "it's wrong", "its wrong"));
    }

    public void testCases(ErrorModuleEnd2End.Mode mode, int bestCase,
                          int swapLines,
                          int deleteLine,
                          int splitLine,
                          int mergeLine,
                          int addStart,
                          int deleteStart,
                          int deleteEnd,
                          int addEnd,
                          int reverseLines
    ) {
        //best case
        Assert.assertEquals(new Long(bestCase), getCount(false, false, mode, false,
                "line 1\nline 2\nline 3",
                "line 1\nline 2\nline 3").get(Count.ERR));
        //change two lines ==> 2*2 errors
        Assert.assertEquals(new Long(swapLines), getCount(false, false, mode, false,
                "ab\ncd\nef\ngh",
                "ab\nef\ncd\ngh").get(Count.ERR));
        //delete one line ==> 2 errors
        Assert.assertEquals(new Long(deleteLine), getCount(false, false, mode, false,
                "ab\ncd",
                "ab\nef\ncd").get(Count.ERR));
        //split one line ==> "cd ef" to "cd" and "" to "ef" ==> 3 + 2 = 5 errors
        Assert.assertEquals(new Long(splitLine), getCount(false, false, mode, false,
                "ab\ncd ef\ngh",
                "ab\ncd\nef\ngh").get(Count.ERR));
        Assert.assertEquals(new Long(Math.max(0, splitLine - 1)), getCount(false, false, mode, false,
                "ab\ncdef\ngh",
                "ab\ncd\nef\ngh").get(Count.ERR));
        //merge two line ==> "cd ef" to "cd" and "" to "ef" ==> 3 + 2 = 5 errors
        Assert.assertEquals(new Long(mergeLine), getCount(false, false, mode, false,
                "ab\ncd\nef",
                "ab\ncd ef").get(Count.ERR));
        Assert.assertEquals(new Long(Math.max(0, mergeLine - 1)), getCount(false, false, mode, false,
                "ab\ncd\nef",
                "ab\ncdef").get(Count.ERR));
        Assert.assertEquals(new Long(mergeLine), getCount(false, false, mode, false,
                "ab\ncd\nef\ngh",
                "ab\ncd ef\ngh").get(Count.ERR));
        //test if start works
        Assert.assertEquals(new Long(addStart), getCount(false, false, mode, false,
                "cd\nef\ngh",
                "ab\ncd\nef\ngh").get(Count.ERR));
        Assert.assertEquals(new Long(deleteStart), getCount(false, false, mode, false,
                "ab\ncd",
                "cd").get(Count.ERR));
        Assert.assertEquals(new Long(deleteStart), getCount(false, false, mode, false,
                "ab\ncd\nef",
                "cd\nef").get(Count.ERR));
        Assert.assertEquals(new Long(deleteStart), getCount(false, false, mode, false,
                "ab\ncd\nef\ngh",
                "cd\nef\ngh").get(Count.ERR));
        //test if end works
        Assert.assertEquals(new Long(deleteEnd), getCount(false, false, mode, false,
                "ab\ncd\nef\ngh",
                "ab\ncd\nef").get(Count.ERR));
        Assert.assertEquals(new Long(addEnd), getCount(false, false, mode, false,
                "ab\ncd\nef",
                "ab\ncd\nef\ngh").get(Count.ERR));
        //reverse lines
        Assert.assertEquals(new Long(reverseLines), getCount(false, false, mode, false,
                "ab\ncd\nef",
                "ef\ncd\nab").get(Count.ERR));

    }

    @Test
    public void testWithReadingOrder() {
        testCases(ErrorModuleEnd2End.Mode.RO, 0, 4, 2, 5, 5, 2, 2, 2, 2, 4);
    }

    @Test
    public void testIgnoreReadingOrder() {
        testCases(ErrorModuleEnd2End.Mode.NO_RO, 0, 0, 2, 5, 5, 2, 2, 2, 2, 0);
    }

    @Test
    public void testIgnoreSegmentation() {
        testCases(ErrorModuleEnd2End.Mode.RO_SEG, 0, 4, 2, 0, 0, 2, 2, 2, 2, 4);
    }

    @Test
    public void testIgnoreReadingOrderSegmentation() {
        testCases(ErrorModuleEnd2End.Mode.NO_RO_SEG, 0, 0, 2, 0, 0, 2, 2, 2, 2, 0);
    }


    @Test
    public void testLetter() {
        Assert.assertEquals(new Long(1), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "it's wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(2), getCount(false, true, ErrorModuleEnd2End.Mode.RO, true, "it's wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(3), getCount(false, true, ErrorModuleEnd2End.Mode.RO, false, "its, wrong", "its, wrong").get(Count.COR));
//        Assert.assertEquals(new Long(4), getCount(true, true, true, true, "30 examples, just some...", "('just') <SOME> 30examples??;:").get(Count.TP));
    }

    public Map<Count, Long> getCount(boolean upper, boolean word, ErrorModuleEnd2End.Mode mode, boolean letterNumber, String gt, String hyp) {
        System.out.println("\"" + gt + "\" vs \"" + hyp + "\"");
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

    @Test
    public void testCaseSensitive() {
        System.out.println("CaseSensitive");

    }
}
