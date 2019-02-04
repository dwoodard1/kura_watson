/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/

package org.eclipse.kura.cloudconnection.watson.mqtt.publisher;

import java.util.Map;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;

public class WatsonPublisherOptions {
    
    private static final Property<String> CLOUD_CONNECTION_SERVICE_PID_PROPERTY = new Property<>(
            CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), "");
    private static final Property<String> PROPERTY_TOPIC = new Property<>("topic", "iot-2/evt/status/fmt/json");
    private static final Property<Integer> PROPERTY_QOS = new Property<>("qos", 0);
    
    private final String cloudConnectionServicePid;
    private final String topic;
    private final int qos;
    
    public WatsonPublisherOptions(final Map<String, Object> properties) {
        this.cloudConnectionServicePid = CLOUD_CONNECTION_SERVICE_PID_PROPERTY.get(properties);
        this.topic = PROPERTY_TOPIC.get(properties);
        this.qos = PROPERTY_QOS.get(properties);
    }
    
    public String getCloudConnectionServicePid() {
        return this.cloudConnectionServicePid;
    }
    
    public String getTopic() {
        return this.topic;
    }
    
    public int getQos() {
        return this.qos;
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
