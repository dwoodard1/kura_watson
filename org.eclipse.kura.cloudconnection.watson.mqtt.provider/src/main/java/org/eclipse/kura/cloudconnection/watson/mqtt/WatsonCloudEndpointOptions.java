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

import java.util.Map;

public class WatsonCloudEndpointOptions {

    private static final Property<Boolean> PUBLISH_DEVICE_INFO = new Property<>("publish.device.info", true);
    private static final Property<Boolean> REPUBLISH_POSITION_ON_GPS_LOCK = new Property<>(
            "republish.position.on.gps.lock", true);
    private static final Property<Boolean> REPUBLISH_MODEM_INFO_ON_MODEM_DETECT = new Property<>(
            "republish.modem.info.on.modem.detect", true);
    private static final Property<String> DISPLAY_NAME_MODE = new Property<>("device.display.name", "HOSTNAME");
    private static final Property<String> CUSTOM_DISPLAY_NAME = new Property<>("device.custom.name", "kura-gateway");
    private static final Property<String> DEVICE_TYPE = new Property<>("device.type", "Watson_MQTTDevice");

    private final boolean publishDeviceInfo;
    private final boolean republishPositionOnGpsLock;
    private final boolean republishModemInfoOnModemDetect;
    private final DisplayNameMode displayNameMode;
    private final String deviceCustomName;
    private final String deviceType;

    public WatsonCloudEndpointOptions(final Map<String, Object> properties) {
        this.publishDeviceInfo = PUBLISH_DEVICE_INFO.get(properties);
        this.republishPositionOnGpsLock = REPUBLISH_POSITION_ON_GPS_LOCK.get(properties);
        this.republishModemInfoOnModemDetect = REPUBLISH_MODEM_INFO_ON_MODEM_DETECT.get(properties);
        this.deviceCustomName = CUSTOM_DISPLAY_NAME.get(properties);
        this.displayNameMode = DisplayNameMode.valueOf(DISPLAY_NAME_MODE.get(properties));
        this.deviceType = DEVICE_TYPE.get(properties);
    }

    public boolean shouldPublishDeviceInfo() {
        return this.publishDeviceInfo;
    }

    public boolean shouldRepublishPositionOnGpsLock() {
        return this.republishPositionOnGpsLock;
    }

    public boolean shouldRepublishModemInfoOnModemDetect() {
        return this.republishModemInfoOnModemDetect;
    }

    public DisplayNameMode getDisplayNameMode() {
        return this.displayNameMode;
    }

    public String getCustomDisplayName() {
        return this.deviceCustomName;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public enum DisplayNameMode {
        DEVICE_NAME,
        HOSTNAME,
        CUSTOM
    }

    private static final class Property<T> {

        private final String key;
        private final T defaultValue;

        public Property(final String key, final T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(final Map<String, Object> properties) {
            final Object value = properties.get(this.key);

            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return this.defaultValue;
        }
    }
}
