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

        this.name = node.getAttributes().getNamedItem("value").getTextContent();

        String[] words = node.getAttributes().getNamedItem("value").getTextContent().split(" ");
        if (words.length == 1) {
            name = words[0];
            type = "";
            modifier = "";
        } else {
            if (words.length == 2) {
                name = words[1];
                type = words[0];
                modifier = "";
            } else {
                if (words.length == 3) {
                    name = words[1];
                    if (name.endsWith(":")) name = name.substring(0, name.length() - 2);
                    type = words[2];
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
                if (ref.equals("type")) {
                    return type;
                } else {
                    if (ref.equals("modifier")) {
                        return modifier;
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
    }

}