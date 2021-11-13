package be.ugent.rml.records.classDiagram;

import be.ugent.rml.access.Access;
import be.ugent.rml.records.Record;
import be.ugent.rml.records.ReferenceFormulationRecordFactory;
import be.ugent.rml.store.QuadStore;
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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ClassDiagramRecordFactory implements ReferenceFormulationRecordFactory {

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws Exception {

        InputStream stream = access.getInputStream();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xml_doc = builder.parse(stream);

        CdParser parser = new CdParser(xml_doc);
        parser.parseClassDiagram();

        return null;

    }

}
