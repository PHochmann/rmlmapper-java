package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;

public class CdClass extends CdElement {
    String name;
    boolean interf;
    CdClass base;
    double separatorY;
    List<CdClass> children;
    List<CdArrow> uses;
    List<CdArrow> usedBy;
    List<CdAttribute> attributes;

    public CdClass(Node node, boolean interf) {
        super(node);
        this.name = node.getAttributes().getNamedItem("value").getTextContent();
        this.interf = interf;
        this.children = new LinkedList<>();
        this.uses = new LinkedList<>();
        this.usedBy = new LinkedList<>();
        this.attributes = new LinkedList<>();
        this.node = node;
    }

    public void addChild(CdClass child) {
        children.add(child);
        child.base = this;
    }

    public void addUse(CdArrow use) {
        uses.add(use);
        use.target.usedBy.add(use);
    }

    public void addAttribute(CdAttribute attr) {
        attributes.add(attr);
        attr.clazz = this;
    }

    public CdAttributeType getAttributeType(double y) {
        if (y < separatorY) {
            // It's a field
            return CdAttributeType.CD_FIELD;
        } else {
            // It's a function
            return CdAttributeType.CD_FUNCTION;
        }
    }

    public void setSeparatorY(double y) {
        this.separatorY = y;
    }

    public String get(String ref) {
        if (ref.equals("id")) {
            return id;
        } else {
            if (ref.equals("name")) {
                return name;
            } else {
                if (ref.startsWith("base.")) {
                    if (base == null) return null;
                    return base.get(ref.substring(ref.indexOf(".") + 1));
                } else {
                    return null;
                }
            }
        }
    }

    public List<CdClass> extractClasses(String selector) {

        LinkedList<CdClass> res = new LinkedList<>();

        if (selector.equals("base")) {
            if (base != null) res.add(base);
        } else {
            if (selector.equals("children")) {
                res.addAll(children);
            } else {
                if (selector.equals("usedClasses")) {
                    for (CdArrow usage : uses) {
                        res.add(usage.target);
                    }
                } else {
                    if (selector.equals("userClasses")) {
                        for (CdArrow usedBy : usedBy) {
                            res.add(usedBy.source);
                        }
                    }
                }
            }
        }

        return res;
    }
}
