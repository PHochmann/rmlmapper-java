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
        String iterator = "attributes";
        if (iterators.size() > 0) {
            iterator = iterators.get(0).getValue();
        }

        List<Record> res = new LinkedList<>();

        if (iterator.equals("attributes")) {
            Enumeration<CdAttribute> attribs = parser.getAttributes();
            while (attribs.hasMoreElements()) {
                res.add(new ClassDiagramRecord(attribs.nextElement()));
            }
        } else {
            if (iterator.equals("classes")) {
                Enumeration<CdClass> classes = parser.getClasses();
                while (classes.hasMoreElements()) {
                    res.add(new ClassDiagramRecord(classes.nextElement()));
                }
            } else {
                if (iterator.equals("usages")) {
                    Enumeration<CdUsage> usages = parser.getUsages();
                    while (usages.hasMoreElements()) {
                        res.add(new ClassDiagramRecord(usages.nextElement()));
                    }
                } else {
                    throw new Exception("Unrecognized iterator");
                }
            }
        }

        return res;

    }

}
