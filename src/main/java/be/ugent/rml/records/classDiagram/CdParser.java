package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class CdParser {

    Document doc;

    // To lookup cells fast and get parent/source/destination-cross references in a single pass
    Dictionary<String, Node> cells = new Hashtable<>();


    Dictionary<String, CdClass> classes  = new Hashtable<>();
    Dictionary<String, CdAttribute> attributes = new Hashtable<>();
    Dictionary<String, CdArrow> usages = new Hashtable<>();

    public CdParser(Document xmlDoc) {
        this.doc = xmlDoc;
    }

    public Enumeration<CdClass> getClasses() {
        return classes.elements();
    }

    public Enumeration<CdAttribute> getAttributes() {
        return attributes.elements();
    }

    public Enumeration<CdArrow> getUsages() {
        return usages.elements();
    }

    String[] getStyles(Node node) {
        Node style = node.getAttributes().getNamedItem("style");
        if (style == null) return null;
        return style.getTextContent().split(";");
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
                String val = getAttr(nodes.item(i), "value");
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
                    String parent_id = getAttr(cell, "parent");
                    if (parent_id != null)
                    {
                        CdClass parent = classes.get(parent_id);
                        if (parent != null) {
                            Node geometry = CdUtils.getChildNode(cell, "mxGeometry");
                            double y = Double.parseDouble(getAttr(geometry, "y"));
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
                    String parent_id = getAttr(cell, "parent");
                    if (parent_id != null) {
                        // Get y to determine if it's a field or a function
                        Node geometry = CdUtils.getChildNode(cell, "mxGeometry");
                        double y = Double.parseDouble(getAttr(geometry, "y"));

                        CdClass parent = classes.get(parent_id);
                        if (parent != null) {
                            CdAttribute attr = new CdAttribute(cell, parent.getAttributeType(y));
                            parent.addAttribute(attr);
                            attributes.put(attr.id, attr);
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
                    String source_id = getAttr(cell, "source");
                    String target_id = getAttr(cell, "target");

                    if (source_id == null || target_id == null) {
                        throw new Exception("No source/destination in arrow");
                        // Todo: reconstruct geometrically
                    }

                    CdClass source = classes.get(source_id);
                    CdClass target = classes.get(target_id);

                    // Maybe, arrow is not connected to class but to something else accidentally
                    if (source == null) {
                        Node node = cells.get(source_id);
                        if (node != null) {
                            source_id = getAttr(node, "parent");
                            source = classes.get(source_id);
                        }
                    }

                    if (target == null) {
                        Node node = cells.get(target_id);
                        if (node != null) {
                            target_id = getAttr(node, "parent");
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
                String parent_id = getAttr(cell, "parent");
                if (parent_id == null) {
                    throw new Exception("Text without parent");
                }

                CdArrow usage = usages.get(parent_id);
                if (usage != null) {
                    Node geometry = CdUtils.getChildNode(cell, "mxGeometry");
                    double x = Double.parseDouble(getAttr(geometry, "x"));
                    if (x < -0.8) {
                        usage.setSourceCardinality(getAttr(cell, "value"));
                    } else {
                        if (x > 0.8) {
                            usage.setTargetCardinality(getAttr(cell, "value"));
                        } else {
                            usage.setLabel(getAttr(cell, "value"));
                        }
                    }
                }
            }
        }

        System.out.println("Done parsing XML document! Classes:" + classes.size() + ", Attribs:" + attributes.size() + ", Usages:" + usages.size());
        System.out.println("~~");

    }

}
