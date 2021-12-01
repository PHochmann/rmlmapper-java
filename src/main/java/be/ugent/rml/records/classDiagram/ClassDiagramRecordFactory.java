package be.ugent.rml.records.classDiagram;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.records.Record;
import be.ugent.rml.records.ReferenceFormulationRecordFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.rdfhdt.hdt.iterator.utils.Iter;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class ClassDiagramRecordFactory implements ReferenceFormulationRecordFactory {

    final String OF = "of";
    final String BY = "by";

    Dictionary<String, CdArrowType> arrowMapping;

    public ClassDiagramRecordFactory() {
        arrowMapping = new Hashtable<>();
        arrowMapping.put("associations", CdArrowType.CD_ARROW_ASSOCIATION);
        arrowMapping.put("aggregations", CdArrowType.CD_ARROW_AGGREGATION);
        arrowMapping.put("compositions", CdArrowType.CD_ARROW_COMPOSITION);
        arrowMapping.put("dependencies", CdArrowType.CD_ARROW_DEPENDENCY);
    }

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws Exception {

        InputStream stream = access.getInputStream();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xml_doc = builder.parse(stream);
        CdParser parser = new CdParser(xml_doc);
        parser.parseClassDiagram();

        // Now that the xml-Document is parsed to a bunch of Class-Diagram classes, process iterator
        List<Term> iterators = Utils.getObjectsFromQuads(rmlStore.getQuads(logicalSource, new NamedNode(NAMESPACES.RML + "iterator"), null));
        String iterator = "*";
        if (iterators.size() > 0) {
            iterator = iterators.get(0).getValue();
        }

        List<Record> res = new LinkedList<>();
        List<CdClass> classSelection = new LinkedList<>();

        String[] iteratorWords = iterator.trim().split(" ");

        String classSelectorString = iteratorWords[iteratorWords.length - 1];
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
            if (iteratorWords.length == 1) {
                res.add(new ClassDiagramRecord(clazz));
            } else {
                if (iteratorWords.length == 3) {
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
                            CdArrowType type = arrowMapping.get(iteratorWords[0]);
                            for (CdArrow usage : arrows) {
                                if (usage.type == type) {
                                    res.add(new ClassDiagramRecord(usage));
                                }
                            }
                        }
                    }
                } else {
                    throw new Exception("Iterator malformed: Not 1 or 3 words");
                }
            }
        }

        return res;

    }

}
