package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

public class CdUsage extends CdElement {
    CdClass source;
    CdClass target;
    String label;
    String source_cardinality;
    String target_cardinality;

    public CdUsage(Node node, CdClass source, CdClass target) {
        super(node);
        this.source = source;
        this.target = target;
        source.addUse(this);
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public void setSourceCardinality(String value) {
        this.source_cardinality = value;
    }

    public void setTargetCardinality(String value) {
        this.target_cardinality = value;
    }
}
