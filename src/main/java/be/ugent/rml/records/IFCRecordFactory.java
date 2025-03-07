package be.ugent.rml.records;

import be.ugent.rml.NAMESPACES;
import be.ugent.rml.Utils;
import be.ugent.rml.access.Access;
import be.ugent.rml.access.BimServerAccess;
import be.ugent.rml.functions.ExecutionParser;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.term.Literal;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang.time.StopWatch;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.bimserver.client.BimServerClient;
import org.bimserver.client.ClientIfcModel;
import org.bimserver.emf.IdEObject;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class IFCRecordFactory implements ReferenceFormulationRecordFactory {

    @Override
    public List<Record> getRecords(Access access, Term logicalSource, QuadStore rmlStore) throws IOException, SQLException, ClassNotFoundException {
        // Interpret result as stream of text
        StopWatch sw = new StopWatch();
        sw.start();
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

            ClientIfcModel model = client.getModel(query_project, query_project.getLastRevisionId(), true, false, ((BimServerAccess)access).includeGeometry());

            sw.stop();
            System.out.println("Filtering in: " + sw.getTime() + " ms");
            sw.reset();
            sw.start();

            List<Record> query_records = new ArrayList<>();

            // Now is the time to check if we need to invoke a filtering function
            List<Term> mappings = Utils.getObjectsFromQuads(rmlStore.getQuads(logicalSource, new NamedNode(NAMESPACES.IFCRML + "iteratorMapping"), null));
            List<Term> executions = Utils.getObjectsFromQuads(rmlStore.getQuads(logicalSource, new NamedNode(NAMESPACES.IFCRML + "iteratorExecution"), null));

            if (!mappings.isEmpty() && !executions.isEmpty()) {
                Method fn_method = ExecutionParser.getMethod(rmlStore, mappings.get(0));

                Pair<List<Class<?>>, List<Object>> fn_params_and_classes = ExecutionParser.parseParamsFromExecution(rmlStore, executions.get(0));
                List<Class<?>> classes = fn_params_and_classes.getLeft();
                List<Object> params = fn_params_and_classes.getRight();

                params.add(0, model); // Inject current IFC file into params
                params.add(1, client);
                params.add(2, query_project.getLastRevisionId());

                try {
                    query_records = (List<Record>)fn_method.invoke(null, params.toArray(new Object[0])); // null as first arg because it's static method
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                for (IdEObject obj : model) {
                    query_records.add(new IFCRecord(obj));
                }
            }

            sw.stop();
            System.out.println("Else in: " + sw.getTime() + " ms");
            return query_records;

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (UserException e) {
            e.printStackTrace();
        }

        return null;
    }

}
