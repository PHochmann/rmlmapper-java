package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

public class CdArrow extends CdElement {
    CdClass source;
    CdClass target;
    String label;
    String source_cardinality;
    String target_cardinality;
    CdArrowStyle style;

    public CdArrow(Node node, CdClass source, CdClass target) {
        super(node);
        this.source = source;
        this.target = target;
        this.style = getArrowStyle(node);
        this.label = CdUtils.getAttribute(node, "value");
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

    public String get(String ref) throws Exception {
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
                        if (ref.equals("style")) {
                            return arrowTypeToString(this.style);
                        } else {
                            if (ref.startsWith("source.")) {
                                return source.get(ref.substring(ref.indexOf(".") + 1));
                            } else {
                                if (ref.startsWith("target.")) {
                                    return target.get(ref.substring(ref.indexOf(".") + 1));
                                } else {
                                    throw new Exception("Malformed reference for arrow : '" + ref + "'");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private CdArrowStyle getArrowStyle(Node node) {
        String startArrow = CdUtils.getStyle(node, "startArrow");
        String endArrow = CdUtils.getStyle(node, "endArrow");
        String dashed = CdUtils.getStyle(node, "dashed");

        if (endArrow != null && dashed != null && endArrow.equals("open") && dashed.equals("1")) {
            return CdArrowStyle.CD_ARROW_DEPENDENCY;
        } else {
            if (endArrow != null && dashed != null && endArrow.equals("open") && dashed.equals("0")) {
                return CdArrowStyle.CD_ARROW_ASSOCIATION;
            } else {
                if (endArrow != null && startArrow != null && endArrow.equals("open") && startArrow.equals("diamondThin")) {
                    if (CdUtils.getStyle(node, "startFill").equals("1")) {
                        return CdArrowStyle.CD_ARROW_COMPOSITION;
                    } else {
                        return CdArrowStyle.CD_ARROW_AGGREGATION;
                    }
                }
            }
        }

        return CdArrowStyle.CD_ARROW_UNKNOWN;
    }

    private String arrowTypeToString(CdArrowStyle type) {
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
