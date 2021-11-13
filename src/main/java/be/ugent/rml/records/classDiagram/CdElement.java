package be.ugent.rml.records.classDiagram;

import org.w3c.dom.Node;

abstract public class CdElement {

    String id;
    Node node;

    public CdElement(Node node) {
        this.id = node.getAttributes().getNamedItem("id").getTextContent();
        this.node = node;
    }
}
