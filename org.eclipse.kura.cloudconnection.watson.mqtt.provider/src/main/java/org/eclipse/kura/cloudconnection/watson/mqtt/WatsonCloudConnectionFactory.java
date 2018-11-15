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

import static org.eclipse.kura.cloudconnection.watson.mqtt.Constants.DEFAULT_STACK_COMPONENT_PIDS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

public class WatsonCloudConnectionFactory implements CloudConnectionFactory {

    private static final String DATA_SERVICE_FACTORY_PID = "org.eclipse.kura.data.DataService";
    private static final String DATA_TRANSPORT_SERVICE_FACTORY_PID = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

    private static final String DATA_SERVICE_REFERENCE_NAME = "DataService";
    private static final String DATA_TRANSPORT_SERVICE_REFERENCE_NAME = "DataTransportService";

    private static final String REFERENCE_TARGET_VALUE_FORMAT = "(" + ConfigurationService.KURA_SERVICE_PID + "=%s)";

    private ConfigurationService configurationService;
    private BundleContext context;

    public void bindConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unbindConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = null;
    }

    public void activate(final ComponentContext context) {
        this.context = context.getBundleContext();
    }

    @Override
    public void createConfiguration(final String pid) throws KuraException {
        String suffix = getSuffix(pid);

        if (suffix == null) {
            suffix = "";
        }

        final String dataServicePid = DEFAULT_STACK_COMPONENT_PIDS.get(1) + suffix;
        final String dataTransportServicePid = DEFAULT_STACK_COMPONENT_PIDS.get(2) + suffix;

        final Map<String, Object> cloudEndpointProperties = new HashMap<>();

        cloudEndpointProperties.put(DATA_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX,
                String.format(REFERENCE_TARGET_VALUE_FORMAT, dataServicePid));
        cloudEndpointProperties.put(CloudConnectionConstants.CLOUD_CONNECTION_FACTORY_PID_PROP_NAME.value(),
                Constants.WATSON_CLOUD_CONNECTION_SERVICE_FACTORY_PID);

        this.configurationService.createFactoryConfiguration(Constants.CLOUD_ENDPOINT_FACTORY_PID, pid,
                cloudEndpointProperties, false);

        final Map<String, Object> dataServiceProperties = Collections.singletonMap(
                DATA_TRANSPORT_SERVICE_REFERENCE_NAME + ComponentConstants.REFERENCE_TARGET_SUFFIX,
                String.format(REFERENCE_TARGET_VALUE_FORMAT, dataTransportServicePid));

        this.configurationService.createFactoryConfiguration(DATA_SERVICE_FACTORY_PID, dataServicePid,
                dataServiceProperties, false);

        this.configurationService.createFactoryConfiguration(DATA_TRANSPORT_SERVICE_FACTORY_PID,
                dataTransportServicePid, null, true);
    }

    @Override
    public void deleteConfiguration(String cloudConnectionServicePid) throws KuraException {
        final Iterator<String> stackComponentPids = getStackComponentsPids(cloudConnectionServicePid).iterator();

        this.configurationService.deleteFactoryConfiguration(stackComponentPids.next(), false);
        this.configurationService.deleteFactoryConfiguration(stackComponentPids.next(), false);
        this.configurationService.deleteFactoryConfiguration(stackComponentPids.next(), true);
    }

    @Override
    public String getFactoryPid() {
        return Constants.CLOUD_ENDPOINT_FACTORY_PID;
    }

    @Override
    public Set<String> getManagedCloudConnectionPids() throws KuraException {
        try {
            return this.context
                    .getServiceReferences(CloudEndpoint.class, Constants.CLOUD_ENDPOINT_SERVICE_FILTER_TEMPLATE)
                    .stream().map(ref -> (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID))
                    .filter(pid -> pid != null
                            && Constants.MANAGED_CLOUD_ENDPOINT_SERVICE_PID_PATTERN.matcher(pid).matches())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public List<String> getStackComponentsPids(final String pid) throws KuraException {
        final String suffix = getSuffix(pid);

        if (suffix == null) {
            return DEFAULT_STACK_COMPONENT_PIDS;
        }

        return DEFAULT_STACK_COMPONENT_PIDS.stream().map(comp -> comp + suffix).collect(Collectors.toList());
    }

    private String getSuffix(final String cloudConnectionServicePid) throws KuraException {

        final Matcher matcher = Constants.MANAGED_CLOUD_ENDPOINT_SERVICE_PID_PATTERN.matcher(cloudConnectionServicePid);

        if (!matcher.matches()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "Provided pid cannot be associated with a Watson CloudService");
        }

        return matcher.group(1);
    }
}
