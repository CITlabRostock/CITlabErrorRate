/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr;

import de.uros.citlab.errorrate.htr.end2end.AlignmentTask;
import de.uros.citlab.errorrate.interfaces.IErrorModuleWithSegmentation;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.interfaces.IPoint;
import de.uros.citlab.errorrate.types.*;
import de.uros.citlab.errorrate.util.ObjectCounter;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;

import java.util.*;

/**
 * Module, which counts the differences between two bag of tokens, namely the
 * bags of tokens from the recognition/hypothesis and the reference/ground
 * truth. uses the calculate the error rates between tokens. Some other classes
 * are needed, to calculate the error rate. Two tokens are the same, if their
 * unicode representation is the same. See {@link ITokenizer} and
 * {@link IStringNormalizer} for more details.
 *
 * @author gundram
 */
public class ErrorModuleBagOfTokens implements IErrorModuleWithSegmentation {

    private final ObjectCounter<Count> counter = new ObjectCounter<>();
    private final ObjectCounter<Substitution> substitutionCounter = new ObjectCounter<>();
    //    private final ObjectCounter<String> counterTP = new ObjectCounter<>();
//    private final ObjectCounter<String> counterFP = new ObjectCounter<>();
//    private final ObjectCounter<String> counterFN = new ObjectCounter<>();
    private final ITokenizer tokenizer;
    private final Boolean detailed;
    private final IStringNormalizer stringNormalizer;

    public ErrorModuleBagOfTokens(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Boolean detailed) {
        this.detailed = detailed;
        if (tokenizer == null) {
            throw new RuntimeException("no tokenizer given (is null)");
        }
        this.tokenizer = tokenizer;
        this.stringNormalizer = stringNormalizer;
    }

    private static final class Token implements Comparable<Token> {
        private final String value;
        private final int lineIdx;

        public Token(String value, int lineIdx) {
            this.value = value;
            this.lineIdx = lineIdx;
        }

        @Override
        public int compareTo(Token o) {
            final int res = value.compareTo(o.value);
            if (res != 0) return res;
            return Integer.compare(lineIdx, o.lineIdx);
        }

        @Override
        public String toString() {
            return "Token{" +
                    "value='" + value + '\'' +
                    ", lineIdx=" + lineIdx +
                    '}';
        }
    }

    /**
     * normalize and tokenize both inputs. Afterwards find the cheapest cost to
     * manipulate the recognition tokens to come to the reference tokens. Count
     * the manipulation which had to be done. If detailed==null or
     * detailed==True, confusion/substitution map is filled.
     */
    List<ILineComparison> calculateIntern(AlignmentTask alignmentTask, boolean calcLineComparison) {
        LinkedList<Token> recoTokens = new LinkedList<>();
        String[] recosLst = alignmentTask.getRecos();
        int[] recoLineMap = alignmentTask.getRecoLineMap();
        for (int i = 0; i < recosLst.length; i++) {
            if(recosLst[i].equals("\n")){
                continue;
            }
            int lineID = recoLineMap == null ? -1 : recoLineMap[i];
//            for (String token : tokenizer.tokenize(recosLst[i])) {
            recoTokens.add(new Token(recosLst[i], lineID));
//            }
        }
        LinkedList<Token> refTokens = new LinkedList<>();
        String[] refsLst = alignmentTask.getRefs();
        int[] refLineMap = alignmentTask.getRefLineMap();
        for (int i = 0; i < refsLst.length; i++) {
            if(refsLst[i].equals("\n")){
                continue;
            }
            int lineID = refLineMap == null ? -1 : refLineMap[i];
//            for (String token : tokenizer.tokenize(refsLst[i])) {
            refTokens.add(new Token(refsLst[i], lineID));
//            }
        }
        List<ILineComparison> res = calcLineComparison ? new LinkedList<>() : null;
        Collections.sort(refTokens);
        Collections.sort(recoTokens);
        int idxRef = 0;
        int idxReco = 0;
        for (int i = 0; i < refTokens.size(); i++) {

//        while (idxReco < recoTokens.size() && idxRef < refTokens.size()) {
            Token recoToken = recoTokens.get(idxReco);
            Token refToken = refTokens.get(idxRef);
            int cmp = refToken.compareTo(recoToken);
            Count c = null;
            if (cmp == 0) {
                c = Count.TP;
                counter.add(Count.TP);
                counter.add(Count.GT);
                counter.add(Count.HYP);
                idxReco++;
                idxRef++;
                if (detailed != null && detailed) {
                    substitutionCounter.add(new Substitution(recoToken.value, refToken.value));
                }
            } else if (cmp > 0) {
                c = Count.FP;
                counter.add(Count.HYP);
                counter.add(Count.FP);
                idxReco++;
                if (detailed == null || detailed) {
                    substitutionCounter.add(new Substitution(recoToken.value, null));
                }
            } else {
                c = Count.FN;
                counter.add(Count.GT);
                counter.add(Count.FN);
                idxRef++;
                if (detailed == null || detailed) {
                    substitutionCounter.add(new Substitution(null, refToken.value));
                }
            }
            if (calcLineComparison) {
                res.add(getLineComparison(recoToken, refToken, c));
            }
//        }
        }
        for (; idxRef < refTokens.size(); idxRef++) {
            Token refToken = refTokens.get(idxRef);
            counter.add(Count.GT);
            counter.add(Count.FN);
            if (detailed == null || detailed) {
                substitutionCounter.add(new Substitution(null, refToken.value));
            }
        }
        for (; idxReco < recoTokens.size(); idxReco++) {
            Token recoToken = recoTokens.get(idxReco);
            counter.add(Count.HYP);
            counter.add(Count.FP);
            if (detailed == null || detailed) {
                substitutionCounter.add(new Substitution(recoToken.value, null));
            }
        }
        return res;
    }

    private ILineComparison getLineComparison(Token reco, Token ref, Count count) {
        return new ILineComparison() {
            @Override
            public int getRecoIndex() {
                return reco == null ? -1 : reco.lineIdx;
            }

            @Override
            public int getRefIndex() {
                return ref == null ? -1 : ref.lineIdx;
            }

            @Override
            public String getRefText() {
                return ref == null ? null : ref.value;
            }

            @Override
            public String getRecoText() {
                return reco == null ? null : reco.value;
            }

            @Override
            public List<IPoint> getPath() {
                return Arrays.asList(new Point(
                        count.equals(Count.TP) ?
                                Manipulation.COR :
                                count.equals(Count.FP) ?
                                        Manipulation.DEL :
                                        Manipulation.INS,
                        reco == null ? null : reco.value,
                        ref == null ? null : ref.value));
            }

            @Override
            public String toString() {
                return String.format("[%2d,%2d]: '%s'=>'%s' %s", getRecoIndex(), getRefIndex(), reco == null ? "" : reco.value, ref == null ? "" : ref.value, getPath().get(0)).replace("\n","\\n" );
            }
        };
    }

    @Override
    public void reset() {
        counter.reset();
        substitutionCounter.reset();
    }

    /**
     * returns the absolute and relative frequency of manipulation. If
     * detailed==null or detailed==True, the confusion map is added in before
     * the basic statistic.
     *
     * @return human readable results
     */
    @Override
    public List<String> getResults() {
        LinkedList<String> res = new LinkedList<>();
        if (detailed == null || detailed) {
            List<Pair<String, Long>> result = new LinkedList<>();
            for (Pair<Substitution, Long> pair : substitutionCounter.getResultOccurrence()) {
                result.add(new Pair<>(pair.getFirst().toString(), pair.getSecond()));
            }
            Collections.sort(result, new Comparator<Pair<String, Long>>() {
                @Override
                public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                    return Long.compare(o1.getSecond(), o2.getSecond());
                }
            });
            int maxSize = String.valueOf(result.get(result.size() - 1).getSecond()).length();
            for (Pair<String, Long> pair : result) {
                res.add(String.format("%" + maxSize + "d=\"%s\"", pair.getSecond(), pair.getFirst()));
            }
        }
        Map<Count, Long> map = getCounter().getMap();
//        List<Pair<PathCalculatorExpanded.Manipulation, Long>> resultOccurrence = counter.getResultOccurrence();
        double tp = map.get(Count.TP);
        double fn = map.get(Count.FN);
        double fp = map.get(Count.FP);
        double recall = tp / (tp + fn);
        double precision = tp / (tp + fp);
//        int length = Math.max(Math.max(String.valueOf(fn).length(), String.valueOf(fp).length()), String.valueOf(intersect).length());
        res.add(String.format("%6s =%6.4f%%", "RECALL", recall));
        res.add(String.format("%6s =%6.4f%%", "PRECISION", precision));
        if (tp == 0) {
            res.add(String.format("%6s =%6.4f%%", "F-MEASURE", 0.0));
        } else {
            res.add(String.format("%6s =%6.4f%%", "F-MEASURE", (recall * precision) / (recall + precision) * 2.0));
        }
        res.add(getCounter().toString());
        return res;
    }

    @Override
    public ObjectCounter<Count> getCounter() {
        return counter;
    }

    private String toOneLine(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        if (lines.size() == 1) {
            return lines.get(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            sb.append('\n').append(lines.get(i));
        }
        return sb.toString();
    }

    @Override
    public void calculate(List<String> reco, List<String> ref) {
        calculate(reco, ref, false);
    }

    @Override
    public void calculate(String reco, String ref) {
        calculate(reco, ref, false);
    }

    @Override
    public void calculateWithSegmentation(List<ILine> reco, List<ILine> ref) {
        calculateWithSegmentation(reco, ref, false);
    }

    @Override
    public List<ILineComparison> calculateWithSegmentation(List<ILine> reco, List<ILine> ref, boolean calcLineComparison) {
        AlignmentTask alignmentTask = new AlignmentTask(reco, ref, tokenizer, stringNormalizer, new AdjacentCalculatorBaselines(0.0));
        return calculateIntern(alignmentTask, calcLineComparison);
    }

    @Override
    public List<ILineComparison> calculate(String reco, String ref, boolean calcLineComparison) {
        if (stringNormalizer != null) {
            reco = stringNormalizer.normalize(reco);
            ref = stringNormalizer.normalize(ref);
        }

        //tokenize both strings
        List<String> recoList = tokenizer.tokenize(reco);
        recoList.add(0, "\n");
        recoList.add("\n");
        String[] recos = recoList.toArray(new String[0]);
        List<String> refList = tokenizer.tokenize(ref);
        refList.add(0, "\n");
        refList.add("\n");
        String[] refs = refList.toArray(new String[0]);
        AlignmentTask alignmentTask = new AlignmentTask(recos, refs);
        return calculateIntern(alignmentTask, calcLineComparison);
    }

    @Override
    public List<ILineComparison> calculate(List<String> reco, List<String> ref, boolean calcLineComparison) {
        return calculate(toOneLine(reco), toOneLine(ref), calcLineComparison);
    }

    @Override
    public Map<Metric, Double> getMetrics() {
        Result res = new Result(Method.BOT);
        res.addCounts(counter);
        return res.getMetrics();

    }

    private static class RecoRef {

        private String[] recos;
        private String[] refs;

        public RecoRef(String[] recos, String[] refs) {
            this.recos = recos;
            this.refs = refs;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Arrays.deepHashCode(this.recos);
            hash = 29 * hash + Arrays.deepHashCode(this.refs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RecoRef other = (RecoRef) obj;
            if (!Arrays.deepEquals(this.recos, other.recos)) {
                return false;
            }
            if (!Arrays.deepEquals(this.refs, other.refs)) {
                return false;
            }
            return true;
        }

    }

}
