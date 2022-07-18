package kallisto.sensors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomGPSGenerator extends BaseInstanceEnabler implements Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(RandomGPSGenerator.class);

    private static final int NUMERIC_LATITUDE = 6051;
    private static final int NUMERIC_LONGITUDE = 6052;
    private static final int NUMERIC_UNCERTAINTY = 6053;
    private static final List<Integer> supportedResources = Arrays.asList(NUMERIC_LATITUDE, NUMERIC_LONGITUDE,
            NUMERIC_UNCERTAINTY);
    private final ScheduledExecutorService scheduler;
    private final Random rng = new Random();
    private double currentLatitude = 20d;
    private double currentLongitude = 20d;

    public RandomGPSGenerator() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("GPS Generator"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                adjustGPS();
            }
        }, 2, 20, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on GPS resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case NUMERIC_LATITUDE:
                return ReadResponse.success(resourceId, getTwoDigitValue(currentLatitude));
            case NUMERIC_LONGITUDE:
                return ReadResponse.success(resourceId, getTwoDigitValue(currentLongitude));
            case NUMERIC_UNCERTAINTY:
                return ReadResponse.success(resourceId, getTwoDigitValue(10d));
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, Arguments arguments) {
        LOG.info("Execute on GPS resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            default:
                return super.execute(identity, resourceId, arguments);
        }
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustGPS() {
        float deltaLatitude = (rng.nextInt(20) - 10) / 10f;
        float deltaLongitude = (rng.nextInt(20) - 10) / 10f;
        currentLatitude += deltaLatitude;
        currentLongitude += deltaLongitude;

        fireResourceChange(NUMERIC_LATITUDE);
        fireResourceChange(NUMERIC_LONGITUDE);

    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }
}