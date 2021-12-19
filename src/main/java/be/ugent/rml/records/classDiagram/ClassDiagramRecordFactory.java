package be.ugent.rml.records.classDiagram;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.records.Record;
import be.ugent.rml.records.ReferenceFormulationRecordFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.commons.lang.time.StopWatch;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class ClassDiagramRecordFactory implements ReferenceFormulationRecordFactory {

    final String OF = "of";
    final String BY = "by";
    final String WHERE = "where";
    final String IS = "==";
    final String ISNOT = "!=";
    final String AND = "and";
    final String OR = "or";

    Dictionary<String, CdArrowStyle> arrowMapping;

    public ClassDiagramRecordFactory() {
        arrowMapping = new Hashtable<>();
        arrowMapping.put("associations", CdArrowStyle.CD_ARROW_ASSOCIATION);
        arrowMapping.put("aggregations", CdArrowStyle.CD_ARROW_AGGREGATION);
        arrowMapping.put("compositions", CdArrowStyle.CD_ARROW_COMPOSITION);
        arrowMapping.put("dependencies", CdArrowStyle.CD_ARROW_DEPENDENCY);
    }

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws Exception {

        CdParser parser = null;
        StopWatch sw = new StopWatch();
        for (int i = 0; i < 1; i++) {
            sw.start();

            InputStream stream = access.getInputStream();
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xml_doc = builder.parse(stream);

            parser = new CdParser(xml_doc);
            parser.parseClassDiagram();

            sw.stop();
            //System.out.print(sw.getTime() + ",");
            sw.reset();
        }

        // Now that the xml-Document is parsed to a bunch of Class-Diagram classes, process iterator
        List<Term> iterators = Utils.getObjectsFromQuads(rmlStore.getQuads(logicalSource, new NamedNode(NAMESPACES.RML + "iterator"), null));
        String iterator = "*";
        if (iterators.size() > 0) {
            iterator = iterators.get(0).getValue();
        }

        List<Record> res = null;
        for (int i = 0; i < 1; i++)
        {
            sw.start();
            res = extractRecords(parser, iterator);
            sw.stop();
            //System.out.print(sw.getTime() + ",");
            sw.reset();
        }

        return res;
    }

    List<Record> extractRecords(CdParser parser, String iterator) throws Exception {

        List<Record> res = new LinkedList<>();
        List<CdClass> classSelection = new LinkedList<>();

        String[] iteratorWords = iterator.trim().split(" ");
        int whereIndex = -1;
        for (int i = 0; i < iteratorWords.length; i++) {
            if (iteratorWords[i].equals(WHERE)) whereIndex = i;
        }

        int classSelectorIndex = -1;
        String classSelectorString = null;

        if (whereIndex == -1) {
            classSelectorIndex = iteratorWords.length - 1;
        } else {
            classSelectorIndex = whereIndex - 1;
        }

        if (classSelectorIndex < 0) throw new Exception("ClassSelector not found");

        classSelectorString = iteratorWords[classSelectorIndex];

        String[] steps = classSelectorString.split("\\.");
        if (steps.length == 0) {
            throw new Exception("Iterator is malformed: Empty string");
        }

        if (steps[0].equals("*")) { // Add all classes
            Collection<CdClass> classes = parser.getClasses();
            for (CdClass clazz : classes) {
                classSelection.add(clazz);
            }
        } else {
            Collection<CdClass> classes = parser.getClasses();
            for (CdClass curr : classes) {
                if (curr.name.equals(steps[0])) {
                    classSelection.add(curr);
                    break;
                }
            }

            if (classSelection.size() == 0) {
                throw new Exception("There is no class named " + steps[0]);
            }
        }

        for (int i = 1; i < steps.length; i++) {
            LinkedList<CdClass> nextSelection = new LinkedList<>();

            for (CdClass clazz : classSelection) {
                nextSelection.addAll(clazz.extractClasses(steps[i]));
            }

            classSelection = nextSelection;
        }

        for (CdClass clazz : classSelection) {
            // Now see which records were actually requested
            // Case 1: Classes
            if (classSelectorIndex == 0) {
                res.add(new ClassDiagramRecord(clazz));
            } else {
                // Case 2: Attributes
                if (iteratorWords[0].equals("attributes") && iteratorWords[1].equals("of")) {
                    for (CdAttribute attr : clazz.attributes) {
                        res.add(new ClassDiagramRecord(attr));
                    }
                } else {
                    // Case 3: usages or other arrows
                    // Must be arrow - either usages to catch all or specific arrow type
                    List<CdArrow> arrows = null;
                    if (iteratorWords[1].equals(OF)) {
                        arrows = clazz.usedBy;
                    } else {
                        if (iteratorWords[1].equals(BY)) {
                            arrows = clazz.uses;
                        } else {
                            throw new Exception("Second iterator word not 'of' or 'by'");
                        }
                    }

                    if (iteratorWords[0].equals("usages")) {
                        for (CdArrow usage : arrows) {
                            res.add(new ClassDiagramRecord(usage));
                        }
                    } else {
                        CdArrowStyle type = arrowMapping.get(iteratorWords[0]);
                        if (type == null) throw new Exception("Unknown prefix");

                        for (CdArrow usage : arrows) {
                            if (usage.style == type) {
                                res.add(new ClassDiagramRecord(usage));
                            }
                        }
                    }
                }
            }
        }

        // Last step: Filter records according to where-clause
        if (whereIndex != -1) {

            List<Record> filteredRes = new LinkedList<>();

            for (Record r : res) {
                boolean inResult = true;
                for (int i = whereIndex; i < iteratorWords.length; i += 4) {
                    if (!iteratorWords[whereIndex + 2].equals(IS) && !iteratorWords[whereIndex + 2].equals(ISNOT)) {
                        throw new Exception("Missing comparison in where-clause");
                    }
                    if (iteratorWords.length != i + 4 && !(iteratorWords[i + 4].equals(AND) || iteratorWords[i + 4].equals(OR))) {
                        throw new Exception("Where-clause malformed");
                    }

                    String logicalOp = iteratorWords[i];
                    String reference = iteratorWords[i + 1];
                    String comparison = iteratorWords[i + 2];
                    String rhs = iteratorWords[i + 3];

                    if (logicalOp.equals(OR)) {
                        if (inResult) {
                            break;
                        } else {
                            inResult = true;
                        }
                    } else {
                        if (!inResult) {
                            continue;
                        }
                    }

                    if (comparison.equals(IS)) {
                        if (!r.get(reference).equals(Collections.singletonList(rhs))) {
                            inResult = false;
                        }
                    }
                    else {
                        if (r.get(reference).equals(Collections.singletonList(rhs))) {
                            inResult = false;
                        }
                    }
                }
                if (inResult) {
                    filteredRes.add(r);
                }
            }

            res = filteredRes;
        }

        return res;
    }

}
