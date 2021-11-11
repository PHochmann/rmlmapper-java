package be.ugent.rml.records;

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
