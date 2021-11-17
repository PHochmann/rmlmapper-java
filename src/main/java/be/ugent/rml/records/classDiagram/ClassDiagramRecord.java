package be.ugent.rml.records.classDiagram;

import be.ugent.rml.records.Record;

import java.util.Collections;
import java.util.List;

public class ClassDiagramRecord extends Record {

    CdArrow usage = null;
    CdClass clazz = null;
    CdAttribute attribute = null;

    public ClassDiagramRecord(CdArrow usage) {
        this.usage = usage;
    }

    public ClassDiagramRecord(CdClass clazz) {
        this.clazz = clazz;
    }

    public ClassDiagramRecord(CdAttribute attribute) {
        this.attribute = attribute;
    }

    @Override
    public List<Object> get(String ref) {

        if (clazz != null) {
            return Collections.singletonList(clazz.get(ref));
        }

        if (usage != null) {
            return Collections.singletonList(usage.get(ref));
        }

        if (attribute != null) {
            return Collections.singletonList(attribute.get(ref));
        }

        return null;
    }

}
