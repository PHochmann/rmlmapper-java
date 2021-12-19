package be.ugent.rml.records.classDiagram;

import org.apache.commons.lang.time.StopWatch;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.*;

public class CdParser {

    final double MAX_RECOVERABLE_DISTANCE = 300;

    Document doc;

    // To lookup cells fast and get parent/source/destination-cross references in a single pass
    Map<String, Node> cells = new HashMap<>();


    Map<String, CdClass> classes  = new HashMap<>();
    Map<String, CdAttribute> attributes = new HashMap<>();
    Map<String, CdArrow> usages = new HashMap<>();

    public CdParser(Document xmlDoc) {
        this.doc = xmlDoc;
    }

    public Collection<CdClass> getClasses() {
        return classes.values();
    }

    public Collection<CdAttribute> getAttributes() {
        return attributes.values();
    }

    public Collection<CdArrow> getUsages() {
        return usages.values();
    }

    String[] getStyles(Node node) {
        Node style = node.getAttributes().getNamedItem("style");
        if (style == null) return null;
        return style.getTextContent().split(";");
    }

    public CdClass getNearestClass(double x, double y) {
        CdClass nearest = null;
        double min_dist = Double.MAX_VALUE;

        for (CdClass curr : getClasses()) {
            double dist = curr.getDistanceToBox(x, y);
            if (dist < min_dist) {
                min_dist = dist;
                nearest = curr;
            }
        }

        if (min_dist > MAX_RECOVERABLE_DISTANCE) {
            System.out.println("Arrow attachment to class not recoverable: max distance exceeded.");
            return null;
        } else {
            return nearest;
        }
    }

    public void parseClassDiagram() throws Exception {

        // Create lookup-table for all cells
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/mxfile/diagram/mxGraphModel/root/mxCell";
        NodeList nodes = (NodeList)xpath.evaluate(expression, this.doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            cells.put(nodes.item(i).getAttributes().getNamedItem("id").getTextContent(), nodes.item(i));
        }
        // - - - - - -

        // Create classes
        for (int i = 0; i < nodes.getLength(); i++) {
            String swimlane_str = CdUtils.getStyle(nodes.item(i),"swimlane");
            if (swimlane_str != null && swimlane_str.equals("1")) {
                // We have a new class
                CdClass clazz = new CdClass(nodes.item(i), false);
                classes.put(clazz.id, clazz);
            } else { // Interfaces have to be detected differently
                String val = CdUtils.getAttribute(nodes.item(i), "value");
                if (val != null && val.contains("«interface»")) {
                    CdClass clazz = new CdClass(nodes.item(i), true);
                    classes.put(clazz.id, clazz);
                }
            }
        }

        // Get field <-> function separator lines
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cell = nodes.item(i);
            String[] styles = getStyles(cell);
            if (styles != null && styles.length > 0) {
                if (styles[0].equals("line")) {
                    String parent_id = CdUtils.getAttribute(cell, "parent");
                    if (parent_id != null)
                    {
                        CdClass parent = classes.get(parent_id);
                        if (parent != null) {
                            Node geometry = CdUtils.getChildNode(cell, "mxGeometry");
                            double y = Double.parseDouble(CdUtils.getAttribute(geometry, "y"));
                            parent.setSeparatorY(y);
                        }
                    }
                }
            }
        }

        // Create attributes and labels
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cell = nodes.item(i);
            String[] styles = getStyles(cell);
            if (styles != null && styles.length > 0) {
                if (styles[0].equals("text")) {
                    // Check if cell has parent that is class
                    String parent_id = CdUtils.getAttribute(cell, "parent");
                    if (parent_id != null) {
                        // Get y to determine if it's a field or a function
                        Node geometry = CdUtils.getChildNode(cell, "mxGeometry");
                        double y = Double.parseDouble(CdUtils.getAttribute(geometry, "y"));

                        CdClass parent = classes.get(parent_id);
                        if (parent != null) {
                            List<CdAttribute> attrs = CdAttribute.getAttributes(cell, parent.getAttributeType(y));
                            for (CdAttribute attr : attrs) {
                                parent.addAttribute(attr);
                                attributes.put(attr.id, attr);
                            }
                        }
                    } else {
                        throw new Exception("Text without parent");
                    }
                }
            }
        }

        // Add cross references
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cell = nodes.item(i);
            String[] styles = getStyles(cell);
            if (styles != null) {
                if (CdUtils.getStyle(cell, "endArrow") != null) {

                    String source_id = CdUtils.getAttribute(cell, "source");
                    String target_id = CdUtils.getAttribute(cell, "target");
                    CdClass source = null;
                    CdClass target = null;

                    String mxpoints_xpath = "mxGeometry/mxPoint";
                    NodeList mxPoints = (NodeList)xpath.evaluate(mxpoints_xpath, cell, XPathConstants.NODESET);

                    if (source_id == null) {
                        //System.out.println("Note: No source in arrow, trying to recover...");
                        for (int j = 0; j < mxPoints.getLength(); j++) {
                            String as_string = CdUtils.getAttribute(mxPoints.item(j), "as");
                            if (as_string != null && as_string.equals("sourcePoint")) {
                                double x = Double.parseDouble(CdUtils.getAttribute(mxPoints.item(j), "x"));
                                double y = Double.parseDouble(CdUtils.getAttribute(mxPoints.item(j), "y"));
                                source = getNearestClass(x, y);
                                //System.out.println("Note: Recovered arrow from " + source.name);
                                break;
                            }
                        }

                        if (source == null) {
                            throw new Exception("No target id and could not recover target class");
                        }
                    }
                    else
                    {
                        source = classes.get(source_id);
                    }

                    if (target_id == null) {
                        //System.out.println("Note: No target in arrow, trying to recover...");
                        for (int j = 0; j < mxPoints.getLength(); j++) {
                            String as_string = CdUtils.getAttribute(mxPoints.item(j), "as");
                            if (as_string != null && as_string.equals("targetPoint")) {
                                double x = Double.parseDouble(CdUtils.getAttribute(mxPoints.item(j), "x"));
                                double y = Double.parseDouble(CdUtils.getAttribute(mxPoints.item(j), "y"));
                                target = getNearestClass(x, y);
                                //System.out.println("Note: Recovered arrow to " + target.name);
                                break;
                            }
                        }

                        if (target == null) {
                            throw new Exception("No target id and could not recover target class");
                        }
                    }
                    else
                    {
                        target = classes.get(target_id);
                    }

                    
                    // Maybe, arrow is not connected to class but to something else accidentally
                    if (source == null) {
                        Node node = cells.get(source_id);
                        if (node != null) {
                            source_id = CdUtils.getAttribute(node, "parent");
                            source = classes.get(source_id);
                        }
                    }

                    if (target == null) {
                        Node node = cells.get(target_id);
                        if (node != null) {
                            target_id = CdUtils.getAttribute(node, "parent");
                            target = classes.get(target_id);
                        }
                    }

                    if (source == null || target == null) {
                        throw new Exception("Source/target does not point to a class");
                    }

                    // Now check which type of arrow we actually have
                    if (styles[0].equals("endArrow=block")) {
                        // Inheritance
                        target.addChild(source);
                    } else {
                        // Uses
                        CdArrow usage = new CdArrow(cell, source, target);
                        usages.put(usage.id, usage);
                    }
                }
            }
        }

        // Add labels and cardinalities to cross references
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cell = nodes.item(i);
            String resizable_str = CdUtils.getStyle(cell,"resizable");
            if (resizable_str != null && resizable_str.equals("0")) {
                String parent_id = CdUtils.getAttribute(cell, "parent");
                if (parent_id == null) {
                    throw new Exception("Text without parent");
                }

                CdArrow usage = usages.get(parent_id);
                if (usage != null) {
                    Node geometry = CdUtils.getChildNode(cell, "mxGeometry");
                    double x = Double.parseDouble(CdUtils.getAttribute(geometry, "x"));
                    if (x < -0.8) {
                        usage.setSourceCardinality(CdUtils.getAttribute(cell, "value"));
                    } else {
                        if (x > 0.8) {
                            usage.setTargetCardinality(CdUtils.getAttribute(cell, "value"));
                        } else {
                            usage.setLabel(CdUtils.getAttribute(cell, "value"));
                        }
                    }
                }
            }
        }


        //System.out.println("Done parsing XML document! Classes:" + classes.size() + ", Attribs:" + attributes.size() + ", Usages:" + usages.size());
        //System.out.println("~~");

    }

}
