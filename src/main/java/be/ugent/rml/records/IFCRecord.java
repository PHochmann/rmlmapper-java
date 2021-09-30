package be.ugent.rml.records;

import org.bimserver.client.BimServerClient;
import org.bimserver.emf.IdEObject;

import java.util.Collections;
import java.util.List;

public class IFCRecord extends Record {

    IdEObject obj;
    //BimServerClient client;

    public IFCRecord(IdEObject obj)
    {
        this.obj = obj;
    }

    @Override
    public List<Object> get(String value)
    {
        if (value.equals("oid")) return Collections.singletonList(obj.getOid());
        if (value.equals("IfcType")) return Collections.singletonList(obj.eClass().getName());
        return Collections.singletonList(obj.eGet(obj.eClass().getEStructuralFeature(value)));
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
