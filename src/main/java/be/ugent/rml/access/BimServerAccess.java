package be.ugent.rml.access;

import org.bimserver.client.BimServerClient;
import org.bimserver.client.json.JsonBimServerClientFactory;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.shared.ChannelConnectionException;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.exceptions.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.ServiceException;
import org.bimserver.shared.exceptions.UserException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

public class BimServerAccess implements Access {

    private String address;
    private String username;
    private String password;
    private String ifcPath;
    private String query;

    public BimServerAccess(String address, String username, String password, String ifcPath, String query) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.ifcPath = ifcPath;
        this.query = query;
    }

    /* Returns new project */
    private SProject checkinFile(BimServerClient client) throws PublicInterfaceNotFoundException, ServerException, UserException {
        try {
            String randomName = "Random " + new Random().nextLong();

            // Create a new project with a random name
            SProject project = client.getServiceInterface().addProject(randomName, "ifc2x3tc1");

            long poid = project.getOid();
            String comment = "This is a comment";

            // This method is an easy way to find a compatible deserializer for the combination of the "ifc" file extension and this project. You can also get a specific deserializer if you want to.
            SDeserializerPluginConfiguration deserializer = client.getServiceInterface().getSuggestedDeserializerForExtension("ifc", poid);

            // Make sure you change this to a path to a local IFC file
            Path demoIfcFile = Paths.get(this.ifcPath);

            client.bulkCheckin(poid, demoIfcFile, comment);

            return project;

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (UserException e) {
            e.printStackTrace();
        }

        return null;
    }

    public InputStream getInputStream() {
        // Connect to server
        // Creating a factory in a try statement, this makes sure the factory will be closed after use
        try (JsonBimServerClientFactory factory = new JsonBimServerClientFactory(this.address)) {
            // Creating a client in a try statement, this makes sure the client will be closed after use
            try (BimServerClient client = factory.create(new UsernamePasswordAuthenticationInfo(username, password))) {
                // Do something with the client
                SProject project = checkinFile(client);

                SSerializerPluginConfiguration serializer = client.getServiceInterface().getSerializerByContentType("application/ifc");

                long topicId = client.getServiceInterface().download(
                        Collections.singleton(project.getLastConcreteRevisionId()),
                        this.query,
                        serializer.getOid(),
                        true); // True for syncing
                return client.getDownloadData(topicId);


            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return null;
    }

    @Override
    public Map<String, String> getDataTypes() {
        return null;
    }
}
