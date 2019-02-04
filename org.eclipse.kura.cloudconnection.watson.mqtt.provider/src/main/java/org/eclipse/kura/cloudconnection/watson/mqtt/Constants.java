/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/

package org.eclipse.kura.cloudconnection.watson.mqtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class Constants {

    public static final List<String> DEFAULT_STACK_COMPONENT_PIDS;

    public static final String CLOUD_ENDPOINT_FACTORY_PID = "org.eclipse.kura.cloudconnection.watson.mqtt.WatsonCloudEndpoint";

    public static final Pattern MANAGED_CLOUD_ENDPOINT_SERVICE_PID_PATTERN = Pattern
            .compile("^org.eclipse.kura.cloudconnection.watson.mqtt.WatsonCloudEndpoint(-[a-zA-Z0-9]+)*$");

    public static final String CLOUD_ENDPOINT_SERVICE_FILTER_TEMPLATE = "(service.factoryPid="
            + CLOUD_ENDPOINT_FACTORY_PID + ")";

    public static final String WATSON_CLOUD_CONNECTION_SERVICE_FACTORY_PID = "org.eclipse.kura.cloudconnection.watson.mqtt.WatsonCloudConnectionFactory";

    static {
        final List<String> defaultStackComponentPids = new ArrayList<>(3);
        defaultStackComponentPids.add(CLOUD_ENDPOINT_FACTORY_PID);
        defaultStackComponentPids.add("org.eclipse.kura.cloudconnection.watson.mqtt.DataService");
        defaultStackComponentPids.add("org.eclipse.kura.cloudconnection.watson.mqtt.MQTTDataTransport");
        DEFAULT_STACK_COMPONENT_PIDS = Collections.unmodifiableList(defaultStackComponentPids);
    }

    private Constants() {
    }
}
