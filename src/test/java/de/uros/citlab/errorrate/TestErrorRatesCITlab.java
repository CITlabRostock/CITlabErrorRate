/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
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
public class TestErrorRatesCITlab {

    @Test
    public void testLineBreak() {
        Assert.assertEquals(new Long(4), getCount(false, true, false, false, "\n this is \n with\n linebreaks\n ", "this is with linebreaks").get(Count.COR));
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(new Long(1), getCount(false, true, false, false, "this is text ", "is this text").get(Count.COR));
        Assert.assertEquals(new Long(3), getCount(false, true, true, false, "this is text ", "is this text").get(Count.COR));
    }

    @Test
    public void testComposition() {
        Assert.assertEquals(new Long(1), getCount(false, true, false, false, "sa\u0308ße", "säße").get(Count.COR));
        Assert.assertEquals(new Long(1), getCount(true, true, false, false, "SA\u0308SSE", "säße").get(Count.COR));
    }

    @Test
    public void testTokenizer() {
        Assert.assertEquals(new Long(1), getCount(false, true, false, false, "it's wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(2), getCount(false, true, false, false, "its wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(3), getCount(false, true, false, false, "its, wrong", "its, wrong").get(Count.COR));
//        Assert.assertEquals(2, get(false, true, false, true, "COR", "it's wrong", "its wrong"));
    }

    @Test
    public void testErrorType() {
        Assert.assertEquals(new Long(1), getCount(false, true, false, false, "its, wrong", "its wrong").get(Count.INS));
        Assert.assertEquals(new Long(1), getCount(false, true, false, false, "its, wrong", "its. wrong").get(Count.SUB));//substitution
        Assert.assertEquals(new Long(2), getCount(false, true, false, false, "its, wrong", "its. wrong").get(Count.COR));//correct
        Assert.assertEquals(new Long(2), getCount(false, true, true, false, "its, wrong", "its. wrong").get(Count.COR));//true positive
        Assert.assertEquals(new Long(1), (Long) (
                getCount(false, true, true, false, "its, wrong", "its. wrong").getOrDefault(Count.INS, 0L) +
                        getCount(false, true, true, false, "its, wrong", "its. wrong").getOrDefault(Count.SUB, 0L)
        ));//false negative
        Assert.assertEquals(new Long(1), (Long) (
                getCount(false, true, true, false, "its, wrong", "its. wrong").getOrDefault(Count.DEL, 0L) +
                        getCount(false, true, true, false, "its, wrong", "its. wrong").getOrDefault(Count.SUB, 0L)
        ));//false positive
        Assert.assertEquals(new Long(2), getCount(false, true, false, false, "wrong", "its, wrong").get(Count.DEL));
        Assert.assertEquals(new Long(2), (Long) (
                getCount(false, true, true, false, "wrong", "its, wrong").getOrDefault(Count.DEL, 0L) +
                        getCount(false, true, true, false, "wrong", "its, wrong").getOrDefault(Count.SUB, 0L)
        ));//false positive
//        Assert.assertEquals(new Long(2), getCount(false, true, true, false, "wrong", "its, wrong").get(Count.COR));
//        Assert.assertEquals(2, get(false, true, false, true, "COR", "it's wrong", "its wrong"));
    }

    @Test
    public void testLetter() {
        Assert.assertEquals(new Long(1), getCount(false, true, false, false, "it's wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(2), getCount(false, true, false, true, "it's wrong", "its wrong").get(Count.COR));
        Assert.assertEquals(new Long(3), getCount(false, true, false, false, "its, wrong", "its, wrong").get(Count.COR));
        Assert.assertEquals(new Long(4), getCount(true, true, true, true, "30 examples, just some...", "('just') <SOME> 30examples??;:").get(Count.COR));
    }

    public Map<Count, Long> getCount(boolean upper, boolean word, boolean bagoftokens, boolean letterNumber, String gt, String hyp) {
        System.out.println("\"" + gt + "\" vs \"" + hyp + "\"");
        ITokenizer tokenizer = new TokenizerCategorizer(word ? new CategorizerWordMergeGroups() : new CategorizerCharacterDft());
        IStringNormalizer sn = new StringNormalizerDft(Normalizer.Form.NFKC, upper);
        if (letterNumber) {
            sn = new StringNormalizerLetterNumber(sn);
        }
        IErrorModule impl = bagoftokens ? new ErrorModuleEnd2End(false, false, true, tokenizer, true) : new ErrorModuleDynProg(tokenizer, sn, false);
        if(sn!=null&&bagoftokens){
            ((ErrorModuleEnd2End)impl).setStringNormalizer(sn);
        }
        impl.calculate(hyp, gt);
        return impl.getCounter().getMap();
    }

    @Test
    public void testCaseSensitive() {
        System.out.println("CaseSensitive");

    }
}
