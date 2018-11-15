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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class Constants {

    public static final List<String> DEFAULT_STACK_COMPONENT_PIDS;

    public static final String CLOUD_ENDPOINT_FACTORY_PID = "org.eclipse.kura.cloudconnection.watson.mqtt.WatsonCloudEndpoint";

    public static final Pattern MANAGED_CLOUD_ENDPOINT_SERVICE_PID_PATTERN = Pattern
            .compile("^org.eclipse.kura.cloud.watson.mqtt.WatsonCloudEndpoint(-[a-zA-Z0-9]+)*$");

    public static final String CLOUD_ENDPOINT_SERVICE_FILTER_TEMPLATE = "(service.factoryPid="
            + CLOUD_ENDPOINT_FACTORY_PID + ")";

    public static final String WATSON_CLOUD_CONNECTION_SERVICE_FACTORY_PID = "org.eclipse.kura.cloudconnection.watson.mqtt.WatsonCloudConnectionFactory";

    static {
        final List<String> defaultStackComponentPids = new ArrayList<>(3);
        defaultStackComponentPids.add("org.eclipse.kura.cloud.watson.mqtt.WatsonCloudEndpoint");
        defaultStackComponentPids.add("org.eclipse.kura.cloud.watson.mqtt.DataService");
        defaultStackComponentPids.add("org.eclipse.kura.cloud.watson.mqtt.MQTTDataTransport");
        DEFAULT_STACK_COMPONENT_PIDS = Collections.unmodifiableList(defaultStackComponentPids);
    }

    private Constants() {
    }
}
