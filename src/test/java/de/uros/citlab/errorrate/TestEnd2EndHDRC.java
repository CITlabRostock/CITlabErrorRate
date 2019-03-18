/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate;

import de.uros.citlab.errorrate.htr.end2end.ErrorModuleEnd2End;
import de.uros.citlab.errorrate.interfaces.ILine;
import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.types.Count;
import de.uros.citlab.errorrate.types.Metric;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.errorrate.util.XmlExtractor;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
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
public class TestEnd2EndHDRC {

    private static File XML_GT = new File("src/test/resources/end2end/ICDAR-2019-HDRC/page/004533645_00059.xml");
    private static File XML_HYP = new File("src/test/resources/end2end/ICDAR-2019-HDRC/page/004533645_00059_changed.xml");

    @Test
    public void testExample() {
        List<XmlExtractor.Line> linesFromGt = XmlExtractor.getLinesFromFile(XML_GT);
        List<XmlExtractor.Line> linesFromHyp = XmlExtractor.getLinesFromFile(XML_HYP);
        boolean[] trueFalse = new boolean[]{true, false};
        for (boolean restrictReadingOrder : trueFalse) {
            for (boolean restirctGeometry : trueFalse) {
                System.out.println("restrictReadingOrder = " + restrictReadingOrder + " restirctGeometry = " + restirctGeometry);
                ErrorModuleEnd2End impl = new ErrorModuleEnd2End(restrictReadingOrder, restirctGeometry, false, false);
                run(impl, linesFromGt, linesFromGt);
                Map<Metric, Double> run = impl.getMetrics();
                Assert.assertEquals(0.0, run.get(Metric.ERR), 0.001);
                Assert.assertEquals(1.0, run.get(Metric.ACC), 0.001);
                Assert.assertEquals(1.0, run.get(Metric.F), 0.001);
                impl.reset();
//                impl.setFileDynProg(new File("restrictReadingOrder_" + restrictReadingOrder + "_restirctGeometry_" + restirctGeometry + ".png"));
//                impl.setSizeProcessViewer(1000);
                run(impl, linesFromGt, linesFromHyp);
                ObjectCounter<Count> counter = impl.getCounter();
                System.out.println(counter);
                System.out.println("ACC = " + (1 - impl.getMetrics().get(Metric.ERR)));
                System.out.println("-----------------------------------------------------");
            }
        }

    }

    public void run(ErrorModuleEnd2End impl, List<XmlExtractor.Line> linesFromGt, List<XmlExtractor.Line> linesFromHyp) {
        impl.setGeometryComparison(ErrorModuleEnd2End.GeometryComaprison.COORDS);
        List<ILineComparison> iLineComparisons = impl.calculateWithSegmentation(getLines(linesFromHyp), getLines(linesFromGt), true);
//        for (ILineComparison iLineComparison : iLineComparisons) {
//            System.out.println(iLineComparison);
//        }
        Map<Metric, Double> metrics = impl.getMetrics();
//        for (Metric metric : metrics.keySet()) {
//            System.out.println(metric + " => " + metrics.get(metric));
//        }
    }

    public List<ILine> getLines(List<XmlExtractor.Line> lines) {
        List<ILine> res = new LinkedList<>();
        for (XmlExtractor.Line line : lines) {
            res.add(new ILine() {
                @Override
                public String getText() {
                    return line.textEquiv;
                }

                @Override
                public Polygon getBaseline() {
                    return line.coords;
                }
            });
        }
        return res;
    }

}
