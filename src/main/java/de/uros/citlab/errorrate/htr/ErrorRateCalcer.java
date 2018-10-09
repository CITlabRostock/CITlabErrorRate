/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr;

import de.uros.citlab.errorrate.costcalculator.CostCalculatorDft;
import de.uros.citlab.errorrate.interfaces.ICostCalculator;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.errorrate.util.ObjectCounter;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import eu.transkribus.languageresources.extractor.pagexml.PAGEXMLExtractor;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.util.Pair;

/**
 * Class to calculate different metrics on documents
 *
 * @author gundram
 */
public class ErrorRateCalcer {

    public class ResultPagewise extends Result {

        private final List<Result> pageResults = new LinkedList<>();

        public ResultPagewise(Method method) {
            super(method);
        }

        @Override
        public void addCounts(ObjectCounter<Count> counts) {
            super.addCounts(counts);
            Result result = new Result(getMethod());
            result.addCounts(counts);
            pageResults.add(result);
        }

        public List<Result> getPageResults() {
            return pageResults;
        }

    }


    private IErrorModule getErrorModule(Method method) {
        Boolean detailed = Boolean.FALSE;
//        ICostCalculator cc = new CostCalculatorDft();

        IStringNormalizer sn = null;
        switch (method) {
            case BOT:
            case WER:
            case CER:
                break;
            case BOT_ALNUM:
            case WER_ALNUM:
            case CER_ALNUM:
                sn = new StringNormalizerLetterNumber(sn);
                break;
            default:
                throw new RuntimeException("unexpected method '" + method + "'.");
        }

        ITokenizer tok = null;
        switch (method) {
            case BOT:
            case BOT_ALNUM:
            case WER:
            case WER_ALNUM:
                tok = new TokenizerCategorizer(new CategorizerWordMergeGroups());
                break;
            case CER:
            case CER_ALNUM:
                tok = new TokenizerCategorizer(new CategorizerCharacterDft());
                break;
            default:
                throw new RuntimeException("unexpected method '" + method + "'.");
        }
        switch (method) {
            case CER:
            case WER:
            case CER_ALNUM:
            case WER_ALNUM:
                return new ErrorModuleDynProg(tok, sn, detailed);
            case BOT:
            case BOT_ALNUM:
                return new ErrorModuleBagOfTokens(tok, sn, detailed);
            default:
                throw new RuntimeException("unexpected method '" + method + "'.");
        }
    }

    private static Pair<List<String>, List<String>> reshape(List<Pair<String, String>> data) {
        List<String> l1 = new LinkedList<>();
        List<String> l2 = new LinkedList<>();
        for (Pair<String, String> pair : data) {
            l1.add(pair.getFirst());
            l2.add(pair.getSecond());
        }
        return new Pair<>(l1, l2);
    }

    public Result process(File[] hyp, File[] gt, Method method) {
        return process(hyp, gt, new Method[]{method}).get(method);
    }

    public Map<Method, Result> process(File[] hyp, File[] gt, Method... methods) {
        return process(hyp, gt, false, methods);
    }

    public ResultPagewise processPagewise(File[] hyp, File[] gt, Method method) {
        return processPagewise(hyp, gt, new Method[]{method}).get(method);
    }

    public Map<Method, ResultPagewise> processPagewise(File[] hyp, File[] gt, Method... methods) {
        Map<Method, Result> results = process(hyp, gt, true, methods);
        Map<Method, ResultPagewise> res = new HashMap<>();
        for (Method method : results.keySet()) {
            res.put(method, (ResultPagewise) results.get(method));
        }
        return res;
    }

    private Map<Method, Result> process(File[] hyp, File[] gt, boolean pagewise, Method... methods) {
        if (hyp.length != gt.length) {
            throw new RuntimeException("different length of comparation lists (" + hyp.length + " vs " + gt.length + ")");
        }
        HashMap<Method, IErrorModule> modules = new HashMap<>();
        HashMap<Method, Result> results = new HashMap<>();
        for (Method method : methods) {
            modules.put(method, getErrorModule(method));
            results.put(method, pagewise ? new ResultPagewise(method) : new Result(method));
        }
        for (int i = 0; i < gt.length; i++) {
            File fileGT = gt[i];
            File fileHYP = hyp[i];
            Pair<List<String>, List<String>> textlines = reshape(new PAGEXMLExtractor().extractTextFromFilePairwise(fileHYP.getPath(), fileGT.getPath()));
            for (Method method : methods) {
                IErrorModule errorModule = modules.get(method);
                errorModule.calculate(textlines.getFirst(), textlines.getSecond());
                results.get(method).addCounts(errorModule.getCounter());
                errorModule.reset();
            }

        }
        return results;
    }

}
