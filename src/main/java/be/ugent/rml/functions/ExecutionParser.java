package be.ugent.rml.functions;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.records.IFCRecord;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.bimserver.emf.IdEObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ExecutionParser {

    public static List<IFCRecord> filterEntitiesFromBoxDistance(Iterator<IdEObject> entities, int x, int y, int z, int width, int height, int depth, int dist) {
        return new LinkedList<IFCRecord>();
    }

    public static List<Object> parseParamsFromExecution(QuadStore store, Term execution)
    {
        LinkedList<Object> result = new LinkedList<>();

        List<Term> functions = Utils.getObjectsFromQuads(store.getQuads(execution, new NamedNode(NAMESPACES.FNO_S + "executes"), null));
        if (functions.isEmpty()) throw new Error("No 'executes' found in execution");
        Term function = functions.get(0);

        List<Term> expects = Utils.getObjectsFromQuads(store.getQuads(function, new NamedNode(NAMESPACES.FNO_S + "expects"), null));
        if (expects.isEmpty()) throw new Error("No 'expects' found in function - if no params, at least specify empty list (TODO)");
        Term expectList = expects.get(0);

        // Parse list of parameters (https://ontola.io/blog/ordered-data-in-rdf/)
        while (!expectList.equals(new NamedNode(NAMESPACES.RDF + "nil")))
        {
            List<Term> firsts = Utils.getObjectsFromQuads(store.getQuads(expectList, new NamedNode(NAMESPACES.RDF + "first"), null));
            if (firsts.isEmpty()) throw new Error("Malformed list: Does not have first");
            Term first = firsts.get(0);

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

            try {
                if (classToCastTo == Integer.class) {
                    result.add(Integer.parseInt(actualParamStr));
                }
                else {
                    result.add(classToCastTo.cast(actualParamStr));
                }
            } catch (Exception e) {
                throw new Error("Can't cast this data type (Todo but not in scope: Do lookup for non-builtin datatypes)");
                // Should be done as given in http://www.java2s.com/Code/Java/Reflection/ConvertagivenStringintotheappropriateClass.htm
            }


            List<Term> rests = Utils.getObjectsFromQuads(store.getQuads(expectList, new NamedNode(NAMESPACES.RDF + "rest"), null));
            if (rests.isEmpty()) throw new Error("Malformed list: Does not have rest");
            expectList = rests.get(0);
        }

        return result;
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
