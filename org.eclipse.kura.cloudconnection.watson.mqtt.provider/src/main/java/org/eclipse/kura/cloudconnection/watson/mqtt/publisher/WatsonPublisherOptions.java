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
package org.eclipse.kura.cloudconnection.watson.mqtt.publisher;

import java.util.Map;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;

public class WatsonPublisherOptions {
    
    private static final Property<String> CLOUD_CONNECTION_SERVICE_PID_PROPERTY = new Property<>(
            CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), "");
    
    private final String cloudConnectionServicePid;
    
    public WatsonPublisherOptions(final Map<String, Object> properties) {
        this.cloudConnectionServicePid = CLOUD_CONNECTION_SERVICE_PID_PROPERTY.get(properties);
    }
    
    public String getCloudConnectionServicePid() {
        return this.cloudConnectionServicePid;
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
