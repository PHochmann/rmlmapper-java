package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

public class CdAttribute extends CdElement {
    String visibility;
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
            visibility = "";
        } else {
            if (words.length == 2) {
                name = words[1];
                type = words[0];
                visibility = "";
            } else {
                if (words.length == 3) {
                    name = words[1];
                    if (name.endsWith(":")) name = name.substring(0, name.length() - 1);
                    type = words[2];
                    visibility = words[0];
                }
            }
        }
    }

    public String get(String ref) throws Exception {
        if (ref.equals("id")) {
            return id;
        } else {
            if (ref.equals("name")) {
                return name;
            } else {
                if (ref.equals("type")) {
                    return type;
                } else {
                    if (ref.equals("visibility")) {
                        return visibility;
                    } else {
                        if (ref.startsWith("class.")) {
                            return clazz.get(ref.substring(ref.indexOf(".") + 1));
                        } else {
                            throw new Exception("Invalid reference for attribute");
                        }
                    }
                }
            }
        }
    }

}