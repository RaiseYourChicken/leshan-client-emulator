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

public class RandomLoudnessGenerator extends BaseInstanceEnabler implements Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(RandomLoudnessGenerator.class);

    private static final String UNIT_DB = "dB";
    private static final int SENSOR_VALUE = 5700;
    private static final int UNITS = 5701;
    private static final int MAX_MEASURED_VALUE = 5602;
    private static final int MIN_MEASURED_VALUE = 5601;
    private static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    private static final List<Integer> supportedResources = Arrays.asList(SENSOR_VALUE, UNITS, MAX_MEASURED_VALUE,
            MIN_MEASURED_VALUE, RESET_MIN_MAX_MEASURED_VALUES);
    private final ScheduledExecutorService scheduler;
    private final Random rng = new Random();
    private double currentLoudness = -20d;
    private double minMeasuredValue = currentLoudness;
    private double maxMeasuredValue = currentLoudness;

    public RandomLoudnessGenerator() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Loudness Generator"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                adjustLoudness();
            }
        }, 2, 20, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on Loudness resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case MIN_MEASURED_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case MAX_MEASURED_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case SENSOR_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(currentLoudness));
            case UNITS:
                return ReadResponse.success(resourceId, UNIT_DB);
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, Arguments arguments) {
        LOG.info("Execute on Loudness resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case RESET_MIN_MAX_MEASURED_VALUES:
                resetMinMaxMeasuredValues();
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, arguments);
        }
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustLoudness() {
        float delta = (rng.nextInt(20) - 10) / 10f;
        currentLoudness += delta;
        Integer changedResource = adjustMinMaxMeasuredValue(currentLoudness);
        if (changedResource != null) {
            fireResourcesChange(getResourcePath(SENSOR_VALUE), getResourcePath(changedResource));
        } else {
            fireResourceChange(SENSOR_VALUE);
        }
    }

    private synchronized Integer adjustMinMaxMeasuredValue(double newLoudness) {
        if (newLoudness > maxMeasuredValue) {
            maxMeasuredValue = newLoudness;
            return MAX_MEASURED_VALUE;
        } else if (newLoudness < minMeasuredValue) {
            minMeasuredValue = newLoudness;
            return MIN_MEASURED_VALUE;
        } else {
            return null;
        }
    }

    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentLoudness;
        maxMeasuredValue = currentLoudness;
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