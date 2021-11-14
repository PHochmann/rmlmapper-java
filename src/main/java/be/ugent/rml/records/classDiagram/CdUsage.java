package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

import java.util.Collections;

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

    public String get(String ref) {
        if (ref.equals("id")) {
            return id;
        } else {
            if (ref.equals("label")) {
                return label;
            } else {
                if (ref.equals("sourceCardinality")) {
                    return source_cardinality;
                } else {
                    if (ref.equals("targetCardinality")) {
                        return target_cardinality;
                    } else {
                        if (ref.startsWith("source.")) {
                            return source.get(ref.substring(ref.indexOf(".") + 1));
                        } else {
                            if (ref.startsWith("target.")) {
                                return target.get(ref.substring(ref.indexOf(".") + 1));
                            } else {
                                return null;
                            }
                        }
                    }
                }
            }
        }
    }
}
