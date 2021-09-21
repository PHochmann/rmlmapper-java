package be.ugent.rml.records;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.Literal;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class IFCRecordFactory implements ReferenceFormulationRecordFactory {

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws IOException, SQLException, ClassNotFoundException {
        // Interpret result as stream of text
        try (InputStream queryResult = access.getInputStream()) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(queryResult, baos);
            System.out.println(baos.size() + " bytes downloaded");

            String file_str = new String( baos.toByteArray(), StandardCharsets.UTF_8 );

            List<Record> res = new ArrayList<>();

            BufferedReader bufReader = new BufferedReader(new StringReader(file_str));

            String line = null;
            while( (line=bufReader.readLine()) != null )
            {
                if (line.startsWith("#")) res.add(new IFCRecord(line));
            }

            return res;
        }
    }

}
