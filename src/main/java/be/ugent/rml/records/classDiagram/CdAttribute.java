package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

public class CdAttribute extends CdElement {
    String modifier;
    String type;
    String name;
    CdAttributeType attrib_type;
    CdClass clazz;

    public CdAttribute(Node node, CdAttributeType attrib_type) {
        super(node);
        this.attrib_type = attrib_type;
        String[] words = node.getAttributes().getNamedItem("value").getTextContent().split(" ");
        if (words.length == 1) {
            name = words[0];
        } else {
            if (words.length == 2) {
                name = words[1];
                type = words[0];
            } else {
                if (words.length == 3) {
                    name = words[2];
                    type = words[1];
                    modifier = words[0];
                }
            }
        }
    }

    public String get(String ref) {
        if (ref.equals("id")) {
            return id;
        } else {
            if (ref.equals("name")) {
                return name;
            } else {
                if (ref.startsWith("class.")) {
                    return clazz.get(ref.substring(ref.indexOf(".") + 1));
                } else {
                    return null;
                }
            }
        }
    }

}