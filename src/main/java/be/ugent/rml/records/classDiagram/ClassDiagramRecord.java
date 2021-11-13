package be.ugent.rml.records.classDiagram;

import be.ugent.rml.records.Record;

import java.util.Collections;
import java.util.List;

public class ClassDiagramRecord extends Record {

    public ClassDiagramRecord() {

    }

    @Override
    public List<Object> get(String value) {
        return Collections.singletonList("test");
    }

}
