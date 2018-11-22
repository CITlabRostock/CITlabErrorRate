/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.ErrorModuleBagOfTokens;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.interfaces.IErrorModuleWithSegmentation;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDft;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDftConfigurable;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.errorrate.util.ExtractUtil;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterConfigurable;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordDftConfigurable;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.languageresources.extractor.pagexml.PAGEXMLExtractor;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser to make {@link ErrorModuleDynProg} accessible for the console.
 *
 * @author gundram
 */
public class End2EndError {

    private static final Logger LOG = LoggerFactory.getLogger(End2EndError.class.getName());
    private final Options options = new Options();

    public End2EndError() {
        options.addOption("h", "help", false, "show this help");
        options.addOption("u", "upper", false, "error rate is calculated from upper string (not case sensitive)");
        options.addOption("N", "normcompatibility", false, "compatibility normal form is used (only one of -n or -N is allowed)");
        options.addOption("n", "normcanonic", false, "canonical normal form is used (only one of -n or -N is allowed)");
        options.addOption("r", "ignore-readingorder", false, "do not penalize errors in reading order");
        options.addOption("s", "ignore-segmentation", false, "do not penalize errors in over- or under-segmentation");
        options.addOption("g", "use-geometry", false, "use baslines of transcription as additional constraint for error calculation");
        options.addOption("t", "thresh", true, "if -g is set: minimal couverage [0.0,1.0] between baseline so that they are assumed to be adjacent (default: 0.0)");
//        options.addOption("m", "mapper", true, "property file to normalize strings with a string-string-mapping");
//        options.addOption("w", "wer", false, "calculate word error rate instead of character error rate");
        options.addOption("d", "detailed", false, "use detailed calculation (creates confusion map) (only one of -d and -D allowed at the same time) ");
        options.addOption("D", "Detailed", false, "use detailed calculation (creates substitution map) (only one of -d and -D allowed at the same time)");
        options.addOption("l", "letters", false, "calculate error rates only for codepoints, belonging to the unicode category \"L\", \"N\" or \"Z\".");
//        options.addOption("b", "bag", false, "using bag of words instead of dynamic programming tabular");
    }

    public Result run(String[] args) {

        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);

            //Help?
            if (cmd.hasOption("h")) {
                help();
            }
            //Word or Character Error Rate?
            boolean wer = cmd.hasOption('w');
            if (cmd.hasOption('d') && cmd.hasOption('D')) {
                help("only one of the parameter -d and -D have to be set. Note that -D includes the output of -d");
            }
            if (cmd.hasOption('d') || cmd.hasOption('D')) {
                help("the options -d and -D are not supported yet.");
            }
            //how detailed should the output be
            Boolean detailed = cmd.hasOption('d') ? null : cmd.hasOption('D');
            //upper case?
            boolean upper = cmd.hasOption('u');
            //canoncal or compatibility composition form?
            boolean normcompatibility = cmd.hasOption('N');
            boolean normcanonic = cmd.hasOption('n');
            if (normcompatibility && normcanonic) {
                help("both normalization options are on - use -n or -N");
            }
            Normalizer.Form form = null;
            if (normcompatibility) {
                form = Normalizer.Form.NFKC;
            }
            if (normcanonic) {
                form = Normalizer.Form.NFC;
            }
            //STRING NORMALIZER
            IStringNormalizer snd = new StringNormalizerDft(form, upper);
            //CATEGORIZER
            ICategorizer categorizer = new CategorizerCharacterDft();
            //normalize to letter or to all codepoints?
            IStringNormalizer sn = cmd.hasOption('l') ? new StringNormalizerLetterNumber(snd) : snd;
            boolean geometry = cmd.hasOption('g');
            ErrorModuleEnd2End.Mode mode =
                    cmd.hasOption('r') ?
                            cmd.hasOption('s') ?
                                    ErrorModuleEnd2End.Mode.NO_RO_SEG :
                                    ErrorModuleEnd2End.Mode.NO_RO :
                            cmd.hasOption('s') ?
                                    ErrorModuleEnd2End.Mode.RO_SEG :
                                    ErrorModuleEnd2End.Mode.RO;
            IErrorModuleWithSegmentation em = new ErrorModuleEnd2End(categorizer, sn, mode, geometry, detailed);
            if (cmd.hasOption('t')) {
                double t = Double.parseDouble(cmd.getOptionValue('t'));
                if(t<0.0||t>=1.0){
                    throw new RuntimeException("threshold for couverage (parameter -t) have to be in intervall [0.0,1.0)");
                }
                ((ErrorModuleEnd2End) em).setThresholdCouverage(t);
            }
            List<String> argList = cmd.getArgList();
            Result res = new Result(cmd.hasOption('l') ? Method.CER_ALNUM : Method.CER);
            if (argList.size() != 2) {
                help("no arguments given, missing <list_pageXml_groundtruth> <list_pageXml_hypothesis>.");
            }
            List<String> refs;
            try {
                refs = FileUtils.readLines(new File(argList.get(0)), "UTF-8");
            } catch (IOException ex) {
                throw new RuntimeException("cannot load file '" + argList.get(0) + "'", ex);
            }
            List<String> recos;
            try {
                recos = FileUtils.readLines(new File(argList.get(1)), "UTF-8");
            } catch (IOException ex) {
                throw new RuntimeException("cannot load file '" + argList.get(1) + "'", ex);
            }
            if (refs.size() != recos.size()) {
                throw new RuntimeException("loaded list " + argList.get(0) + " and " + argList.get(1) + " do not have the same number of lines.");
            }
            for (int i = 0; i < recos.size(); i++) {
                String reco = recos.get(i);
                String ref = refs.get(i);
                LOG.debug("process [{}/{}]:{} <> {}", i + 1, recos.size(), reco, ref);
                if (geometry) {
                    List<ILine> recoLines = ExtractUtil.getLinesFromFile(reco);
                    List<ILine> refLines = ExtractUtil.getLinesFromFile(ref);
                    em.calculateWithSegmentation(recoLines, refLines);
                } else {
                    List<String> recoLines = ExtractUtil.getTextFromFile(reco);
                    List<String> refLines = ExtractUtil.getTextFromFile(ref);
                    em.calculate(recoLines, refLines);
                }
            }
            //print statistic to console
            List<Pair<Count, Long>> resultOccurrence = em.getCounter().getResultOccurrence();
            if (detailed == null || detailed == true) {
                List<String> results = em.getResults();
                for (String result : results) {
                    System.out.println(result);
                }
            }
            res.addCounts(em.getCounter());
            return res;
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
                "java -jar <this-jar>.jar <list_pageXml_groundtruth> <list_pageXml_hypothesis>",
                "This method calculates the (character) error rates between two lists of PAGE-XML-files."
                        + " As input it requires two lists of PAGE-XML-files. The first one is the ground truth, the second one is the hypothesis."
                        + " The programm returns the character error rate (CER).",
                options,
                suffix,
                true
        );
        System.exit(0);
    }

    public static void main(String[] args) {
//        args = ("--help").split(" ");
        End2EndError erp = new End2EndError();
        Result res = erp.run(args);
        for (Metric metric : res.getMetrics().keySet()) {
            System.out.println(metric + " = " + res.getMetric(metric));
        }

    }
}
