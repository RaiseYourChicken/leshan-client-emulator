package kallisto;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.PrintWriter;
import java.io.StringWriter;

import kallisto.sensors.*;

import static org.eclipse.leshan.client.object.Security.*;
import static org.eclipse.leshan.core.LwM2mId.*;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mValueChecker;
import org.eclipse.leshan.core.node.codec.NodeEncoder;

import org.eclipse.leshan.core.node.codec.text.LwM2mNodeTextEncoder;
import org.eclipse.leshan.core.request.ContentFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorClient {

    private static final Logger LOG = LoggerFactory.getLogger(EmulatorClient.class);
    private static final int OBJECT_ID_TEMPERATURE_SENSOR = 3303;
    private static final int OBJECT_ID_HUMIDITY_SENSOR = 3304;
    private static final int OBJECT_ID_BAROMETER_SENSOR = 3315;
    private static final int OBJECT_ID_LOUDNESS = 3324;
    private static final int OBJECT_ID_GPS = 3336;
    private static final int OBJECT_ID_PARTICULATES_SENSOR = 10314;

    public static void main(String[] args) {
        try {
            String endpoint = "test";
            if(args.length != 0){
                endpoint = args[0];
            }
            LwM2mModelRepository repository = createModel();

            final ObjectsInitializer initializer = new ObjectsInitializer(repository.getLwM2mModel());

            // setting bootstrap and server
            initializer.setInstancesForObject(SECURITY, noSecBootstap("coap://localhost:5783"));
            initializer.setClassForObject(SERVER, Server.class);

            // setting lwm2m objects
            initializer.setInstancesForObject(DEVICE, new MyDevice());
            // initializer.setInstancesForObject(LOCATION, locationInstance);
            initializer.setInstancesForObject(OBJECT_ID_TEMPERATURE_SENSOR, new RandomTemperatureSensor());
            initializer.setDefaultContentFormat(OBJECT_ID_TEMPERATURE_SENSOR, ContentFormat.TEXT);
            initializer.setInstancesForObject(OBJECT_ID_HUMIDITY_SENSOR, new RandomHumiditySensor());
            initializer.setDefaultContentFormat(OBJECT_ID_HUMIDITY_SENSOR, ContentFormat.TEXT);
            initializer.setInstancesForObject(OBJECT_ID_BAROMETER_SENSOR, new RandomBarometerSensor());
            initializer.setDefaultContentFormat(OBJECT_ID_BAROMETER_SENSOR, ContentFormat.TEXT);
            initializer.setInstancesForObject(OBJECT_ID_LOUDNESS, new RandomLoudnessGenerator());
            initializer.setDefaultContentFormat(OBJECT_ID_LOUDNESS, ContentFormat.TEXT);
            initializer.setInstancesForObject(OBJECT_ID_GPS, new RandomGPSGenerator());
            initializer.setDefaultContentFormat(OBJECT_ID_GPS, ContentFormat.TEXT);
            initializer.setInstancesForObject(OBJECT_ID_PARTICULATES_SENSOR, new RandomParticulatesSensor());
            initializer.setDefaultContentFormat(OBJECT_ID_PARTICULATES_SENSOR, ContentFormat.TEXT);

            List<LwM2mObjectEnabler> enablers = initializer.createAll();

            LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
            builder.setObjects(enablers);

            // Configure Registration Engine
            DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();

            engineFactory.setCommunicationPeriod(3600 * 1000);
            engineFactory.setReconnectOnUpdate(true);
            engineFactory.setResumeOnConnect(true);
            engineFactory.setQueueMode(false);

            builder.setRegistrationEngineFactory(engineFactory);
            // Support old format
            builder.setDecoder(new DefaultLwM2mDecoder(true));
            builder.setEncoder(new DefaultLwM2mEncoder(true));

            // This sets the encoders as text only, but default content format is oma TLV
            // right now and bootstrap server just uses it as default too, would need to
            // code a bootstrap server that forces text maybe?
            // Map<ContentFormat, NodeEncoder> encoders = new HashMap<>();
            // encoders.put(ContentFormat.TEXT, new LwM2mNodeTextEncoder());
            // builder.setEncoder(new DefaultLwM2mEncoder(encoders,
            // DefaultLwM2mEncoder.getDefaultPathEncoder(),
            // new LwM2mValueChecker()));

            LeshanClient client = builder.build();
            client.start();

            // De-register on shutdown and stop client.
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    client.destroy(true); // send de-registration request before destroy
                }
            });

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

        String[] modelPaths = new String[] { "3303.xml", "3304.xml", "3315.xml", "3324.xml", "3336.xml", "10314.xml" };
        models.addAll(ObjectLoader.loadDdfResources("/models", modelPaths));

        // models.addAll(ObjectLoader.loadObjectsFromDir(folder, true));

        return new LwM2mModelRepository(models);
    }
}