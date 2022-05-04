package be.ugent.rml.functions;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.records.IFCRecord;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.bimserver.client.BimServerClient;
import org.bimserver.client.ClientIfcModel;
import org.bimserver.client.GeometryException;
import org.bimserver.database.queries.om.QueryException;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterfaceException;
import org.bimserver.models.ifc2x3tc1.IfcRelVoidsElement;
import org.bimserver.plugins.services.Geometry;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ExecutionParser {

    private static Object getAttr(EObject obj, String attribute)
    {
        EStructuralFeature ftr = obj.eClass().getEStructuralFeature(attribute);
        return ftr != null ? obj.eGet(ftr) : null;
    }

    public static List<IFCRecord> filterEntitiesFromBoxDistance(ClientIfcModel model,
                                                                BimServerClient client,
                                                                long roid,
                                                                String id,
                                                                int dist) throws IfcModelInterfaceException, QueryException, ServerException, GeometryException, UserException, IOException {

        List<IFCRecord> res = new LinkedList<>();

        model.loadGeometry();

        for (IdEObject obj : model) {
            Object gid = getAttr(obj, "GlobalId");
            if (gid != null && gid.equals(id)) {
                Geometry g1 = client.getGeometry(roid, obj);
                for (IdEObject obj2 : model) {
                    Geometry g2 = client.getGeometry(roid, obj2);
                    if (Math.abs(g1.getMaxX() - g2.getMaxX()) < dist
                        || Math.abs(g1.getMaxX() - g2.getMaxX()) < dist
                        || Math.abs(g1.getMaxX() - g2.getMaxX()) < dist
                        || Math.abs(g1.getMaxX() - g2.getMaxY()) < dist
                        || Math.abs(g1.getMaxX() - g2.getMinX()) < dist
                        || Math.abs(g1.getMaxX() - g2.getMinY()) < dist
                        || Math.abs(g1.getMaxY() - g2.getMaxX()) < dist
                        || Math.abs(g1.getMaxY() - g2.getMaxY()) < dist
                        || Math.abs(g1.getMaxY() - g2.getMinX()) < dist
                        || Math.abs(g1.getMaxY() - g2.getMinY()) < dist
                        || Math.abs(g1.getMinX() - g2.getMaxX()) < dist
                        || Math.abs(g1.getMinX() - g2.getMaxY()) < dist
                        || Math.abs(g1.getMinX() - g2.getMinX()) < dist
                        || Math.abs(g1.getMinX() - g2.getMinY()) < dist
                        || Math.abs(g1.getMinY() - g2.getMaxX()) < dist
                        || Math.abs(g1.getMinY() - g2.getMaxY()) < dist
                        || Math.abs(g1.getMinY() - g2.getMinX()) < dist
                        || Math.abs(g1.getMinY() - g2.getMinY()) < dist) {
                            res.add(new IFCRecord(obj2));
                    }
                }
            }
        }
        return new LinkedList<IFCRecord>();
    }

    public static List<IFCRecord> getOpeningsOfWall(ClientIfcModel model, BimServerClient client, long roid, String id) {

        List<IFCRecord> res = new LinkedList<>();

        for (IdEObject obj : model)
        {
            Object gid = getAttr(obj,"GlobalId");
            if (gid != null && gid.equals(id)) {
                res.add(new IFCRecord(obj));
                for (EObject obj2 : obj.eCrossReferences()) {
                    if (obj2.eClass().getName().equals("IfcRelVoidsElement")) {
                        for (EObject obj3 : obj2.eCrossReferences())
                        {
                            if (obj3.eClass().getName().equals("IfcOpeningElement"))
                            {
                                res.add(new IFCRecord(obj3));
                                for (EObject obj4 : obj3.eCrossReferences()) {
                                    if (obj4.eClass().getName().equals("IfcRelFillsElement"))
                                    {
                                        Object filling = getAttr(obj4, "RelatedBuildingElement");
                                        if (filling != null) res.add(new IFCRecord((EObject)filling));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return res;
    }

    private static Object castLookup(Class<?> clazz, String str) {
        try {
            if (clazz == Integer.class) {
                return Integer.parseInt(str);
            }
            else {
                return clazz.cast(str);
            }
        } catch (Exception e) {
            throw new Error("Can't cast this data type (Todo but not in scope: Do lookup for non-builtin datatypes)");
            // Should be done as given in http://www.java2s.com/Code/Java/Reflection/ConvertagivenStringintotheappropriateClass.htm
        }
    }

    public static Pair<List<Class<?>>, List<Object>> parseParamsFromExecution(QuadStore store, Term execution)
    {
        List<Class<?>> classes = new LinkedList<>();
        LinkedList<Object> params = new LinkedList<>();

        List<Term> functions = Utils.getObjectsFromQuads(store.getQuads(execution, new NamedNode(NAMESPACES.FNO_S + "executes"), null));
        if (functions.isEmpty()) throw new Error("No 'executes' found in execution");
        Term function = functions.get(0);

        List<Term> expects = Utils.getObjectsFromQuads(store.getQuads(function, new NamedNode(NAMESPACES.FNO_S + "expects"), null));
        if (expects.isEmpty()) throw new Error("No 'expects' found in function - if no params, at least specify empty list (TODO)");
        Term expectList = expects.get(0);

        List<Term> params_list = Utils.getList(store, expectList);

        for (Term first : params_list)
        {
            List<Term> paramPredicates = Utils.getObjectsFromQuads(store.getQuads(first, new NamedNode(NAMESPACES.FNO_S + "predicate"), null));
            if (paramPredicates.isEmpty()) throw new Error("Parameter does not have a predicate.");
            Term paramPredicate = paramPredicates.get(0);

            List<Term> actualParams = Utils.getObjectsFromQuads(store.getQuads(execution, paramPredicate, null));
            if (actualParams.isEmpty()) throw new Error("Parameter that is specified by function has no value in execution");
            Term actualParam = actualParams.get(0);

            // Try to cast it to the correct type
            Class<?> classToCastTo = null;
            List<Term> paramTypes = Utils.getObjectsFromQuads(store.getQuads(first, new NamedNode(NAMESPACES.FNO_S + "type"), null));
            if (!paramTypes.isEmpty()) {
                classToCastTo = FunctionUtils.getParamType(paramTypes.get(0));
            }
            else {
                // If there is no type present, implicitly treat it as a string
                classToCastTo = String.class;
            }

            String actualParamStr = actualParam.getValue().replaceAll("^\"|\"$", "");
            params.add(castLookup(classToCastTo, actualParamStr));
            classes.add(classToCastTo);
        }

        return new Pair<>(classes, params);
    }

    public static Method getMethod(QuadStore store, Term mapping) {
        List<Term> implementations = Utils.getObjectsFromQuads(store.getQuads(mapping, new NamedNode(NAMESPACES.FNO_S + "implementation"), null));
        if (implementations.isEmpty()) throw new Error("No implementations found.");
        Term implementation = implementations.get(0);

        List<Term> classNames = Utils.getObjectsFromQuads(store.getQuads(implementation, new NamedNode(NAMESPACES.FNOI + "class-name"), null));
        if (classNames.isEmpty()) throw new Error("No class name found.");
        String className = classNames.get(0).toString().replaceAll("^\"|\"$", "");

        List<Term> methodMappings = Utils.getObjectsFromQuads(store.getQuads(mapping, new NamedNode(NAMESPACES.FNO_S + "methodMapping"), null));
        if (methodMappings.isEmpty()) throw new Error("No method mappings found.");
        Term methodMapping = methodMappings.get(0);

        List<Term> methodNames = Utils.getObjectsFromQuads(store.getQuads(methodMapping, new NamedNode(NAMESPACES.FNOM + "method-name"), null));
        if (methodNames.isEmpty()) throw new Error("No method names found.");
        String methodName = methodNames.get(0).toString().replaceAll("^\"|\"$", "");

        // Now that class and method name have been extracted, get the function by reflection. Then, execute it.
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new Error("Class not found in Java.");
        }

        Method method = null;
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                method = m;
                break;
            }
        }
        if (method == null) {
            throw new Error("Method not found in Java.");
        }

        return method;
    }

}
