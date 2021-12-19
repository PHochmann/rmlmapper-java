package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;

public class CdAttribute extends CdElement {
    String visibility;
    String type;
    String name;
    CdAttributeType attrib_type;
    CdClass clazz;

    // Needed because linebreaks could be used to create multiple attributes from one text label
    public static List<CdAttribute> getAttributes(Node node, CdAttributeType attrib_type) {
        String totalText = node.getAttributes().getNamedItem("value").getTextContent();
        String[] texts = totalText.split("%0A|&#xa;|\n");
        List<CdAttribute> res = new LinkedList<>();
        for (String str : texts) {
            if (!str.equals("")) {
                res.add(new CdAttribute(node, str, attrib_type));
            }
        }
        return res;
    }

    private CdAttribute(Node node, String text, CdAttributeType attrib_type) {
        super(node);
        this.attrib_type = attrib_type;

        this.name = text;

        String[] words = text.split(" ");
        if (words.length == 1) {
            name = words[0];
            type = "";
            visibility = "";
        } else {
            if (words.length == 2) {
                name = words[0];
                type = words[1];
                visibility = "";
            } else {
                if (words.length == 3) {
                    name = words[1];
                    type = words[2];
                    visibility = words[0];
                }
            }
        }
        if (name.endsWith(":")) name = name.substring(0, name.length() - 1);
    }

    public String get(String ref) throws Exception {
        if (ref.equals("id")) {
            return id;
        } else {
            if (ref.equals("name")) {
                return name;
            } else {
                if (ref.equals("datatype")) {
                    return type;
                } else {
                    if (ref.equals("visibility")) {
                        return visibility;
                    } else {
                        if (ref.equals("isFunction")) {
                            return attrib_type == CdAttributeType.CD_FUNCTION ? "true" : "false";
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



}