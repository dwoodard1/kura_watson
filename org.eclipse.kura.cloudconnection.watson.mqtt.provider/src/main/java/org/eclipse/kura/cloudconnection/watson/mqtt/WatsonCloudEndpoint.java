/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/

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
    private WatsonCloudEndpointOptions options;
    
    private String imei;
    private String iccid;
    private String imsi;

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
        
        if (super.getDataService().isConnected()) {
            onConnectionEstablished();
        }
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
        postConnectionStateChangeEvent(true);
        super.onConnectionEstablished();
    }
    
    @Override
    public void onConnectionLost(Throwable cause) {
        super.onConnectionLost(cause);
        postConnectionStateChangeEvent(false);
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        postConnectionStateChangeEvent(false);
    }

    /*
     * EventHandler Methods
     * 
     */
    @Override
    public void handleEvent(Event event) {
        if (PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC.contains(event.getTopic())) {
            // if we get a position locked event,
            // republish the birth certificate only if we are configured to
            logger.info("Handling PositionLockedEvent");
            if (isConnected() && this.options.shouldRepublishPositionOnGpsLock()) {
                try {
                    publishPosition();
                } catch (Exception e) {
                    logger.warn("Cannot publish position", e);
                }
            }
        } else if (ModemReadyEvent.MODEM_EVENT_READY_TOPIC.contains(event.getTopic())) {
            logger.info("Handling ModemReadyEvent");
            ModemReadyEvent modemReadyEvent = (ModemReadyEvent) event;
            // keep these identifiers around until we can publish the certificate
            this.imei = (String) modemReadyEvent.getProperty(ModemReadyEvent.IMEI);
            this.imsi = (String) modemReadyEvent.getProperty(ModemReadyEvent.IMSI);
            this.iccid = (String) modemReadyEvent.getProperty(ModemReadyEvent.ICCID);

            if (isConnected() && this.options.shouldRepublishModemInfoOnModemDetect()) {
                if (!((this.imei == null || this.imei.length() == 0 || this.imei.equals("ERROR"))
                        && (this.imsi == null || this.imsi.length() == 0 || this.imsi.equals("ERROR"))
                        && (this.iccid == null || this.iccid.length() == 0 || this.iccid.equals("ERROR")))) {
                    logger.debug("handleEvent() :: publishing position ...");
                    try {
                        publishModemInfo();
                    } catch (Exception e) {
                        logger.warn("Cannot publish modem info", e);
                    }
                }
            }
        }
    }

    /*
     * Private Methods
     * 
     */
    private void publishDeviceInfo() {
        // TODO: Implement birth information for Watson
    }
    
    private void publishPosition() {
        // TODO: Implement
    }
    
    private void publishModemInfo() {
     // TODO: Implement
    }

    private void postConnectionStateChangeEvent(final boolean isConnected) {

        final Map<String, Object> eventProperties = Collections.singletonMap(CONNECTION_EVENT_PID_PROPERTY_KEY,
                (String) this.ctx.getProperties().get(ConfigurationService.KURA_SERVICE_PID));

        final Event event = isConnected ? new CloudConnectionEstablishedEvent(eventProperties)
                : new CloudConnectionLostEvent(eventProperties);
        this.eventAdmin.postEvent(event);
    }
}
