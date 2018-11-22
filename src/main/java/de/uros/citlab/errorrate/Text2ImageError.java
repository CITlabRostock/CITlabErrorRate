/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.aligner.BaseLineAligner;
import de.uros.citlab.errorrate.aligner.IBaseLineAligner;
import de.uros.citlab.errorrate.htr.ErrorModuleDynProg;
import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.normalizer.StringNormalizerDftConfigurable;
import de.uros.citlab.errorrate.normalizer.StringNormalizerLetterNumber;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterConfigurable;
import de.uros.citlab.tokenizer.categorizer.CategorizerCharacterDft;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordDftConfigurable;
import de.uros.citlab.tokenizer.categorizer.CategorizerWordMergeGroups;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.languageresources.extractor.xml.XMLExtractor;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Parser to make {@link ErrorModuleDynProg} accessible for the console.
 *
 * @author gundram
 */
public class Text2ImageError {

    private static final Logger LOG = LoggerFactory.getLogger(Text2ImageError.class.getName());
    private final Options options = new Options();

    public Text2ImageError() {
        options.addOption("h", "help", false, "show this help");
        options.addOption("u", "upper", false, "error rate is calculated from upper string (not case sensitive)");
        options.addOption("N", "normcompatibility", false, "compatibility normal form is used (only one of -n or -N is allowed)");
        options.addOption("n", "normcanonic", false, "canonical normal form is used (only one of -n or -N is allowed)");
        options.addOption("c", "category", true, "property file to categorize codepoints with codepoint-category-mapping");
        options.addOption("i", "isolated", true, "property file to define, if a codepoint is used as single token or not with codepoint-boolean-mapping");
        options.addOption("s", "separator", true, "property file to define, if a codepoint is a separator with codepoint-boolean-mapping");
        options.addOption("m", "mapper", true, "property file to normalize strings with a string-string-mapping");
        options.addOption("w", "wer", false, "calculate word error rate instead of character error rate");
        options.addOption("p", "pagewise", false, "output values pagewise");
        options.addOption("l", "letters", false, "calculate error rates only for codepoints, belonging to the category \"L\"");
        options.addOption("b", "bag", false, "using bag of words instead of dynamic programming tabular");
        options.addOption("t", "thresh", true, "threshold for alignment of textlines (the higher the harder is alignment. value have to be in [0,1]");
    }

    public Map<String, Double> run(String[] args) {
        return run(args, null, null);
    }

    public Map<String, Double> run(String[] args, File[] gts, File[] hyps) {
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
            IStringNormalizer.IPropertyConfigurable snd = new StringNormalizerDftConfigurable(form, upper);
            //property map for substitute substrings while normalization
            if (cmd.hasOption('m')) {
                String optionValue = cmd.getOptionValue('m');
                try {
                    snd.putSubstitutionProperties(optionValue);
                } catch (Throwable e) {
                    help("cannot load file '" + optionValue + "' properly - use java property syntax in file.", e);
                }
            }
            //CATEGORIZER
            ICategorizer categorizer = null;
            if (!cmd.hasOption('c') && !cmd.hasOption('s') && !cmd.hasOption('i')) {
                categorizer = wer ? new CategorizerWordMergeGroups() : new CategorizerCharacterDft();
            } else {
                ICategorizer.IPropertyConfigurable categorizerConfigurable = wer ? new CategorizerWordDftConfigurable() : new CategorizerCharacterConfigurable();
                categorizer = categorizerConfigurable;
                //property map for categorize codepoints
                if (cmd.hasOption('c')) {
                    String optionValue = cmd.getOptionValue('c');
                    try {
                        categorizerConfigurable.putCategoryProperties(optionValue);
                    } catch (Throwable e) {
                        help("cannot load file '" + optionValue + "' properly - use java property syntax in file.", e);
                    }
                }
                //property map for specify separator codepoints
                if (cmd.hasOption('s')) {
                    String optionValue = cmd.getOptionValue('s');
                    try {
                        categorizerConfigurable.putSeparatorProperties(optionValue);
                    } catch (Throwable e) {
                        help("cannot load file '" + optionValue + "' properly - use java property syntax in file.", e);
                    }
                }
                //property map for specify isolated codepoints
                if (cmd.hasOption('i')) {
                    String optionValue = cmd.getOptionValue('i');
                    try {
                        categorizerConfigurable.putIsolatedProperties(optionValue);
                    } catch (Throwable e) {
                        help("cannot load file '" + optionValue + "' properly - use java property syntax in file.", e);
                    }
                }
            }
            double threshold = 0.7;
            if (cmd.hasOption('t')) {
                threshold = Double.parseDouble(cmd.getOptionValue('t'));
            } else {
                LOG.warn("threshold not set, use {} as default", threshold);
            }
            //ouput calculate pagewise
            boolean pagewise = cmd.hasOption('p');
            //normalize to letter or to all codepoints?
            IStringNormalizer sn = cmd.hasOption('l') ? new StringNormalizerLetterNumber(snd) : snd;
            IErrorModule errorModule = new ErrorModuleDynProg(categorizer, sn, detailed);
//            IBaseLineAligner baseLineAligner = new BaseLineAlignerSameBaselines();
            IBaseLineAligner baseLineAligner = new BaseLineAligner();
            List<String> argList = cmd.getArgList();
            List<String> refs;
            List<String> recos;
            if (argList.size() != 2) {
                if (gts == null || hyps == null) {
                    help("no arguments given, missing <list_pageXml_groundtruth> <list_pageXml_hypothesis>.");
                }
                refs = new LinkedList<>();
                recos = new LinkedList<>();
                if (gts.length != hyps.length) {
                    throw new RuntimeException("groundtruth list and hypothesis list differ in length");
                }
                for (int i = 0; i < hyps.length; i++) {
                    refs.add(gts[i].getPath());
                    recos.add(hyps[i].getPath());
                }
            } else {
                try {
                    refs = FileUtils.readLines(new File(argList.get(0)), "UTF-8");
                } catch (IOException ex) {
                    throw new RuntimeException("cannot load file '" + argList.get(0) + "'", ex);
                }
                try {
                    recos = FileUtils.readLines(new File(argList.get(1)), "UTF-8");
                } catch (IOException ex) {
                    throw new RuntimeException("cannot load file '" + argList.get(1) + "'", ex);
                }
            }
            if (refs.size() != recos.size()) {
                throw new RuntimeException("loaded list " + argList.get(0) + " and " + argList.get(1) + " do not have the same number of lines.");
            }
            double sum = 0;
            double sumAll = 0;
            double emGt = 0;
            double emHyp = 0;
            double emErr = 0;
            int cntLinesErr = 0;
            int cntLinesGt = 0;
            for (int i = 0; i < recos.size(); i++) {
                double emGtCur = 0;
                double emHypCur = 0;
                double emErrCur = 0;
                double sumCur = 0;
                double sumAllCur = 0;
                int cntLinesCorErr = 0;
                int cntLinesGtCur = 0;
                String reco = recos.get(i);
                String ref = refs.get(i);
                LOG.debug("process [{}/{}]:{} <> {}", i + 1, recos.size(), reco, ref);
                List<XMLExtractor.Line> linesGT = XMLExtractor.getLinesFromFile(new File(ref));
                List<XMLExtractor.Line> linesLA = XMLExtractor.getLinesFromFile(new File(reco));
                List<XMLExtractor.Line> linesHyp = new LinkedList<>();
                for (XMLExtractor.Line line : linesLA) {
                    if (line.textEquiv != null) {
                        linesHyp.add(line);
                    }
                }
                //TODO: is it dispose that each GT-line can be aligned to only one HYP-line?
                IBaseLineAligner.IAlignerResult alignment = baseLineAligner.getAlignment(toArray(linesGT), toArray(linesLA), toArray(linesHyp), threshold, null);
                int[][] precAlignment = alignment.getGTLists();
                // Algorithm returns index of GT-lines, which overlap with a HYP-line with more than <var_t> percent.
                // The array can have length 0-#GT-lines
                for (int j = 0; j < precAlignment.length; j++) {
                    int[] idsGT = precAlignment[j];
                    String textHyp = linesHyp.get(j).textEquiv;
                    String gtText = null;
                    //create GT-String from 0, 1 or arbitrary many strings
                    switch (idsGT.length) {
                        case 0:
                            gtText = "";
                            break;
                        case 1:
                            gtText = linesGT.get(idsGT[0]).textEquiv;
                            break;
                        default: {
                            StringBuilder sb = new StringBuilder();
                            sb.append(linesGT.get(idsGT[0]).textEquiv);
                            for (int k = 1; k < linesGT.size(); k++) {
                                sb.append(' ').append(linesGT.get(idsGT[k]).textEquiv);
                            }
                            gtText = sb.toString();
                        }
                    }
                    //TODO: make space as seperator optional
                    if (!gtText.equals(textHyp)) {
                        cntLinesCorErr += idsGT.length;
                    }
                    //TODO: check if order ist okay: gt-hyp vs. reco-ref
                    errorModule.calculate(textHyp, gtText);
                }
                //calculate couverage of GT-lines by HYP-lines. If the HYP couver all GT-lines, sumCur=sumAllCur
                double[] recValue = alignment.getRecallsLA();
                for (int j = 0; j < recValue.length; j++) {
                    sumCur += linesGT.get(j).textEquiv.length() * recValue[j];
                }
                for (XMLExtractor.Line line : linesGT) {
                    sumAllCur += line.textEquiv.length();
                }
                emErrCur = errorModule.getCounter().get(Count.ERR);
                emGtCur = errorModule.getCounter().get(Count.GT);
                emHypCur = errorModule.getCounter().get(Count.HYP);
                errorModule.reset();
                cntLinesGtCur = linesGT.size();
                if (pagewise) {
                    System.out.println(String.format("P-Value(text): %.4f R-Value(text): %.4f R-Value(geom): %.4f R-Value(line): %.4f - %s <>%s", ((double) emErrCur) / emGtCur, ((double) emErrCur) / sumCur, ((double) sumCur) / sumAllCur, ((double) cntLinesCorErr) / cntLinesGtCur, reco, ref));
                }
                sum += sumCur;
                sumAll += sumAllCur;
                emErr += emErrCur;
                emGt += emGtCur;
                emHyp += emHypCur;
                cntLinesErr += cntLinesCorErr;
                cntLinesGt += cntLinesGtCur;
            }
            if(LOG.isDebugEnabled()){
                LOG.debug("sum = {} sumAll = {} emErr = {} emGT = {} emHyp = {}", sum,sumAll,emErr,emGt,emHyp);
                LOG.debug("lines: err = {} sum = {}", cntLinesErr,cntLinesGt);
            }
            HashMap res = new HashMap();
            res.put("P_text", 1.0 - emErr / emGt);
            res.put("R_text", 1.0 - emErr / emHyp);
            res.put("R_geom", sum / sumAll);
            res.put("LER", ((double) cntLinesErr) / cntLinesGt);
            res.put("CER", emErr / emGt);
            return res;
        } catch (ParseException e) {
            help("Failed to parse comand line properties", e);
            return null;
        }

    }

    private static Polygon[] toArray(List<XMLExtractor.Line> lines) {
        Polygon[] res = new Polygon[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            res[i] = lines.get(i).baseLine;
        }
        return res;
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
                "java -cp errorrate.jar de.uros.citlab.errorrate.text2image.Text2ImageParser <list_pageXml_groundtruth> <list_pageXml_hypothesis>",
                "This method calculates the precision and recall between two lists of PAGE-XML-files, assuming that the Hypthesis is the result of a Text2Image alignment"
                        + " As input it requires two lists of PAGE-XML-files. The first one is the ground truth, the second one is the hypothesis/alignment."
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
        Text2ImageError erp = new Text2ImageError();
        Map<String, Double> run = erp.run(args);
        for (String string : run.keySet()) {
            System.out.println(String.format("%10s: %.4f", string, run.get(string)));
        }
    }
}
