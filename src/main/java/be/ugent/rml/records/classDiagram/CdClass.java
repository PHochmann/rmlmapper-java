package be.ugent.rml.records.classDiagram;

import org.apache.jena.atlas.lib.Pair;
import org.w3c.dom.Node;

import java.util.LinkedList;
import java.util.List;

public class CdClass extends CdElement {
    String name;
    CdClass base;
    double separatorY;
    List<CdClass> children;
    List<CdUsage> uses;
    List<CdUsage> usedBy;
    List<CdAttribute> attributes;

    public CdClass(Node node) {
        super(node);
        this.name = node.getAttributes().getNamedItem("value").getTextContent();
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

    public void addUse(CdUsage use) {
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
}
