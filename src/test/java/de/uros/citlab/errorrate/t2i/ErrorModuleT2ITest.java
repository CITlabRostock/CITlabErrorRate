package de.uros.citlab.errorrate.t2i;

import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.types.Metric;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ErrorModuleT2ITest {

    static private Polygon getPoly(int xMin, int xMax, int y) {
        Polygon p = new Polygon();
        p.addPoint(xMin * 50 - 20, y);
        p.addPoint(xMax * 50 + 20, y);
        return p;
    }

    static private ILine getLine(String text, int x, int y) {
        return getLine(text, x, x, y);
    }

    static private ILine getLine(String text, int xMin, int xMax, int y) {
        return new ILine() {
            @Override
            public String getText() {
                return text;
            }

            @Override
            public Polygon getBaseline() {
                return getPoly(xMin, xMax, y);
            }
        };
    }

    private int countCorrect(List<ILineComparison> lines) {
        int i = 0;
        for (ILineComparison line : lines) {
            if (line.getRecoText() != null && !line.getRecoText().isEmpty() && line.getRecoText().equals(line.getRefText())) {
                i++;
            }
        }
        return i;
    }

    @Test
    public void testPrecision() {

        ILine ref1 = getLine("one line", 0, 0);
        ILine ref2 = getLine("second text", 1, 0);
        ILine ref3 = getLine("one lin", 0, 0);
        ILine ref4 = getLine("one line second text", 0, 1, 0);

//        testcase(Arrays.asList(ref1), Arrays.asList(ref1, ref2), 0, "one line".length(), 1);
//        testcase(Arrays.asList(ref3), Arrays.asList(ref3), 0, ref3.getText().length(), 1);
//        testcase(Arrays.asList(ref1), Arrays.asList(ref3), 1, ref3.getText().length(), 0);
//        testcase(Arrays.asList(ref1, ref3), Arrays.asList(ref3), ref1.getText().length(), ref3.getText().length(), 1);
//
//        testcase(Arrays.asList(ref1), Arrays.asList(ref1), 0, ref1.getText().length(), 1);
//        testcase(Arrays.asList(ref1), Arrays.asList(ref1,ref1), 0, ref1.getText().length(), 1);

        testcase(Arrays.asList(ref1, ref2), Arrays.asList(ref4),
                ref1.getText().length() + ref1.getText().length() + 1,
                ref2.getText().length(), 0);
        testcase(Arrays.asList(ref4),Arrays.asList(ref1, ref2),
                ref1.getText().length() + 1,
                ref2.getText().length(), 0);

    }

    private int cntChars(List<ILine> line) {
        int res = 0;
        for (ILine iLine : line) {
            res += iLine.getText().length();
        }
        return res;
    }

    private void testcase(List<ILine> reco, List<ILine> ref, int ld, int cor, int linecor) {
        ErrorModuleT2I impl = new ErrorModuleT2I(false);
        impl.calculateWithSegmentation(reco, ref);
        Map<Metric, Double> metrics = impl.getMetrics();
        System.out.println(metrics);
//        Assert.assertEquals("ref has wrong length", cntChars(ref), counterREC.get(Count.GT));
//        Assert.assertEquals("reco has wrong length", cntChars(reco), counterCER.get(Count.GT));
        Assert.assertEquals("for LD distance is wrong", ld, (int) Math.round(metrics.get(Metric.ERR) * cntChars(reco)));
        Assert.assertEquals("for REC COR is wrong", cor, (int) Math.round(metrics.get(Metric.REC) * cntChars(ref)));
        Assert.assertEquals("for LER COR is wrong", linecor, (int) Math.round(metrics.get(Metric.PREC) * reco.size()));

    }
}