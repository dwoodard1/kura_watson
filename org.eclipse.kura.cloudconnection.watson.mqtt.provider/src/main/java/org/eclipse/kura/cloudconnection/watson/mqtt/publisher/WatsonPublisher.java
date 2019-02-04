/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/

package org.eclipse.kura.cloudconnection.watson.mqtt.publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.watson.mqtt.MqttCloudEndpointConstants;
import org.eclipse.kura.cloudconnection.watson.mqtt.WatsonCloudEndpoint;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatsonPublisher implements CloudPublisher, ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(WatsonPublisher.class);

    private WatsonCloudEndpoint cloudConnectionService;
    private ServiceTracker<CloudEndpoint, CloudEndpoint> tracker;

    private WatsonPublisherOptions options;
    private BundleContext bundleContext;

    private final Set<CloudConnectionListener> registeredCloudConnectionStatusListener = new CopyOnWriteArraySet<>();
    private final Set<CloudDeliveryListener> registeredCloudDeliveryListeners = new CopyOnWriteArraySet<>();
    
    /*
     * OSGi Activation Methods
     * 
     */
    public void activate(final ComponentContext context, final Map<String, Object> properties) {
        logger.info("Activating...{}", this.getClass().getSimpleName());

        this.bundleContext = context.getBundleContext();

        updated(properties);

        logger.info("Activating {}...done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("Updating...{}", this.getClass().getSimpleName());

        closeCloudServiceTracker();

        this.options = new WatsonPublisherOptions(properties);

        try {
            reopenCloudServiceTracker();
        } catch (Exception e) {
            logger.warn("Invalid cloud.service.pid value in configuration", e);
        }
        
        logger.info("Updating {}...done", this.getClass().getSimpleName());
    }

    public void deactivate() {
        logger.info("Deactivating...{}", this.getClass().getSimpleName());

        closeCloudServiceTracker();

        logger.info("Deactivating {}...done", this.getClass().getSimpleName());
    }

    /*
     * CloudPublisher Methods
     * 
     */

    @Override
    public String publish(KuraMessage message) throws KuraException {
        synchronized(this) {
            Map<String, Object> publishMessageProps = new HashMap<>();
            publishMessageProps.put(MqttCloudEndpointConstants.TOPIC.name(), this.options.getTopic());
            publishMessageProps.put(MqttCloudEndpointConstants.QOS.name(), this.options.getQos());
            return this.cloudConnectionService.publish(new KuraMessage(message.getPayload(), publishMessageProps));
        }
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.registeredCloudConnectionStatusListener.add(cloudConnectionListener);
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.registeredCloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.registeredCloudConnectionStatusListener.remove(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.registeredCloudDeliveryListeners.remove(cloudDeliveryListener);
    }
    
    /*
     * CloudConnectionListener Methods
     * 
     */
    @Override
    public void onConnectionEstablished() {
        this.registeredCloudConnectionStatusListener.forEach(CloudConnectionListener::onConnectionEstablished);
    }

    @Override
    public void onConnectionLost() {
        this.registeredCloudConnectionStatusListener.forEach(CloudConnectionListener::onConnectionLost);
    }

    @Override
    public void onDisconnected() {
        this.registeredCloudConnectionStatusListener.forEach(CloudConnectionListener::onDisconnected);
    }
    
    /*
     * CloudDeliveryListener Methods
     * 
     */
    @Override
    public void onMessageConfirmed(String messageId) {
        this.registeredCloudDeliveryListeners.forEach(listener -> listener.onMessageConfirmed(messageId));
    }

    /*
     * Private Methods
     */
    private void reopenCloudServiceTracker() throws InvalidSyntaxException {
        closeCloudServiceTracker();

        final String cloudServicePid = this.options.getCloudConnectionServicePid();
        final String filterString = new StringBuilder()
                .append("(&(objectClass=org.eclipse.kura.cloudconnection.CloudEndpoint)(kura.service.pid=")
                .append(cloudServicePid).append("))").toString();

        this.tracker = new ServiceTracker<>(this.bundleContext, FrameworkUtil.createFilter(filterString),
                new CloudServiceTrackerCustomizer());
        this.tracker.open();
    }
    
    private void closeCloudServiceTracker() {
        if (this.tracker != null) {
            this.tracker.close();
            this.tracker = null;
        }
    }

    private final class CloudServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudEndpoint, CloudEndpoint> {

        @Override
        public CloudEndpoint addingService(final ServiceReference<CloudEndpoint> reference) {
            final CloudEndpoint service = WatsonPublisher.this.bundleContext.getService(reference);

            if (service instanceof WatsonCloudEndpoint) {
                setCloudConnectionService((WatsonCloudEndpoint) service);
                logger.info("CloudConnectionService found");
                return service;
            } else {
                logger.warn("configured CloudConnectionService is not a WatsonCloudConnectionService, ignoring it");
                WatsonPublisher.this.bundleContext.ungetService(reference);
            }

            return null;
        }

        @Override
        public void removedService(final ServiceReference<CloudEndpoint> reference, final CloudEndpoint service) {
            unsetCloudConnectionService();
        }

        @Override
        public void modifiedService(ServiceReference<CloudEndpoint> reference, CloudEndpoint service) {
            // no need
        }
        
        private synchronized void setCloudConnectionService(final WatsonCloudEndpoint cConnectionService) {
            cloudConnectionService = cConnectionService;
            cloudConnectionService.registerCloudConnectionListener(WatsonPublisher.this);
        }

        private synchronized void unsetCloudConnectionService() {
            cloudConnectionService.unregisterCloudConnectionListener(WatsonPublisher.this);
            cloudConnectionService = null;
        }
    }
}
