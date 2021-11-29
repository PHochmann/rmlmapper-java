package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CdUtils {
    public static String getStyle(Node node, String key) {

        Node style = node.getAttributes().getNamedItem("style");
        if (style == null) return null;
        String[] styles = style.getTextContent().split(";");

        for (int i = 0; i < styles.length; i++) {
            if (styles[i].startsWith(key)) {
                if (styles[i].indexOf("=") == -1) return "1"; // Not every style is "key=value", but sometimes only "key". Treat it as "key=1".
                return styles[i].substring(styles[i].indexOf("=") + 1);
            }
        }

        return null;
    }

    public static Node getChildNode(Node outer, String innerName) {
        NodeList children = outer.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeName().equals(innerName)) {
                return children.item(i);
            }
        }
        return null;
    }

    public static String getAttribute(Node node, String attr) {
        Node a = node.getAttributes().getNamedItem(attr);
        if (a == null) return null;
        return a.getTextContent();
    }
}
