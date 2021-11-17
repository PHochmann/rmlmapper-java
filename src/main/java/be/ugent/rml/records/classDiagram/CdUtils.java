package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

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
}
