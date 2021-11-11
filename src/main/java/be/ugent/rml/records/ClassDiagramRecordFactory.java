package be.ugent.rml.records;

import be.ugent.rml.access.Access;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.Term;
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

    class CdClass {

    }

    class CdAttribute {

    }

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws SQLException, IOException, ClassNotFoundException, ParserConfigurationException, SAXException, XPathExpressionException {

        InputStream stream = access.getInputStream();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xml_doc = builder.parse(stream);

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/mxfile/diagram/mxGraphModel/root/mxCell";
        NodeList cells = (NodeList) xpath.evaluate(expression, xml_doc, XPathConstants.NODESET);

        parseClassDiagram(cells);

        return null;

    }

    public List<Object> parseClassDiagram(NodeList nodes) {

        Dictionary<String, Node> cell_dict = new Hashtable<>();
        Dictionary<String, Node> classes  = new Hashtable<>();
        Dictionary<String, >

        for (int i = 0; i < nodes.getLength(); i++)
        {
            cell_dict.put(nodes.item(i).getAttributes().getNamedItem("id").getTextContent(), nodes.item(i));
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            Node cell = nodes.item(i);
            Node style = cell.getAttributes().getNamedItem("style");
            if (style != null) {
                String style_text = style.getTextContent();
                if (style_text.startsWith("swimlane")) {
                    // We have a class box
                }
                else
                {
                    // No class box - attribute or arrow
                }
            }
        }
        return null;
    }

}
