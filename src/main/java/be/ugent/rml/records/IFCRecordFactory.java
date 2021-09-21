package be.ugent.rml.records;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.Literal;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.commons.csv.CSVParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class IFCRecordFactory implements ReferenceFormulationRecordFactory {

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws IOException, SQLException, ClassNotFoundException {
        // Interpret result as stream of text
        try (InputStream queryResult = access.getInputStream()) {
            List<String> doc =
                    new BufferedReader(new InputStreamReader(queryResult,
                            StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

            List<Record> result = new ArrayList<>();
            for (String str : doc) {
                result.add(new IFCRecord(str));
            }

            return result;
        }


    }

}
