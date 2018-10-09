/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.htr;

import de.uros.citlab.errorrate.interfaces.IErrorModule;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.PathCalculatorGraph;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.tokenizer.TokenizerCategorizer;
import de.uros.citlab.tokenizer.interfaces.ICategorizer;
import eu.transkribus.interfaces.IStringNormalizer;
import eu.transkribus.interfaces.ITokenizer;
import org.apache.commons.math3.util.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Module, which uses the {@link PathCalculatorGraph} to calculate the error
 * rates between tokens. Some other classes are needed, to calculate the error
 * rate. See {@link ITokenizer} and
 * {@link IStringNormalizer} for more details.
 *
 * @author gundram
 */
public class ErrorModuleDynProg implements IErrorModule {
    private final ObjectCounter<Count> counter = new ObjectCounter<>();
    private final ObjectCounter<RecoRef> counterSub = new ObjectCounter<>();
    private final ITokenizer tokenizer;
    private final Boolean detailed;
    private final IStringNormalizer stringNormalizer;

    public ErrorModuleDynProg(ICategorizer categorizer, IStringNormalizer stringNormalizer, Boolean detailed) {
        this( new TokenizerCategorizer(categorizer), stringNormalizer, detailed);
    }

    public ErrorModuleDynProg(ITokenizer tokenizer, IStringNormalizer stringNormalizer, Boolean detailed) {
        this.detailed = detailed;
        if (tokenizer == null) {
            throw new RuntimeException("no tokenizer given (is null)");
        }
        this.tokenizer = tokenizer;
        this.stringNormalizer = stringNormalizer;
    }

    private void calcBestPathFast(String[] recos, String[] refs) {
        final int X = refs.length + 1;
        final int Y = recos.length + 1;
        int[][] costs = new int[Y][X];
        Count[][] predecessor = new Count[Y][X];
        int[] costsStart = costs[0];
        Count[] predecessorStart = predecessor[0];
        predecessorStart[0] = Count.ERR;
        for (int i = 1; i < X; i++) {
            costsStart[i] = i;
            predecessorStart[i] = Count.INS;
        }
        for (int i = 1; i < Y; i++) {
            final int[] costVec = costs[i];
            final int[] costVecB = costs[i - 1];
            final Count[] predecessorVec = predecessor[i];
            costVec[0] = i;
            predecessorVec[0] = Count.DEL;
            String reco = recos[i - 1];
            for (int j = 1; j < X; j++) {
                final int sub = costVecB[j - 1];
                final int del = costVecB[j];
                final int ins = costVec[j - 1];
                if (del < sub) {
                    if (del < ins) {
                        costVec[j] = del + 1;
                        predecessorVec[j] = Count.DEL;
                    } else {
                        costVec[j] = ins + 1;
                        predecessorVec[j] = Count.INS;
                    }
                } else {//sub <= del
                    if (ins < sub) {
                        costVec[j] = ins + 1;
                        predecessorVec[j] = Count.INS;
                    } else { // sub <= ins,
                        if (reco.equals(refs[j - 1])) {
                            costVec[j] = sub;
                            predecessorVec[j] = Count.COR;
                        } else {
                            costVec[j] = sub + 1;
                            predecessorVec[j] = Count.SUB;
                        }
                    }
                }
            }
        }
        LinkedList<PathCalculatorGraph.IDistance<String, String>> res = new LinkedList<>();
        int i = Y - 1;
        int j = X - 1;
        Count manipulation = predecessor[i][j];
        String[] empty = new String[0];
        while (manipulation != Count.ERR) {
            counter.add(manipulation);
            switch (manipulation) {
                case INS:
                    counter.add(Count.ERR);
                    if (detailed == null || detailed == true) {
                        counterSub.add(new RecoRef(empty, new String[]{refs[j - 1]}));
                    }
                    j--;
                    break;
                case DEL:
                    counter.add(Count.ERR);
                    if (detailed == null || detailed == true) {
                        counterSub.add(new RecoRef(new String[]{recos[i - 1]}, empty));
                    }
                    i--;
                    break;
                case SUB:
                    counter.add(Count.ERR);
                    if (detailed == null || detailed == true) {
                        counterSub.add(new RecoRef(new String[]{recos[i - 1]}, new String[]{refs[j - 1]}));
                    }
                    i--;
                    j--;
                    break;
                case COR:
                    if (detailed != null && detailed == true) {
                        counterSub.add(new RecoRef(new String[]{recos[i - 1]}, new String[]{refs[j - 1]}));
                    }
                    i--;
                    j--;
                    break;
            }
            manipulation = predecessor[i][j];
        }
        counter.add(Count.GT, refs.length);
        counter.add(Count.HYP, recos.length);
    }

    /**
     * normalize and tokenize both inputs. Afterwards find the cheapest cost to
     * manipulate the recognition tokens to come to the reference tokens. Count
     * the manipulation which had to be done. If detailed==null or
     * detailed==True, confusion/substitution map is filled.
     *
     * @param reco hypothesis
     * @param ref  reference
     */
    @Override
    public void calculate(String reco, String ref) {
        //use string normalizer, if set
        if (stringNormalizer != null) {
            reco = stringNormalizer.normalize(reco);
            ref = stringNormalizer.normalize(ref);
        }
        //tokenize both strings
        String[] recos = tokenizer.tokenize(reco).toArray(new String[0]);
        String[] refs = tokenizer.tokenize(ref).toArray(new String[0]);
        //use dynamic programming to calculate the cheapest path through the dynamic programming tabular
        calcBestPathFast(recos, refs);
//        List<PathCalculatorGraph.IDistance<String, String>> calcBestPath = pathCalculator.calcBestPath(recos, refs);
    }

    @Override
    public void reset() {
        counter.reset();
        counterSub.reset();
    }

    /**
     * returns the absolute and relative frequency of manipulation. If
     * detailed==null or detailed==True, the confusion map is added in before
     * the basic statistic.
     *
     * @return human readable result
     */
    @Override
    public List<String> getResults() {
        LinkedList<String> res = new LinkedList<>();
        if (detailed == null || detailed) {
            for (Pair<RecoRef, Long> pair : counterSub.getResultOccurrence()) {
                RecoRef first = pair.getFirst();
                String key1;
                switch (first.recos.length) {
                    case 0:
                        key1 = "";
                        break;
                    case 1:
                        key1 = first.recos[0];
                        break;
                    default:
                        key1 = Arrays.toString(first.recos);
                }
                String key2;
                switch (first.refs.length) {
                    case 0:
                        key2 = "";
                        break;
                    case 1:
                        key2 = first.refs[0];
                        break;
                    default:
                        key2 = Arrays.toString(first.refs);
                }
                res.addFirst("[" + key1 + "=>" + key2 + "]=" + pair.getSecond());
            }
        }
        List<Pair<Count, Long>> resultOccurrence = getCounter().getResultOccurrence();
//        long sum = 0;
//        int length = 1;
//        for (Pair<Count, Long> pair : resultOccurrence) {
//            sum += pair.getSecond();
//            length = Math.max(length, pair.getFirst().toString().length());
//        }
//        int length2 = String.valueOf(sum).length();
//        res.add(String.format("%" + length + "s =%6.2f%% ; %" + length2 + "d", "ALL", 100.0, sum));
//        for (Pair<Count, Long> pair : resultOccurrence) {
//            res.add(String.format("%" + length + "s =%6.2f%% ; %" + length2 + "d", pair.toString(), (((double) pair.getSecond()) / sum * 100), pair.getSecond()));
//        }
        res.add(resultOccurrence.toString());
        return res;
    }

    private static class CostCalculatorIntern implements PathCalculatorGraph.ICostCalculator<String, String> {

        private List<String> recos;
        private List<String> refs;
        private PathCalculatorGraph.DistanceMat<String, String> mat;
        private final String manipulation;
        private final String[] emptyList = new String[0];

        public CostCalculatorIntern(String manipulation) {
            this.manipulation = manipulation;
        }

        @Override
        public PathCalculatorGraph.IDistance<String, String> getNeighbour(int[] point) {
            final int y = point[0];
            final int x = point[1];
            switch (manipulation) {
                case "SUB": {
                    int xx = x + 1;
                    int yy = y + 1;
                    if (yy >= recos.size() || xx >= refs.size()) {
                        return null;
                    }
                    final double cost = recos.get(yy).equals(refs.get(xx))?0:1;
                    return new PathCalculatorGraph.Distance<>(cost == 0 ? "COR" : "SUB",
                            cost, mat.get(point).getCostsAcc() + cost,
                            new int[]{yy, xx},
                            point, new String[]{recos.get(yy)},
                            new String[]{refs.get(xx)});
                }
                case "INS": {
                    int xx = x + 1;
                    if (xx >= refs.size()) {
                        return null;
                    }
                    final double cost = 1;
                    return new PathCalculatorGraph.Distance<>("INS",
                            cost, mat.get(point).getCostsAcc() + cost,
                            new int[]{y, xx},
                            point, emptyList,
                            new String[]{refs.get(xx)});
                }
                case "DEL": {
                    int yy = y + 1;
                    if (yy >= recos.size()) {
                        return null;
                    }
                    final double cost = 1;
                    return new PathCalculatorGraph.Distance<>("DEL",
                            cost, mat.get(point).getCostsAcc() + cost,
                            new int[]{yy, x},
                            point, new String[]{recos.get(yy)},
                            emptyList);
                }
                default:
                    throw new RuntimeException("not expected manipulation " + manipulation);
            }
        }

        @Override
        public void init(PathCalculatorGraph.DistanceMat<String, String> mat, List<String> recos, List<String> refs) {
            this.mat = mat;
            this.recos = recos;
            this.refs = refs;
        }

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
        if (reco.size() == ref.size()) {
            for (int i = 0; i < ref.size(); i++) {
                calculate(reco.get(i), ref.get(i));
            }
        } else {
            calculate(toOneLine(reco), toOneLine(ref));
        }
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
