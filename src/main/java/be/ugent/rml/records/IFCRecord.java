package be.ugent.rml.records;

import java.util.Collections;
import java.util.List;

public class IFCRecord extends Record {

    String entity;

    public IFCRecord(String entity)
    {
        this.entity = entity;
    }

    public List<Object> get(String value)
    {
        return Collections.singletonList(entity);
    }

    /**
     * This method returns the datatype of a reference in the record.
     * @param value the reference for which the datatype needs to be returned.
     * @return the IRI of the datatype.
     */
    public String getDataType(String value)
    {
        return null;
    }
}
