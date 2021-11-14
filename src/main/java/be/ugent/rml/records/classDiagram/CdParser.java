package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
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
    Dictionary<String, CdUsage> usages = new Hashtable<>();

    public CdParser(Document xmlDoc) {
        this.doc = xmlDoc;
    }

    public Enumeration<CdClass> getClasses() {
        return classes.elements();
    }

    public Enumeration<CdAttribute> getAttributes() {
        return attributes.elements();
    }

    public Enumeration<CdUsage> getUsages() {
        return usages.elements();
    }

    String[] getStyles(Node node) {
        Node style = node.getAttributes().getNamedItem("style");
        if (style == null) return null;
        return style.getTextContent().split(";");
    }

    String getAttr(Node node, String attr) {
        Node a = node.getAttributes().getNamedItem(attr);
        if (a == null) return null;
        return a.getTextContent();
    }

    Node getChildNode(Node outer, String innerName) {
        NodeList children = outer.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals(innerName)) {
                return children.item(i);
            }
        }
        return null;
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
            Node cell = nodes.item(i);
            String[] styles = getStyles(cell);
            if (styles != null) {
                if (styles[0].equals("swimlane")) {
                    // We have a new class
                    CdClass clazz = new CdClass(cell);
                    classes.put(clazz.id, clazz);
                }

            }
        }

        // Add cross references
        for (int i = 0; i < nodes.getLength(); i++) {
            Node cell = nodes.item(i);
            String[] styles = getStyles(cell);
            if (styles != null) {
                if (styles[0].equals("endArrow=block") || styles[0].equals("endArrow=open")) { // Catch both types of arrows
                    String source_id = getAttr(cell, "source");
                    String destination_id = getAttr(cell, "target");

                    if (source_id == null || destination_id == null) {
                        throw new Exception("No source/destination in arrow");
                    }

                    CdClass source = classes.get(source_id);
                    CdClass target = classes.get(destination_id);
                    if (source == null || target == null) {
                        throw new Exception("Source/target does not point to a class");
                    }

                    // Now check which type of arrow we actually have
                    if (styles[0].equals("endArrow=block")) {
                        // Inheritance
                        target.addChild(source);
                    } else {
                        // Uses
                        CdUsage usage = new CdUsage(cell, source, target);
                        usages.put(usage.id, usage);
                    }
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
                            Node geometry = getChildNode(cell, "mxGeometry");
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
                    if (parent_id != null)
                    {
                        // Get y to determine if it's a field or a function
                        Node geometry = getChildNode(cell, "mxGeometry");
                        double y = Double.parseDouble(getAttr(geometry, "y"));

                        CdClass parent = classes.get(parent_id);
                        if (parent != null) {
                            CdAttribute attr = new CdAttribute(cell, parent.getAttributeType(y));
                            parent.addAttribute(attr);
                            attributes.put(attr.id, attr);
                        } else {
                            CdUsage usage = usages.get(parent_id);
                            if (usage != null) {
                                usage.setLabel(getAttr(cell, "value"));
                            } else {
                                throw new Exception("Text has parent that is neither class nor usage-arrow");
                            }
                        }
                    } else {
                        throw new Exception("Text without parent");
                    }
                } else {
                    // Check for usage cardinalities
                    if (styles[0].equals("resizable=0")) {
                        String parent_id = getAttr(cell, "parent");
                        if (parent_id == null) throw new Exception("Text without parent");

                        CdUsage usage = usages.get(parent_id);
                        if (usage != null) {
                            Node geometry = getChildNode(cell, "mxGeometry");
                            String geoX = getAttr(geometry, "x");
                            if (Double.parseDouble(geoX) < 0) {
                                usage.setSourceCardinality(getAttr(cell, "value"));
                            } else {
                                usage.setTargetCardinality(getAttr(cell, "value"));
                            }
                        } else {
                            throw new Exception("Text has parent that is neither class nor usage-arrow");
                        }
                    }
                }

            }
        }

        System.out.println("Done parsing XML document! Classes:" + classes.size() + ", Attribs:" + attributes.size() + ", Usages:" + usages.size());
        System.out.println("~~");

    }

}
