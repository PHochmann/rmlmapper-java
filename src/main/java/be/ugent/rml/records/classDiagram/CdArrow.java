package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

import java.util.Collections;

public class CdArrow extends CdElement {
    CdClass source;
    CdClass target;
    String label;
    String source_cardinality;
    String target_cardinality;
    CdArrowType type;

    public CdArrow(Node node, CdClass source, CdClass target) {
        super(node);
        this.source = source;
        this.target = target;
        this.type = getTypeFromStyle(node);
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
                        if (ref.equals("type")) {
                            return arrowTypeToString(this.type);
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

    private CdArrowType getTypeFromStyle(Node node) {
        String startArrow = CdUtils.getStyle(node, "startArrow");
        String endArrow = CdUtils.getStyle(node, "endArrow");
        String dashed = CdUtils.getStyle(node, "dashed");

        if (endArrow != null && dashed != null && endArrow.equals("open") && dashed.equals("1")) {
            return CdArrowType.CD_ARROW_DEPENDENCY;
        } else {
            if (endArrow != null && dashed != null && endArrow.equals("open") && dashed.equals("0")) {
                return CdArrowType.CD_ARROW_ASSOCIATION;
            } else {
                if (endArrow != null && startArrow != null && endArrow.equals("open") && startArrow.equals("diamondThin")) {
                    if (CdUtils.getStyle(node, "startFill").equals("1")) {
                        return CdArrowType.CD_ARROW_COMPOSITION;
                    } else {
                        return CdArrowType.CD_ARROW_AGGREGATION;
                    }
                }
            }
        }

        return CdArrowType.CD_ARROW_UNKNOWN;
    }

    private String arrowTypeToString(CdArrowType type) {
        switch (type) {
            case CD_ARROW_AGGREGATION:
                return "Aggregation";
            case CD_ARROW_DEPENDENCY:
                return "Dependency";
            case CD_ARROW_ASSOCIATION:
                return "Association";
            case CD_ARROW_COMPOSITION:
                return "Composition";
            default:
                return "Unknown";
        }
    }

}
