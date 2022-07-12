package kallisto;

import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.eclipse.leshan.client.object.Security.*;
import static org.eclipse.leshan.core.LwM2mId.*;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.demo.LwM2mDemoConstant;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorClient {

    private static final Logger LOG = LoggerFactory.getLogger(EmulatorClient.class);
    private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;

    public static void main(String[] args) {
        try {
            String endpoint = "test" ; // choose an endpoint name
            LwM2mModelRepository repository = createModel();

            final ObjectsInitializer initializer = new ObjectsInitializer(repository.getLwM2mModel());

            //setting bootstrap and server 
            initializer.setInstancesForObject(SECURITY, noSecBootstap("coap://localhost:5783"));
            initializer.setClassForObject(SERVER, Server.class);

            //setting lwm2m objects
            initializer.setInstancesForObject(DEVICE, new MyDevice());
            //initializer.setInstancesForObject(LOCATION, locationInstance);
            initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, new RandomTemperatureSensor());

            List<LwM2mObjectEnabler> enablers = initializer.createAll();

            LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
            builder.setObjects(enablers);

            LeshanClient client = builder.build();
            client.start();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            LOG.error(sStackTrace);
        }
    }

    private static LwM2mModelRepository createModel() throws Exception {

        List<ObjectModel> models = ObjectLoader.loadAllDefault();

        String[] modelPaths = new String[] {"3303.xml"};
        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));

        //models.addAll(ObjectLoader.loadObjectsFromDir(folder, true));

        return new LwM2mModelRepository(models);
    }
}