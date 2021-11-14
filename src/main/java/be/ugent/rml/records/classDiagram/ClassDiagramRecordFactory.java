package be.ugent.rml.records.classDiagram;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.records.Record;
import be.ugent.rml.records.ReferenceFormulationRecordFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.jena.atlas.lib.Pair;
import org.rdfhdt.hdt.dictionary.impl.DictionaryIDMapping;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

public class ClassDiagramRecordFactory implements ReferenceFormulationRecordFactory {

    static final String ATTRIBUTES_OF = "attributes of ";
    static final String USAGES_BY = "usages by ";
    static final String USAGES_OF = "usages of ";

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

        String classSelectorString = iterator;
        int whatsWanted = 0; // 0: Classes, 1: Attributes, 2: Uses, 3: UsedBy

        if (iterator.startsWith(ATTRIBUTES_OF)) {
            classSelectorString = classSelectorString.substring(ATTRIBUTES_OF.length());
            whatsWanted = 1;
        } else {
            if (iterator.startsWith(USAGES_BY)) {
                classSelectorString = classSelectorString.substring(USAGES_BY.length());
                whatsWanted = 2;
            } else {
                if (iterator.startsWith(USAGES_OF)) {
                    classSelectorString = classSelectorString.substring(USAGES_OF.length());
                    whatsWanted = 3;
                }
            }
        }

        String[] steps = classSelectorString.split("\\.");
        if (steps.length == 0) {
            throw new Exception("Iterator is malformed: Empty string");
        }

        if (steps[0].equals("*")) { // Add all classes
            Enumeration<CdClass> classes = parser.getClasses();
            while (classes.hasMoreElements()) {
                classSelection.add(classes.nextElement());
            }
        } else {
            Enumeration<CdClass> classes = parser.getClasses();
            while (classes.hasMoreElements()) {
                CdClass curr = classes.nextElement();
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
            if (whatsWanted == 0) {
                res.add(new ClassDiagramRecord(clazz));
            } else {
                if (whatsWanted == 1) {
                    for (CdAttribute attr : clazz.attributes) {
                        res.add(new ClassDiagramRecord(attr));
                    }
                } else {
                    if (whatsWanted == 2) {
                        for (CdUsage usage : clazz.uses) {
                            res.add(new ClassDiagramRecord(usage));
                        }
                    } else {
                        for (CdUsage usage : clazz.usedBy) {
                            res.add(new ClassDiagramRecord(usage));
                        }
                    }
                }
            }
        }

        return res;

    }

}
