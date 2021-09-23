package be.ugent.rml.records;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.access.BimServerAccess;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.Literal;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVParser;
import org.bimserver.client.BimServerClient;
import org.bimserver.client.ClientIfcModel;
import org.bimserver.emf.IdEObject;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;

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

            String file_str = new String(baos.toByteArray(), StandardCharsets.UTF_8 );
            String random_name = "temp" + new Random().nextLong() + ".txt";

            try (PrintWriter out = new PrintWriter(random_name)) {
                out.println(file_str);
            }

            BimServerClient client = ((BimServerAccess)access).getClient();

            // Now check in query result as new project
            SProject query_project = BimServerAccess.checkinFile(random_name, ((BimServerAccess)access).getFormat(), client);

            query_project = client.getServiceInterface().getProjectByPoid(query_project.getOid());

            ClientIfcModel model = client.getModel(query_project, query_project.getLastRevisionId(), true, false, false);

            List<Record> query_records = new ArrayList<>();

            for (IdEObject obj : model) {
                query_records.add(new IFCRecord(obj));
            }

            return query_records;

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (UserException e) {
            e.printStackTrace();
        }

        return null;
    }

}
