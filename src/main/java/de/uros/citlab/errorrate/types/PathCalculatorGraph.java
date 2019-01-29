/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.util.HeatMapUtil;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

/**
 * @param <Reco>
 * @param <Reference>
 * @author gundram
 */
public class PathCalculatorGraph<Reco, Reference> {

    private Reco typeReco;
    private Reference typeReference;
    private UpdateScheme updateScheme = UpdateScheme.LAZY;
    private int sizeProcessViewer = -1;
    private File folderDynMats = null;
    private boolean show = true;
    private static final Logger LOG = LoggerFactory.getLogger(PathCalculatorGraph.class);
    private final List<ICostCalculator<Reco, Reference>> costCalculators = new ArrayList<>();
    private final List<ICostCalculatorMulti<Reco, Reference>> costCalculatorsMutli = new ArrayList<>();
    private PathFilter<Reco, Reference> filter = null;
    private final Comparator<DistanceSmall> cmpCostsAcc = new Comparator<DistanceSmall>() {
        @Override
        public int compare(DistanceSmall o1, DistanceSmall o2) {
            int d = Double.compare(o1.costsAcc, o2.costsAcc);
            if (d != 0) {
                return d;
            }
            if (o1 == o2) {
                return 0;
            }
            int d2 = Integer.compare(o2.point[0], o1.point[0]);
            if (d2 != 0) {
                return d2;
            }
            return Integer.compare(o2.point[1], o1.point[1]);
        }
    };

    public void resetCostCalculators() {
        costCalculators.clear();
        costCalculatorsMutli.clear();
    }

    public interface ICostCalculator<Reco, Reference> {

        void init(DistanceMat<Reco, Reference> mat, Reco[] recos, Reference[] refs);

        DistanceSmall getNeighbourSmall(int[] point, DistanceSmall dist);

        IDistance<Reco, Reference> getNeighbour(DistanceSmall dist);

    }

    public interface ICostCalculatorMulti<Reco, Reference> {

        void init(DistanceMat<Reco, Reference> mat, Reco[] recos, Reference[] refs);

        List<DistanceSmall> getNeighboursSmall(int[] point, DistanceSmall dist);

        IDistance<Reco, Reference> getNeighbour(DistanceSmall dist);

    }


    public void addCostCalculator(ICostCalculator<Reco, Reference> costCalculator) {
        costCalculators.add(costCalculator);
    }

    public void addCostCalculator(ICostCalculatorMulti<Reco, Reference> costCalculator) {
        costCalculatorsMutli.add(costCalculator);
    }

    public void setFileDynMat(File folderDynMats) {
        this.folderDynMats = folderDynMats;
    }

    public void setSizeProcessViewer(int useProgressBar) {
        this.sizeProcessViewer = useProgressBar;
    }

    public void setShowDynMat(boolean show) {
        this.show = show;
    }

    public void setFilter(PathFilter<Reco, Reference> filter) {
        this.filter = filter;
    }

    public void setUpdateScheme(UpdateScheme updateScheme) {
        this.updateScheme = updateScheme;
    }

    public static enum UpdateScheme {
        LAZY, ALL;
    }

    public interface PathFilter<Reco, Reference> {

        void init(Reco[] recos, Reference[] references);

        boolean addNewEdge(DistanceSmall newDistance);

        boolean followPathsFromBestEdge(DistanceSmall bestDistance);
    }

    public static class DistanceSmall {
        public int[] pointPrevious;
        public int[] point;
        public final double costsAcc;
        public boolean marked = false;
        public Object costCalculator;

        public DistanceSmall(int[] pointPrevious, int[] point, double costsAcc, Object costCalculator) {
            this.pointPrevious = pointPrevious;
            this.point = point;
            this.costsAcc = costsAcc;
            this.costCalculator = costCalculator;
        }

        public void dispose() {
            point = null;
            pointPrevious = null;
            costCalculator = null;
        }

        public IDistance getLargeDistance() {
            if (costCalculator == null) {
                return new Distance<>(null, costsAcc, costsAcc, point, pointPrevious, null, null);
            }
            if (costCalculator instanceof ICostCalculator) {
                return ((ICostCalculator) costCalculator).getNeighbour(this);
            }
            if (costCalculator instanceof ICostCalculatorMulti) {
                return ((ICostCalculatorMulti) costCalculator).getNeighbour(this);
            }
            throw new RuntimeException("cannot interprete class " + costCalculator.getClass().getName());
        }

        @Override
        public String toString() {
            return "DistanceSmall{" +
                    " pos=" + Arrays.toString(point) +
                    ", prev=" + Arrays.toString(pointPrevious) +
                    ", costs=" + costsAcc +
//                    ", marked=" + marked +
                    ", CC=" + costCalculator +
                    '}';
        }
    }


    public interface IDistance<Reco, Reference> {
        double getCosts();

        Reco[] getRecos();

        Reference[] getReferences();

        String getManipulation();

        int[] getPointPrevious();

        int[] getPoint();

        double getCostsAcc();
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

        public DistanceSmall get(int y, int x) {
//            if (doublestruct) {
//                Object o = distMap.get(y);
//                if (o == null) {
//                    return null;
//                }
//                return (IDistance<Reco, Reference>) ((TIntObjectHashMap) o).get(x);
//            }
            return (DistanceSmall) distMap.get(y * sizeX + x);
        }

        public DistanceSmall get(int[] pos) {
            return get(pos[0], pos[1]);
        }

        public void set(int y, int x, DistanceSmall distance) {
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

        public void set(int[] position, DistanceSmall distance) {
            set(position[0], position[1], distance);
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeX() {
            return sizeX;
        }

        public DistanceSmall getLastElement() {
            return get(getSizeY() - 1, getSizeX() - 1);
        }

        public List<IDistance<Reco, Reference>> getBestPath() {
            DistanceSmall lastElement = getLastElement();
            if (lastElement == null) {
                LOG.warn("Distance Matrix not completely calculated.");
                return null;
            }
            LinkedList<IDistance<Reco, Reference>> res = new LinkedList<>();
            res.add(lastElement.getLargeDistance());
            int[] pos = lastElement.pointPrevious;
            while (pos != null) {
                lastElement = get(pos[0], pos[1]);
                res.addFirst(lastElement.getLargeDistance());
                pos = lastElement.pointPrevious;
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

        public DistanceMat<Reco, Reference> cleanup(TreeSet<DistanceSmall> queue) {
            int cnt = 0;
            for (DistanceSmall tail : queue) {
                cnt += mark(tail, true);
            }
            int size = getElements();
            DistanceMat<Reco, Reference> res = new DistanceMat<Reco, Reference>(getSizeY(), getSizeX());
            distMap.forEachEntry(new TIntObjectProcedure() {
                @Override
                public boolean execute(int i, Object o) {
                    DistanceSmall o1 = (DistanceSmall) o;
                    if (o1 == null) {
                        return true;
                    }
                    if (o1.marked) {
                        res.set(o1.point[0], o1.point[1], o1);
                        o1.marked = false;
                    } else {
                        o1.dispose();
                    }
                    return true;
                }
            });
//            }
            int size2 = res.getElements();
            LOG.debug("#Edges before: {} now: {}", size, size2);
            return res;
        }

        private int mark(DistanceSmall dist, boolean mark) {
            if (mark == dist.marked) {
                return 0;
            }
            dist.marked = mark;
            int[] pointPrevious = dist.pointPrevious;
            if (pointPrevious == null) {
                return 1;
            }
            return 1 + mark(get(pointPrevious), mark);
        }


    }

    private int handleDistance(DistanceSmall distNew, DistanceMat<Reco, Reference> distMat, TreeSet<DistanceSmall> QSortedCostAcc) {
        if (distNew == null) {
            return 0;
        }
        int cnt = 0;
        final int[] posNew = distNew.point;
        DistanceSmall distOld = distMat.get(posNew);
        StopWatch.start("Filter");
        boolean addDistance = filter == null || filter.addNewEdge(distNew);
        StopWatch.stop("Filter");
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
        return calcDynProg(reco, ref, -1);
    }

    public DistanceMat<Reco, Reference> calcDynProg(List<Reco> reco, List<Reference> ref, int maxCount) {
        if (ref == null || reco == null) {
            throw new RuntimeException("target or output is null");
        }
        int recoOffset = (reco.isEmpty() || reco.get(0) != null) ? 1 : 0;
        int refOffset = (ref.isEmpty() || ref.get(0) != null) ? 1 : 0;
//        int recoLength = reco.size();
//        int refLength = ref.size();
        Reco[] nativeReco = (Reco[]) Array.newInstance(reco.get(0).getClass(), reco.size() + recoOffset);
        for (int i = 0; i < reco.size(); i++) {
            nativeReco[i + recoOffset] = reco.get(i);
        }
        Reference[] nativeRef = (Reference[]) Array.newInstance(ref.get(0).getClass(), ref.size() + refOffset);
        for (int i = 0; i < ref.size(); i++) {
            nativeRef[i + refOffset] = ref.get(i);
        }
//        Reference[] nativeRef = (Reference[]) reco.toArray();
        DistanceMat<Reco, Reference> distMat = new DistanceMat<>(nativeReco.length, nativeRef.length);
//        IDistance<Reco, Reference> distanceInfinity = new Distance<>(null, null, 0, Double.MAX_VALUE, null);
//        LinkedList<IDistance<Reco, Reference>> candidates = new LinkedList<>();
        for (ICostCalculator<Reco, Reference> costCalculator : costCalculators) {
            costCalculator.init(distMat, nativeReco, nativeRef);
        }
        for (ICostCalculatorMulti<Reco, Reference> costCalculator : costCalculatorsMutli) {
            costCalculator.init(distMat, nativeReco, nativeRef);
        }
        if (filter != null) {
            filter.init(nativeReco, nativeRef);
        }
        TreeSet<DistanceSmall> QSortedCostAcc = new TreeSet<>(cmpCostsAcc);
//        HashSet<IDistance<Reco, Reference>> G = new LinkedHashSet<>();
        int[] startPoint = new int[]{0, 0};
        DistanceSmall start = new DistanceSmall(null, startPoint, 0, null);
        distMat.set(startPoint, start);
        QSortedCostAcc.add(start);
//        G.add(start);
//        distMat.set(startPoint, start);
        ProcessField bar = sizeProcessViewer > 0 || folderDynMats != null ? new ProcessField("calculating Dynamic Matrix", sizeProcessViewer, folderDynMats, show) : null;
        int factorNextCleanup = 500000;
        int boundNextCleanup = factorNextCleanup;
        int cntEdges = 0;
        int cntVerticies = 0;
//        int cntEdges = 0;
        boolean[][] isdead = new boolean[distMat.sizeY][distMat.sizeX];
        StopWatch swCalculators = new StopWatch("calculators");
        StopWatch swHandle = new StopWatch("handles");
        StopWatch swCleanup = new StopWatch("cleanup");
        while (!QSortedCostAcc.isEmpty()) {
            cntVerticies++;
            DistanceSmall distActual = QSortedCostAcc.pollFirst();
            if (updateScheme.equals(UpdateScheme.LAZY) && distMat.getLastElement() == distActual) {
                break;
            }
            if (isdead[distActual.point[0]][distActual.point[1]]) {
                continue;
            }
            isdead[distActual.point[0]][distActual.point[1]] = true;
            StopWatch.start("FilterAllow");
            if (filter != null && !filter.followPathsFromBestEdge(distActual)) {
                StopWatch.stop("FilterAllow");
                continue;
            }
            StopWatch.stop("FilterAllow");
            final int[] pos = distActual.point;
            if (bar != null) {
                bar.update(pos, distMat, distActual);
            }
            //all Neighbours v of u
            for (ICostCalculator<Reco, Reference> costCalculator : costCalculators) {
                StopWatch.start(costCalculator.getClass().getSimpleName());
                swCalculators.start();
                DistanceSmall distance = costCalculator.getNeighbourSmall(pos, distActual);
                swCalculators.stop();
                StopWatch.stop(costCalculator.getClass().getSimpleName());
                swHandle.start();
                cntEdges += handleDistance(distance, distMat, QSortedCostAcc);
                swHandle.stop();
            }
            for (ICostCalculatorMulti<Reco, Reference> costCalculator : costCalculatorsMutli) {
                StopWatch.start(costCalculator.getClass().getSimpleName());
                swCalculators.start();
                List<DistanceSmall> distances = costCalculator.getNeighboursSmall(pos, distActual);
                swCalculators.stop();
                StopWatch.stop(costCalculator.getClass().getSimpleName());
                if (distances == null) {
                    continue;
                }
                swHandle.start();
                for (DistanceSmall distance : distances) {
                    cntEdges += handleDistance(distance, distMat, QSortedCostAcc);
                }
                swHandle.stop();
            }
            if (maxCount > 0 && cntVerticies >= maxCount) {
                LOG.debug(String.format("found count = %d, return with so far calculated dynProg.", cntEdges));
                return distMat;
            }

            if (cntVerticies > boundNextCleanup) {
                swCleanup.start();
                distMat = distMat.cleanup(QSortedCostAcc);
                boundNextCleanup = cntVerticies + factorNextCleanup;
//                factorNextCleanup *= 1.0;
                swCleanup.stop();
            }
        }
        LOG.info("time spent: {}", swCalculators);
        LOG.info("time spent: {}", swHandle);
        LOG.info("time spent: {}", swCleanup);
        LOG.info("time spent: \n{}", StopWatch.getStats());
        if (bar != null) {
            bar.update(null, distMat, null);
            bar.setEnd();
        }
        if (distMat.getLastElement() == null) {
            LOG.warn("no path found from start to end with given cost calulators");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("caculate " + cntEdges + " edges for " + ((reco.size() - 1) * (ref.size() - 1)) + "/" + cntVerticies + " verticies");
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
        return calcDynProg(reco, ref).getLastElement().costsAcc;
    }

    public double calcCosts(Reco[] reco, Reference[] ref) {
        return calcDynProg(Arrays.asList(reco), Arrays.asList(ref)).getLastElement().costsAcc;
    }

    public static class Distance<Reco, Reference> extends DistanceSmall implements PathCalculatorGraph.IDistance<Reco, Reference> {

        //        private final Distance previousDistance;
        private final String manipulation;
        private final double costs;
        private Reco[] recos;
        private Reference[] references;
//        private boolean marked = false;

        public Distance(DistanceSmall distanceSmall, String manipulation, double costs, Reco[] recos, Reference[] references) {
            this(manipulation, costs, distanceSmall.costsAcc, distanceSmall.point, distanceSmall.pointPrevious, recos, references);
        }

        public Distance(String manipulation, double costs, double costAcc, int[] point, int[] previous, Reco[] recos, Reference[] references) {
            super(previous, point, costAcc, null);
            this.manipulation = manipulation;
            this.costs = costs;
            this.recos = recos;
            this.references = references;
        }

        public boolean isMarked() {
            return marked;
        }

        public void mark(boolean mark) {
            this.marked = mark;
        }

        @Override
        public void dispose() {
            super.dispose();
            if (!isMarked()) {
                recos = null;
                references = null;
            }
        }

        @Override
        public IDistance<Reco, Reference> getLargeDistance() {
            return this;
        }

//        @Override
//        public IDistance<Reco, Reference> getLargeDistance(DistanceMat<Reco, Reference> mat) {
//            return this;
//        }

        @Override
        public double getCosts() {
            return costs;
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
        public int[] getPointPrevious() {
            return super.pointPrevious;
        }

        @Override
        public int[] getPoint() {
            return super.point;
        }

        @Override
        public double getCostsAcc() {
            return super.costsAcc;
        }

        @Override
        public String toString() {
            return String.format("cost=%5.2f, manipulation=%10s, costAcc=%5.2f, pos=[%3d,%3d], reco=%s, ref=%s", costs, manipulation, costsAcc, point[0], point[1], Arrays.deepToString(recos), Arrays.deepToString(references));
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
        private File outFile;
        private boolean show;

        public ProcessField(String title, int sizeProcessViewer, File outFile, boolean show) {
            this.size = sizeProcessViewer;
            this.show = show;
            this.outFile = outFile;
            // JProgressBar-Objekt wird erzeugt
            progressBar = new JProgressBar(0, steps);

            // Wert für den Ladebalken wird gesetzt
            progressBar.setValue(0);
            // Der aktuelle Wert wird als
            // Text in Prozent angezeigt
            progressBar.setStringPainted(true);
            if (show) {
                mainFrame = new JFrame();
//        meinJFrame.setSize(size, size + 300);
                mainFrame.setTitle(title);
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                JPanel meinPanel = new JPanel();
                meinPanel.setLayout(new BoxLayout(meinPanel, BoxLayout.Y_AXIS));


                image = new JLabel();
                image.setIcon(new ImageIcon(HeatMapUtil.getHeatMap(new double[size][size], 7)));
                image.setAlignmentX(Component.CENTER_ALIGNMENT);
                progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
                meinPanel.add(image);
                // JProgressBar wird Panel hinzugefügt
                meinPanel.add(progressBar);
                mainFrame.add(meinPanel);
                if (size > 0) {
                    mainFrame.setLocation(new Point((int) (screenSize.getWidth() - mainFrame.getWidth()) / 2, (int) (screenSize.getHeight() - mainFrame.getHeight()) / 2));
                    mainFrame.setVisible(true);
                    mainFrame.pack();
                }
            }
        }

        private void setEnd() {
            progressBar.setValue(steps);
            if (outFile != null && mat != null) {
                HeatMapUtil.save(HeatMapUtil.getHeatMap(mat, 7), outFile);
            }
        }

        private void dispose() {
            if (show) {
                mainFrame.setVisible(false);
                mainFrame.dispose();
            }

        }

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        void update(int[] pos, DistanceMat distMat, DistanceSmall actual) {
            int sizeX = isDebug ? size : distMat.getSizeX();
            int sizeY = isDebug ? size : distMat.getSizeY();
            int newProcess = pos == null ? steps : (int) Math.round(((double) pos[1] / sizeX * steps));
            if (progressBar.getValue() < newProcess || isDebug || pos == null) {
                int mb = (1024 * 1024);
                MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                LOG.trace("memory usage: {}/{} = {}%", heapMemoryUsage.getUsed() / mb, heapMemoryUsage.getMax() / mb, heapMemoryUsage.getUsed() * 100 / heapMemoryUsage.getMax());
                progressBar.setValue(newProcess);
                int factorX = Math.max(1, sizeX / size);
                int factorY = Math.max(1, sizeY / size);
                if (mat == null) {
                    mat = new double[sizeY / factorY][sizeX / factorX];
                }
                for (int i = 0; i < mat.length; i++) {
                    double[] vec = mat[i];
                    for (int j = 0; j < vec.length; j++) {
                        DistanceSmall dist = distMat.get(i * factorY, j * factorX);
                        if (dist == null) {
                            vec[j] = vec[j] > 0 ? vec[j] : isDebug ? -5 : 0;
                        } else {
                            vec[j] = vec[j] > 0 ? Math.min(dist.costsAcc, vec[j]) : dist.costsAcc;
                        }
//                        vec[j] = dist == null ? isDebug ? -5 : 0 : dist.getCostsAcc();
                    }
                }
                if (show) {
                    BufferedImage heatMap = HeatMapUtil.getHeatMap(mat, 7);
                    image.setIcon(new ImageIcon(heatMap));
                    mainFrame.invalidate();
                    mainFrame.revalidate();
                    mainFrame.repaint();
                }
//                System.out.println("done");
            }

        }

        public BufferedImage getImage() {
            return HeatMapUtil.getHeatMap(mat, 7);
        }

    }

}
