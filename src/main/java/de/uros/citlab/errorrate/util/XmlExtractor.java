package de.uros.citlab.errorrate.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class XmlExtractor {

    public static List<Line> getLinesFromFile(File file) {
        Document documentFromFile = getDocumentFromFile(file.getPath());
        NodeList elementsByTagName = documentFromFile.getElementsByTagName("TextLine");
        List<Line> res = new LinkedList<>();
        for (int i = 0; i < elementsByTagName.getLength(); ++i) {
            Node line = elementsByTagName.item(i);
            Node baseline = getChild(line, "Baseline");
            Node coords = getChild(line, "Coords");
            String id = getAttribute(line, "id");
            if (baseline == null && coords == null) {
                throw new RuntimeException("no baseline or coords for textline " + id + " in file " + file.getPath() + ".");
            }

            Polygon baselinePoly = baseline == null ? null : string2Polygon(getAttribute(baseline, "points"));
            Polygon coordsPoly = coords == null ? null : string2Polygon(getAttribute(coords, "points"));
            Node textequiv = getChild(line, "TextEquiv");
            if (textequiv == null) {
                res.add(new Line(id, baselinePoly, coordsPoly));
            } else {
                Node unicode = getChild(textequiv, "Unicode");
                String attribute = getAttribute(textequiv, "conf");
                if (attribute == null) {
                    res.add(new Line(id, baselinePoly, coordsPoly, unicode.getTextContent()));
                } else {
                    res.add(new Line(id, baselinePoly, coordsPoly, unicode.getTextContent(), Float.parseFloat(attribute)));
                }
            }
        }

        return res;
    }

    static Document getDocumentFromFile(String path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(new File(path));
        } catch (ParserConfigurationException var4) {
            var4.printStackTrace();
            throw new RuntimeException();
        } catch (SAXException var5) {
            var5.printStackTrace();
            throw new RuntimeException("XML file could not be parsed, it is probably malformatted.");
        } catch (IOException var6) {
            throw new RuntimeException("XML file could not be found for given path: " + path);
        }
    }

    public static Polygon string2Polygon(String string) {
        String[] split = string.split(" ");
        int size = split.length;
        int[] x = new int[size];
        int[] y = new int[size];

        for (int i = 0; i < size; ++i) {
            String[] point = split[i].split(",");
            x[i] = Integer.parseInt(point[0]);
            y[i] = Integer.parseInt(point[1]);
        }

        return new Polygon(x, y, size);
    }

    private static String getAttribute(Node node, String key) {
        Node namedItem = node.getAttributes().getNamedItem(key);
        return namedItem == null ? null : namedItem.getTextContent();
    }

    private static Node getChild(Node parent, String name) {
        NodeList childNodes = parent.getChildNodes();
        Node res = null;

        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node child = childNodes.item(i);
            if (child.getNodeName().equals(name)) {
                if (res != null) {
                    throw new RuntimeException("there are more than one child with this name");
                }

                res = child;
            }
        }

        return res;
    }

    private static String polygon2String(Polygon p) {
        if(p==null){
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.npoints; i++) {
            sb.append(p.xpoints[i]).append(',').append(p.ypoints[i]).append(' ');
        }
        return sb.toString().trim();
    }

    public static class Line {
        public final String id;
        public final Polygon baseLine;
        public final Polygon coords;
        public final String textEquiv;
        public final Float confidence;

        public Line(String id, Polygon baseLine, Polygon coords) {
            this(id, baseLine, coords, (String) null, null);
        }

        public Line(String id, Polygon baseLine, Polygon coords, String textEquiv) {
            this(id, baseLine, coords, textEquiv, null);
        }

        public Line(String id, Polygon baseLine, Polygon coords, String textEquiv, Float confidence) {
            this.id = id;
            this.baseLine = baseLine;
            this.coords = coords;
            this.textEquiv = textEquiv;
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "Line{" +
                    "id='" + id + '\'' +
                    ", baseLine=" + polygon2String(baseLine) +
                    ", coords=" + polygon2String(coords) +
                    ", textEquiv='" + textEquiv + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }

}
