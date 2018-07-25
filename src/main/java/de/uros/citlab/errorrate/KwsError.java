/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

//github.com/Transkribus/TranskribusErrorRate.git
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.panayotis.gnuplot.JavaPlot;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.uros.citlab.errorrate.aligner.BaseLineAligner;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.kws.KWSEvaluationMeasure;
import de.uros.citlab.errorrate.kws.measures.IRankingMeasure;
import de.uros.citlab.errorrate.kws.measures.IRankingStatistic;
import de.uros.citlab.errorrate.types.KWS.GroundTruth;
import de.uros.citlab.errorrate.types.KWS.Result;
import de.uros.citlab.errorrate.types.KWS.Word;
import de.uros.citlab.errorrate.util.PlotUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Parser to make {@link ErrorModuleDynProg} accessible for the console.
 *
 * @author gundram
 */
public class KwsError {

    private static final Logger LOG = Logger.getLogger(KwsError.class.getName());
    private final Options options = new Options();
    public final KWSEvaluationMeasure evaluationMeasure = new KWSEvaluationMeasure(new BaseLineAligner());

    public KwsError() {
        options.addOption("h", "help", false, "show this help");
//        options.addOption("p", "pages", true, "path to a file which contains the pathes to the PAGE-Xml-files (as <groundtruth> a keyword-list can be set. if <groundtruth> is not set, all possible keywords of the validation are used)");
//        options.addOption("s", "substring", false, "if 'p' is set: a keyword can be a substring af a word.");
        options.addOption("m", "metrics", true, ",-seperated list of methods " + Arrays.toString(IRankingMeasure.Measure.values()));
        options.addOption("d", "display", false, "display PR-Curve");
        options.addOption("s", "save", true, "save PR-Curve to given path");
//        options.addOption("i", "index", false, "result file contains index of result file list, not the path to the image");
//        options.addOption("k", "keywords", true, "if no kw list is given, generated kw list is written to given path");
    }

    public KWSEvaluationMeasure getEvaluationMeasure() {
        return evaluationMeasure;
    }

    private static Result getHyp(File path) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            return gson.fromJson(new FileReader(path), Result.class);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static GroundTruth getGT(File path) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            return gson.fromJson(new FileReader(path), GroundTruth.class);
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

//    private Pair<String[], String[]> getListsPageAndIndex(CommandLine cmd) {
//        File listFile = new File(cmd.getOptionValue('p'));
//        if (!listFile.exists()) {
//            help("file " + listFile.getPath() + " containing the xml-pathes does not exist.");
//        }
//        String[] pagesFile = null;
//        String[] pagesIndex = null;
//        try {
//            pagesFile = FileUtils.readLines(listFile).toArray(new String[0]);
//        } catch (IOException ex) {
//            help("file " + listFile.getPath() + " containing the xml-pathes cannot be laoded.", ex);
//        }
//        if (cmd.hasOption('i')) {
//            pagesIndex = new String[pagesFile.length];
//            for (int i = 0; i < pagesFile.length; i++) {
//                pagesIndex[i] = "" + i;
//            }
//        } else {
//            pagesIndex = pagesFile;
//        }
//        return new Pair<>(pagesFile, pagesIndex);
//    }
//    private List<String> getKeywords(KWS.GroundTruth gt) {
//        Set<String> resSet = new LinkedHashSet<>();
//        for (KWS.Page page : gt.getPages()) {
//            for (KWS.Line line : page.getLines()) {
//                for (KWS.Line line1 : page.getLines()) {
//                    resSet.addAll(line.getKeyword2Baseline().keySet());
//                }
//            }
//        }
//        ArrayList<String> res = new ArrayList<>(resSet);
//        Collections.sort(res);
//        return res;
//    }
    public Map<IRankingMeasure.Measure, Double> run(String[] args) {

        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);

            //Help?
            if (cmd.hasOption("h")) {
                help();
            }
            String[] args1 = cmd.getArgs();
            if (args1.length != 2) {
                help("number of arguments have to be 2, but is " + args1.length + ".");
            }
            File hypoFile = new File(args1[0]);
            if (!hypoFile.exists()) {
                help("kws result file " + hypoFile.getPath() + " does not exists.");
            }
            File gtFile = new File(args1[1]);
            if (!gtFile.exists()) {
                help("kws groundtruth file " + gtFile.getPath() + " does not exists.");
            }
            Result hyp = null;
            try {
                hyp = getHyp(hypoFile);
            } catch (RuntimeException ex) {
                help("cannot load kws result file '" + hypoFile.getPath() + "'.", ex);
            }
            GroundTruth gt = null;
            try {
                gt = getGT(gtFile);
            } catch (RuntimeException ex) {
                help("cannot load kws result file '" + gtFile.getPath() + "'.", ex);
            }
//            if (args1.length == 1) {
//                if (!cmd.hasOption('p')) {
//                    help("no groundtruth file and no file containing the xml-pathes (-p <file>) is given");
//                }
//                if (cmd.hasOption('s')) {
//                    help("if no keyword list is provided keywords can only be extracted automatically as non-substrings");
//                }
//                Pair<String[], String[]> listsPageAndIndex = getListsPageAndIndex(cmd);
//                final ITokenizer tokIntern = new TokenizerCategorizer(new CategorizerWordMergeGroups());
//                final IStringNormalizer sn = new StringNormalizerLetterNumber(null);
//                KeywordExtractor kwe = new KeywordExtractor(true);
//                ITokenizer tok = new ITokenizer() {
//                    @Override
//                    public List<String> tokenize(String string) {
//                        return tokIntern.tokenize(sn.normalize(string));
//                    }
//                };
//                gt = kwe.getKeywordGroundTruth(
//                        listsPageAndIndex.getFirst(),
//                        listsPageAndIndex.getSecond(),
//                        tok);
//                if (cmd.hasOption('k')) {
//                    try {
//                        FileUtils.writeLines(new File(cmd.getOptionValue('k')), getKeywords(gt));
//                    } catch (IOException ex) {
//                        help("cannot save keyword list to file " + cmd.getOptionValue('k') + ".", ex);
//                    }
//                }
//            } else {
//                if (cmd.hasOption('p')) {
//                    Pair<String[], String[]> listsPageAndIndex = getListsPageAndIndex(cmd);
//                    List<String> readLines = null;
//                    try {
//                        readLines = FileUtils.readLines(gtFile);
//                    } catch (IOException ex) {
//                        help("cannot load groundtruth file " + gtFile.getPath() + ".", ex);
//                    }
//
//                    KeywordExtractor kwe = new KeywordExtractor(!cmd.hasOption('s'));
//                    gt = kwe.getKeywordGroundTruth(
//                            listsPageAndIndex.getFirst(),
//                            listsPageAndIndex.getSecond(),
//                            readLines);
//                } else {
//                }
//            }
//            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
//            FileUtils.write(new File("gt.json"), gson.toJson(gt));
//            gt = gson.fromJson(FileUtils.readFileToString(new File("gt.json")), GroundTruth.class);
            List<IRankingMeasure.Measure> m = new LinkedList<>();
            if (cmd.hasOption('m')) {
                String[] split = cmd.getOptionValue('m').split(",");
                for (String string : split) {
                    m.add(IRankingMeasure.Measure.valueOf(string.trim()));
                }
            } else {
                m.addAll(Arrays.asList(IRankingMeasure.Measure.values()));
            }
            evaluationMeasure.setGroundtruth(gt);
            evaluationMeasure.setResults(hyp);
            Map<IRankingMeasure.Measure, Double> measure = evaluationMeasure.getMeasure(m);
            if (cmd.hasOption('d') || cmd.hasOption('s')) {
                StringBuilder sb = new StringBuilder();
                if (measure.containsKey(IRankingMeasure.Measure.MAP)) {
                    sb.append(String.format("MAP=%.3f", measure.get(IRankingMeasure.Measure.MAP))).append(' ');
                }
                if (measure.containsKey(IRankingMeasure.Measure.R_PRECISION)) {
                    sb.append(String.format("R-Prec=%.3f", measure.get(IRankingMeasure.Measure.R_PRECISION))).append(' ');
                }
                String name = sb.toString().trim();
                Map<IRankingStatistic.Statistic, double[]> stats = evaluationMeasure.getStats(Arrays.asList(IRankingStatistic.Statistic.PR_CURVE));
                JavaPlot prCurve = PlotUtil.getPRCurve(stats.values().iterator().next(), name);
                if (cmd.hasOption('d')) {
                    PlotUtil.getDefaultTerminal().accept(prCurve);
                }
                if (cmd.hasOption('s')) {
                    PlotUtil.getImageFileTerminal(new File(cmd.getOptionValue('s'))).accept(prCurve);
                }
//                PlotUtil.getImageFileTerminal(new File("")).accept(prCurve);
            }
            return measure;
        } catch (ParseException e) {
            help("Failed to parse comand line properties", e);
            return null;
        }
    }

    private void help() {
        help(null, null);
    }

    private void help(String suffix) {
        help(suffix, null);
    }

    private void help(String suffix, Throwable e) {
        // This prints out some help
        if (suffix != null && !suffix.isEmpty()) {
            suffix = "ERROR:\n" + suffix;
            if (e != null) {
                suffix += "\n" + e.getMessage();
            }
        }
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp(
                "java -cp <this-jar>.jar " + KwsError.class.getName() + " <path_result_file> <path_groundtruth_file>",
                "This method calculates different measures for KWS results. "
                + "Both files have to be json-files with the desired structure.",
                options,
                suffix,
                true
        );
        System.exit(0);
    }

    public static void main(String[] args) {
//        args = ("--help").split(" ");
        KwsError erp = new KwsError();
        Map<IRankingMeasure.Measure, Double> res = erp.run(args);
        for (IRankingMeasure.Measure measure : res.keySet()) {
            System.out.println(measure + " = " + res.get(measure));
        }

    }
}
