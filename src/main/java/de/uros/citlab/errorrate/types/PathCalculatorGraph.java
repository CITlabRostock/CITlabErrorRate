/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.util.HeatMapUtil;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * @param <Reco>
 * @param <Reference>
 * @author gundram
 */
public class PathCalculatorGraph<Reco, Reference> {

    private UpdateScheme updateScheme = UpdateScheme.LAZY;
    private boolean useProgressBar = false;
    private static final Logger LOG = LoggerFactory.getLogger(PathCalculatorGraph.class);
    private final List<ICostCalculator<Reco, Reference>> costCalculators = new ArrayList<>();
    private final List<ICostCalculatorMulti<Reco, Reference>> costCalculatorsMutli = new ArrayList<>();
    private PathFilter<Reco, Reference> filter = null;
    private final Comparator<IDistance<Reco, Reference>> cmpCostsAcc = new Comparator<IDistance<Reco, Reference>>() {
        @Override
        public int compare(IDistance<Reco, Reference> o1, IDistance<Reco, Reference> o2) {
            int d = o1.compareTo(o2);
            if (d != 0) {
                return d;
            }
            if (o1 == o2) {
                return 0;
            }
            int d2 = Integer.compare(o2.getPoint()[0], o1.getPoint()[0]);
            if (d2 != 0) {
                return d2;
            }
            return Integer.compare(o2.getPoint()[1], o1.getPoint()[1]);
        }
    };

    public void resetCostCalculators() {
        costCalculators.clear();
        costCalculatorsMutli.clear();
    }

    public interface ICostCalculator<Reco, Reference> {

        void init(DistanceMat<Reco, Reference> mat, List<Reco> recos, List<Reference> refs);

        IDistance<Reco, Reference> getNeighbour(int[] point, IDistance<Reco, Reference> dist);

    }

    public interface ICostCalculatorMulti<Reco, Reference> {

        void init(DistanceMat<Reco, Reference> mat, List<Reco> recos, List<Reference> refs);

        List<IDistance<Reco, Reference>> getNeighbours(int[] point, IDistance<Reco, Reference> dist);

    }

    public static class CostCalculatorTranspose<Reco, Reference> implements ICostCalculator<Reco, Reference> {

        private final ICostCalculator<Reference, Reco> cc;
        private DistanceMat<Reference, Reco> mat;

        public CostCalculatorTranspose(ICostCalculator<Reference, Reco> cc) {
            this.cc = cc;
        }

        @Override
        public void init(DistanceMat<Reco, Reference> mat, List<Reco> recos, List<Reference> refs) {
            this.mat = new DistanceMatTranspose(mat);
            cc.init(this.mat, refs, recos);
        }

        private int[] transpose(int[] point) {
            return new int[]{point[1], point[0]};
        }

        @Override
        public IDistance<Reco, Reference> getNeighbour(int[] point, IDistance<Reco, Reference> dist) {
            int[] transpose = transpose(point);
            IDistance<Reference, Reco> distance = cc.getNeighbour(transpose, mat.get(transpose));
            return new Distance<>(distance.getManipulation(), distance.getCosts(), distance.getCostsAcc(), transpose(distance.getPoint()), distance.getPointPrevious(), distance.getReferences(), distance.getRecos());
        }

        private class DistanceMatTranspose<Reco, Reference> extends DistanceMat<Reco, Reference> {

            private final DistanceMat<Reco, Reference> matInner;

            public DistanceMatTranspose(DistanceMat<Reco, Reference> mat) {
                super(0, 0);
                matInner = mat;
            }

            @Override
            public int getSizeX() {
                return matInner.getSizeY();
            }

            @Override
            public int getSizeY() {
                return matInner.getSizeX();
            }

            @Override
            public IDistance get(int y, int x) {
                return matInner.get(x, y);
            }

            @Override
            public void set(int y, int x, IDistance distance) {
                matInner.set(x, y, distance);
            }

            @Override
            public List<IDistance<Reco, Reference>> getBestPath() {
                return matInner.getBestPath();
            }

            @Override
            public IDistance<Reco, Reference> getLastElement() {
                return matInner.getLastElement();
            }

            @Override
            public String toString() {
                return matInner.toString();
            }

            @Override
            public int hashCode() {
                return matInner.hashCode();
            }

        }

    }

    public void useProgressBar(boolean useProgressBar) {
        this.useProgressBar = useProgressBar;
    }

    public void addCostCalculator(ICostCalculator<Reco, Reference> costCalculator) {
        costCalculators.add(costCalculator);
    }

    public void addCostCalculator(ICostCalculatorMulti<Reco, Reference> costCalculator) {
        costCalculatorsMutli.add(costCalculator);
    }

    public void addCostCalculatorTransposed(ICostCalculator<Reference, Reco> costCalculator) {
        costCalculators.add(new CostCalculatorTranspose<>(costCalculator));
    }

    public void setFilter(PathFilter<Reco, Reference> filter) {
        this.filter = filter;
        if (filter != null) {
            filter.init(cmpCostsAcc);
        }

    }

    //    public static enum Manipulation {
//
//        INS, DEL, SUB, COR, SPECIAL;
//    }
    public void setUpdateScheme(UpdateScheme updateScheme) {
        this.updateScheme = updateScheme;
    }

    public static enum UpdateScheme {
        LAZY, ALL;
    }

    public static interface PathFilter<Reco, Reference> {

        public void init(Comparator<IDistance<Reco, Reference>> comparator);

        public boolean addDistance(IDistance<Reco, Reference> newDistance);

        public boolean followDistance(IDistance<Reco, Reference> bestDistance);
    }

    public static interface IDistance<Reco, Reference> extends Comparable<IDistance<Reco, Reference>> {

        public double getCosts();

        public double getCostsAcc();

        public Reco[] getRecos();

        public Reference[] getReferences();

        public String getManipulation();

        public int[] getPointPrevious();

        public int[] getPoint();

        public boolean equals(IDistance<Reco, Reference> obj);

        public boolean isMarked();

        public boolean mark(boolean mark);

        public void dispose();

    }

    public static class DistanceMat<Reco, Reference> {

        private final TIntObjectHashMap distMap;
        private final int sizeY;
        private final int sizeX;
//        boolean doublestruct = true;

        public DistanceMat(int y, int x) {
            this.distMap = new TIntObjectHashMap();
            sizeY = y;
            sizeX = x;
        }

        public IDistance<Reco, Reference> get(int y, int x) {
//            if (doublestruct) {
//                Object o = distMap.get(y);
//                if (o == null) {
//                    return null;
//                }
//                return (IDistance<Reco, Reference>) ((TIntObjectHashMap) o).get(x);
//            }
            return (IDistance<Reco, Reference>) distMap.get(y * sizeX + x);
        }

        public IDistance<Reco, Reference> get(int[] pos) {
            return get(pos[0], pos[1]);
        }

        public void set(int y, int x, IDistance<Reco, Reference> distance) {
//            if (doublestruct) {
//                Object o = distMap.get(y);
//                if (o == null) {
//                    o = new TIntObjectHashMap();
//                    distMap.put(y, o);
//                }
//                ((TIntObjectHashMap) o).put(x, distance);
//                return;
//            }
            distMap.put(y * sizeX + x, distance);
        }

        public void set(int[] position, IDistance<Reco, Reference> distance) {
            set(position[0], position[1], distance);
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeX() {
            return sizeX;
        }

        public IDistance<Reco, Reference> getLastElement() {
            return get(getSizeY() - 1, getSizeX() - 1);
        }

        public List<IDistance<Reco, Reference>> getBestPath() {
            IDistance<Reco, Reference> lastElement = getLastElement();
            if (lastElement == null) {
                LOG.warn("Distance Matrix not completely calculated.");
                return null;
            }
            LinkedList<IDistance<Reco, Reference>> res = new LinkedList<>();
            res.add(lastElement);
            int[] pos = lastElement.getPointPrevious();
            while (pos != null) {
                lastElement = get(pos[0], pos[1]);
                res.addFirst(lastElement);
                pos = lastElement.getPointPrevious();
            }
            res.removeFirst();
            return res;
        }

        private int getElements() {
//            int cnt = 0;
//            if (doublestruct) {
//                for (Object value : distMap.getValues()) {
//                    if (value != null) {
//                        cnt += ((TIntObjectHashMap) value).size();
//                    }
//                }
//                return cnt;
//            }
            return distMap.size();
        }

        public DistanceMat<Reco, Reference> cleanup(TreeSet<IDistance<Reco, Reference>> queue) {
            int cnt = 0;
            for (IDistance<Reco, Reference> tail : queue) {
                cnt += mark(tail, true);
            }
            int size = getElements();
            DistanceMat<Reco, Reference> res = new DistanceMat<Reco, Reference>(getSizeY(), getSizeX());
//            if (doublestruct) {
//                distMap.forEachEntry(new TIntObjectProcedure() {
//                    @Override
//                    public boolean execute(int i, Object o) {
//                        if (o != null) {
//                            TIntObjectHashMap o1 = (TIntObjectHashMap) o;
//                            return o1.forEachEntry(new TIntObjectProcedure() {
//                                @Override
//                                public boolean execute(int j, Object o) {
//                                    IDistance<Reco, Reference> o2 = (IDistance<Reco, Reference>) o;
//                                    if (o2 == null) {
//                                        return true;
//                                    }
//                                    if (o2.isMarked()) {
//                                        res.set(i, j, o2);
//                                        o2.mark(false);
//                                    } else {
//                                        o2.dispose();
//                                    }
//                                    return true;
//                                }
//                            });
//                        }
//                        return true;
//                    }
//                });
//            } else {
            distMap.forEachEntry(new TIntObjectProcedure() {
                @Override
                public boolean execute(int i, Object o) {
                    IDistance<Reco, Reference> o1 = (IDistance<Reco, Reference>) o;
                    if (o1 == null) {
                        return true;
                    }
                    if (o1.isMarked()) {
                        res.set(o1.getPoint()[0], o1.getPoint()[1], o1);
                        o1.mark(false);
                    } else {
                        o1.dispose();
                    }
                    return true;
                }
            });
//            }
            LOG.debug("start point still in matrix = {}", res.get(0, 0) != null);
            int size2 = res.getElements();
            LOG.debug("#Edges before: {} now: {}", size, size2);
            return res;
        }

        private int mark(IDistance<Reco, Reference> dist, boolean mark) {
            if (mark == dist.isMarked()) {
                return 0;
            }
            dist.mark(mark);
            int[] pointPrevious = dist.getPointPrevious();
            if (pointPrevious == null) {
                return 1;
            }
            return 1 + mark(get(pointPrevious), mark);
        }


    }

    private int handleDistance(IDistance<Reco, Reference> distNew, DistanceMat<Reco, Reference> distMat, TreeSet<IDistance<Reco, Reference>> QSortedCostAcc, PathFilter<Reco, Reference> filter) {
        if (distNew == null) {
            return 0;
        }
        int cnt = 0;
        final int[] posNew = distNew.getPoint();
        IDistance<Reco, Reference> distOld = distMat.get(posNew);
        boolean addDistance = filter == null || filter.addDistance(distNew);
        if (addDistance) {
            cnt++;
        }
        if (distOld == null) {
            if (addDistance) {
                distMat.set(posNew, distNew);
                int size = QSortedCostAcc.size();
                QSortedCostAcc.add(distNew);
                if (QSortedCostAcc.size() == size) {
                    throw new RuntimeException("error in using tree");
                }
            }
        } else if (cmpCostsAcc.compare(distNew, distOld) < 0) {
            if (addDistance) {
                distMat.set(posNew, distNew);
                QSortedCostAcc.remove(distOld);
                QSortedCostAcc.add(distNew);
            } else {
                distMat.set(posNew, null);
                QSortedCostAcc.remove(distOld);
            }

        }
        if (LOG.isTraceEnabled()) {
            if (distOld == null) {
                LOG.trace("calculate at " + posNew[0] + ";" + posNew[1] + " distance " + distNew);
            } else if (cmpCostsAcc.compare(distNew, distOld) < 0) {
                LOG.trace("calculate at " + posNew[0] + ";" + posNew[1] + " distance " + distNew);
            } else {
                LOG.trace("calculate at " + posNew[0] + ";" + posNew[1] + " distance " + distOld);
            }
        }
        return cnt;
    }

    public DistanceMat<Reco, Reference> calcDynProg(List<Reco> reco, List<Reference> ref) {
        if (ref == null || reco == null) {
            throw new RuntimeException("target or output is null");
        }
        boolean deleteFirstReco = false;
        if (reco.isEmpty() || reco.get(0) != null) {
            try {
                reco.add(0, null);
            } catch (UnsupportedOperationException ex) {
                reco = new LinkedList<>(reco);
                reco.add(0, null);
            }
            deleteFirstReco = true;
        }
        boolean deleteFirstRef = false;
        if (ref.isEmpty() || ref.get(0) != null) {
            try {
                ref.add(0, null);
            } catch (UnsupportedOperationException ex) {
                ref = new LinkedList<>(ref);
                ref.add(0, null);
            }
            deleteFirstRef = true;
        }
        int recoLength = reco.size();
        int refLength = ref.size();
        DistanceMat<Reco, Reference> distMat = new DistanceMat<>(recoLength, refLength);
//        IDistance<Reco, Reference> distanceInfinity = new Distance<>(null, null, 0, Double.MAX_VALUE, null);
//        LinkedList<IDistance<Reco, Reference>> candidates = new LinkedList<>();
        for (ICostCalculator<Reco, Reference> costCalculator : costCalculators) {
            costCalculator.init(distMat, reco, ref);
        }
        for (ICostCalculatorMulti<Reco, Reference> costCalculator : costCalculatorsMutli) {
            costCalculator.init(distMat, reco, ref);
        }
        int cntEdges = 0;
        TreeSet<IDistance<Reco, Reference>> QSortedCostAcc = new TreeSet<>(cmpCostsAcc);
//        HashSet<IDistance<Reco, Reference>> G = new LinkedHashSet<>();
        int[] startPoint = new int[]{0, 0};
        Distance<Reco, Reference> start = new Distance(null, 0, 0, startPoint, null, null, null);
        distMat.set(startPoint, start);
        QSortedCostAcc.add(start);
//        G.add(start);
//        distMat.set(startPoint, start);
        int sizeOutput = 1600;
        ProcessField bar = true ? new ProcessField("calculating Dynamic Matrix", sizeOutput) : null;
        int factorNextCleanup = 10000;
        int boundNextCleanup = factorNextCleanup;
        int cntVerticies = 0;
        int cntVerticiesSkip = 0;
        boolean[][] isdead = new boolean[distMat.sizeY][distMat.sizeX];
        while (!QSortedCostAcc.isEmpty()) {
            IDistance<Reco, Reference> distActual = QSortedCostAcc.pollFirst();
            if (updateScheme.equals(UpdateScheme.LAZY) && distMat.getLastElement() == distActual) {
                break;
            }
            if (isdead[distActual.getPoint()[0]][distActual.getPoint()[1]]) {
//                System.out.println("vertical skip:" + cntVerticiesSkip++);
                continue;
            }
            isdead[distActual.getPoint()[0]][distActual.getPoint()[1]] = true;
            if (filter != null && !filter.followDistance(distActual)) {
                continue;
            }
            final int[] pos = distActual.getPoint();
            if (bar != null) {
                bar.update(pos, distMat, distActual);
            }
            cntVerticies++;
            //all Neighbours v of u
            for (ICostCalculator<Reco, Reference> costCalculator : costCalculators) {
                IDistance<Reco, Reference> distance = costCalculator.getNeighbour(pos, distActual);
                cntEdges += handleDistance(distance, distMat, QSortedCostAcc, filter);
            }
            for (ICostCalculatorMulti<Reco, Reference> costCalculator : costCalculatorsMutli) {
                List<IDistance<Reco, Reference>> distances = costCalculator.getNeighbours(pos, distActual);
                if (distances == null) {
                    continue;
                }
                for (IDistance<Reco, Reference> distance : distances) {
                    cntEdges += handleDistance(distance, distMat, QSortedCostAcc, filter);
                }
            }
            if (cntVerticies > boundNextCleanup) {
                distMat = distMat.cleanup(QSortedCostAcc);
                boundNextCleanup = cntVerticies + factorNextCleanup;
                factorNextCleanup *= 1.5;
            }

        }
        if (bar != null) {
            bar.setEnd();
            BufferedImage image = bar.getImage();
            try {
                ImageIO.write(image, "png", new File("out.png"));
            } catch (Throwable e) {
                LOG.debug("cannot save debug image", e);

            }
        }
        if (distMat.getLastElement() == null) {
            LOG.warn("no path found from start to end with given cost calulators");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("caculate " + cntEdges + " edges for " + ((reco.size() - 1) * (ref.size() - 1)) + "/" + cntVerticies + " verticies");
        }
        if (deleteFirstReco) {
            reco.remove(0);
        }
        if (deleteFirstRef) {
            ref.remove(0);
        }
        if (bar != null) {
            bar.dispose();
        }
        return distMat;

    }

    public List<IDistance<Reco, Reference>> calcBestPath(Reco[] reco, Reference[] ref) {
        return calcBestPath(Arrays.asList(reco), Arrays.asList(ref));
    }

    public List<IDistance<Reco, Reference>> calcBestPath(List<Reco> reco, List<Reference> ref) {
        return calcDynProg(reco, ref).getBestPath();
    }

    public List<IDistance<Reco, Reference>> calcBestPath(DistanceMat<Reco, Reference> distMat) {
        return distMat.getBestPath();
    }

    public double calcCosts(List<Reco> reco, List<Reference> ref) {
        return calcDynProg(reco, ref).getLastElement().getCostsAcc();
    }

    public double calcCosts(Reco[] reco, Reference[] ref) {
        return calcDynProg(Arrays.asList(reco), Arrays.asList(ref)).getLastElement().getCostsAcc();
    }

    public static class Distance<Reco, Reference> implements PathCalculatorGraph.IDistance<Reco, Reference> {

        //        private final Distance previousDistance;
        private final String manipulation;
        private final double costs;
        private final double costsAcc;
        private int[] previous;
        private int[] point;
        private Reco[] recos;
        private Reference[] references;
        private boolean marked = false;

        public Distance(String manipulation, double costs, double costAcc, int[] point, int[] previous, Reco[] recos, Reference[] references) {
            this.manipulation = manipulation;
            this.costs = costs;
            this.previous = previous;
            this.costsAcc = costAcc;
            this.recos = recos;
            this.references = references;
            this.point = point;
        }

        @Override
        public boolean isMarked() {
            return marked;
        }

        @Override
        public boolean mark(boolean mark) {
            this.marked = mark;
            return mark;
        }

        @Override
        public void dispose() {
            if (!isMarked()) {
                previous = null;
                point = null;
                recos = null;
                references = null;
            }
        }

        @Override
        public double getCosts() {
            return costs;
        }

        @Override
        public double getCostsAcc() {
            return costsAcc;
        }

        @Override
        public Reference[] getReferences() {
            return references;
        }

        @Override
        public Reco[] getRecos() {
            return recos;
        }

        @Override
        public String getManipulation() {
            return manipulation;
        }

        @Override
        public int[] getPoint() {
            return point;
        }

        @Override
        public int[] getPointPrevious() {
            return previous;
        }

        @Override
        public String toString() {
            return "cost=" + costs + ";manipulation=" + manipulation + ";costAcc=" + costsAcc + ";" + Arrays.deepToString(recos) + ";" + Arrays.deepToString(references);
        }

        //        @Override
//        public int compareTo(IDistance<Reco, Reference> o) {
//            return Double.compare(getCostsAcc(), o.getCostsAcc());
//        }
        @Override
        public boolean equals(IDistance<Reco, Reference> obj) {
            return obj == this;
        }

        @Override
        public int compareTo(IDistance<Reco, Reference> o) {
            return Double.compare(costsAcc, o.getCostsAcc());
        }
    }

    private static class ProcessField {
        private JFrame mainFrame;
        private JProgressBar progressBar;
        private JLabel image;
        private int size;
        private boolean isDebug = false;
        private double[][] mat = null;
        private final int steps = 100;

        public ProcessField(String title, int size) {
            this.size = size;
            mainFrame = new JFrame();
//        meinJFrame.setSize(size, size + 300);
            mainFrame.setTitle(title);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            JPanel meinPanel = new JPanel();
            meinPanel.setLayout(new BoxLayout(meinPanel, BoxLayout.Y_AXIS));

            // JProgressBar-Objekt wird erzeugt
            progressBar = new JProgressBar(0, steps);

            // Wert für den Ladebalken wird gesetzt
            progressBar.setValue(0);
            // Der aktuelle Wert wird als
            // Text in Prozent angezeigt
            progressBar.setStringPainted(true);

            image = new JLabel();
            image.setIcon(new ImageIcon(HeatMapUtil.getHeatMap(new double[size][size], 2)));
            image.setAlignmentX(Component.CENTER_ALIGNMENT);
            progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
            meinPanel.add(image);
            // JProgressBar wird Panel hinzugefügt
            meinPanel.add(progressBar);
            mainFrame.add(meinPanel);
            mainFrame.setLocation(new Point((int) (screenSize.getWidth() - mainFrame.getWidth()) / 2, (int) (screenSize.getHeight() - mainFrame.getHeight()) / 2));
            mainFrame.setVisible(true);
            mainFrame.pack();
        }

        private void setEnd() {
            progressBar.setValue(steps);
        }

        private void dispose() {
            mainFrame.setVisible(false);
            mainFrame.dispose();

        }

        void update(int[] pos, DistanceMat distMat, IDistance actual) {
            int sizeX = isDebug ? size : distMat.getSizeX();
            int sizeY = isDebug ? size : distMat.getSizeY();
            int newProcess = (int) Math.round(((double) pos[1] / sizeX * steps));
            if (progressBar.getValue() < newProcess || isDebug) {
                progressBar.setValue(newProcess);
                int factorX = Math.max(1, sizeX / size);
                int factorY = Math.max(1, sizeY / size);
                if (mat == null) {
                    mat = new double[sizeY / factorY][sizeX / factorX];
                }
                for (int i = 0; i < mat.length; i++) {
                    double[] vec = mat[i];
                    for (int j = 0; j < vec.length; j++) {
                        IDistance dist = distMat.get(i * factorY, j * factorX);
                        if (dist == null) {
                            vec[j] = vec[j] > 0 ? vec[j] : isDebug ? -5 : 0;
                        } else {
                            vec[j] = vec[j] > 0 ? Math.min(dist.getCostsAcc(), vec[j]) : dist.getCostsAcc();
                        }
//                        vec[j] = dist == null ? isDebug ? -5 : 0 : dist.getCostsAcc();
                    }
                }
                BufferedImage heatMap = HeatMapUtil.getHeatMap(mat, 2);
                image.setIcon(new ImageIcon(heatMap));
                mainFrame.invalidate();
                mainFrame.revalidate();
                mainFrame.repaint();
//                System.out.println("done");
            }

        }

        public BufferedImage getImage() {
            return HeatMapUtil.getHeatMap(mat, 7);
        }

    }

}
