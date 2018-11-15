/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      Eurotech
 ******************************************************************************/
package org.eclipse.kura.cloudconnection.watson.mqtt;

import java.util.Collections;
import java.util.Map;

import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatsonCloudEndpoint extends MqttCloudEndpoint implements ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(WatsonCloudEndpoint.class);

    private static final String CONNECTION_EVENT_PID_PROPERTY_KEY = "cloud.service.pid";

    private EventAdmin eventAdmin;

    private ComponentContext ctx;
    private boolean deviceInfoPublished;
    private WatsonCloudEndpointOptions options;

    /*
     * Dependencies
     * 
     */
    public void bindDataService(DataService dataService) {
        super.bindDataServiceInternal(dataService);
    }

    public void unbindDataService(DataService dataService) {
        super.unbindDataServiceInternal();
    }

    public void bindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unbindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    /*
     * OSGi Activation Methods
     * 
     */
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activate {}...", properties.get(ConfigurationService.KURA_SERVICE_PID));

        //
        // save the bundle context and the properties
        this.ctx = componentContext;
        this.options = new WatsonCloudEndpointOptions(properties);

        super.activateInternal();
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated {}...: {}", properties.get(ConfigurationService.KURA_SERVICE_PID), properties);

        // Update properties and re-publish Birth certificate
        this.options = new WatsonCloudEndpointOptions(properties);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivate {}...", componentContext.getProperties().get(ConfigurationService.KURA_SERVICE_PID));

        super.deactivateInternal();
    }

    /*
     * MqttCloudEndpoint Methods
     * 
     */
    @Override
    public void onConnectionEstablished() {
        if (!this.deviceInfoPublished) {
            try {
                publishDeviceInfo();
                this.deviceInfoPublished = true;
            } catch (Exception e) {
                logger.warn("Failed to publish device info", e);
            }
        }

        postConnectionStateChangeEvent(true);
        super.onConnectionEstablished();
    }

    /*
     * EventHandler Methods
     * 
     */
    @Override
    public void handleEvent(Event event) {
        if (PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC.contains(event.getTopic())) {
            // TODO: Publish data on GPS lock?
        } else if (ModemReadyEvent.MODEM_EVENT_READY_TOPIC.contains(event.getTopic())) {
            // TODO: Publish data on Modem discovery?
        }
    }

    /*
     * Private Methods
     * 
     */
    private void publishDeviceInfo() {
        // TODO: Implement birth information for Watson
    }

    private void postConnectionStateChangeEvent(final boolean isConnected) {

        final Map<String, Object> eventProperties = Collections.singletonMap(CONNECTION_EVENT_PID_PROPERTY_KEY,
                (String) this.ctx.getProperties().get(ConfigurationService.KURA_SERVICE_PID));

        final Event event = isConnected ? new CloudConnectionEstablishedEvent(eventProperties)
                : new CloudConnectionLostEvent(eventProperties);
        this.eventAdmin.postEvent(event);
    }
}
