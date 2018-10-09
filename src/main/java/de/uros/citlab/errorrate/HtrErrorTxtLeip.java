/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.costcalculator.CostCalculatorDft;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Method;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.types.Result;
import de.uros.citlab.tokenizer.TokenizerConfig;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser to make {@link ErrorModuleDynProg} accessible for the console.
 *
 * @author gundram
 */
public class HtrErrorTxtLeip {

    private static final Logger LOG = Logger.getLogger(HtrErrorTxtLeip.class.getName());
    private final Options options = new Options();

    public HtrErrorTxtLeip() {
        options.addOption("h", "help", false, "show this help");
        options.addOption("u", "upper", false, "error rate is calculated from upper string (not case sensitive)");
        options.addOption("p", "pfile", true, "property file to configure tokenizer");
        options.addOption("d", "detailed", false, "use detailed calculation (creates confusion map) (only one of -d and -D allowed at the same time) ");
        options.addOption("D", "Detailed", false, "use detailed calculation (creates substitution map) (only one of -d and -D allowed at the same time)");
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
//            boolean wer = cmd.hasOption('w');
            if (cmd.hasOption('d') && cmd.hasOption('D')) {
                help("only one of the parameter -d and -D have to be set. Note that -D includes the output of -d");
            }
            //how detailed should the output be
            Boolean detailed = cmd.hasOption('d') ? null : cmd.hasOption('D');
            if (!cmd.hasOption('p')) {
                help("Please set path to property file \'-p\'");
            }
            //CATEGORIZER
            //normalize to letter or to all codepoints?
            IErrorModule em = new ErrorModuleDynProg(new TokenizerConfig(cmd.getOptionValue('p')), null, detailed);
            Result res = null;
            if (cmd.hasOption('b')) {
                res = new Result(cmd.hasOption('l') ? Method.BOT_ALNUM : Method.BOT);
            } else if (cmd.hasOption('w')) {
                res = new Result(cmd.hasOption('l') ? Method.WER_ALNUM : Method.WER);
            } else {
                res = new Result(cmd.hasOption('l') ? Method.CER_ALNUM : Method.CER);
            }
            List<String> argList = cmd.getArgList();
            if (argList.size() != 2) {
                help("no arguments given, missing <txt_groundtruth> <txt_hypothesis>.");
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
                LOG.log(Level.FINE, "process [{0}/{1}]:{2} <> {3}", new Object[]{i + 1, recos.size(), reco, ref});
                LOG.log(Level.FINE, "ref: ''{0}''", ref);
                LOG.log(Level.FINE, "reco: ''{0}''", reco);
                em.calculate(reco, ref);
            }
            //print statistic to console
            //print statistic to console
            List<Pair<Count, Long>> resultOccurrence = em.getCounter().getResultOccurrence();
            if (detailed == null || detailed == true) {
                List<String> results = em.getResults();
                for (String result : results) {
                    System.out.println(result);
                }
            }
            Map<Count, Long> map = new HashMap<>();
            for (Pair<Count, Long> pair : resultOccurrence) {
                map.put(pair.getFirst(), pair.getSecond());
            }
            for (Count count : new Count[]{Count.DEL, Count.INS, Count.SUB, Count.COR, Count.GT, Count.HYP}) {
                map.putIfAbsent(count, 0L);
            }

            map.put(Count.ERR, map.get(Count.DEL) + map.get(Count.INS) + map.get(Count.SUB));
            res.addCounts(em.getCounter());
            return res;

        } catch (ParseException e) {
            help("Failed to parse comand line properties", e);
        }
        return null;
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
                "java -cp <this-jar>.jar de.uros.citlab.errorrate.ErrorRateParserTxtLeipTok <list_pageXml_groundtruth> <list_pageXml_hypothesis>",
                "This method calculates the (character) error rates between two lists of textfiles."
                        + " As input it requires two lists of UTF8-encoded text-files. The first one is the ground truth, the second one is the hypothesis."
                        + " The programm returns the number of manipulations (corrects, substitution, insertion or deletion)"
                        + " and the corresponding percentage to come from the hypothesis to the ground truth."
                        + " The order of the xml-files in both lists has to be the same.",
                options,
                suffix,
                true
        );
        System.exit(0);
    }

    public static void main(String[] args) {
//        args = ("--help").split(" ");
        HtrErrorTxtLeip erp = new HtrErrorTxtLeip();
        Result res = erp.run(args);
        for (Metric metric : res.getMetrics().keySet()) {
            System.out.println(metric + " = " + res.getMetric(metric));
        }

    }
}
