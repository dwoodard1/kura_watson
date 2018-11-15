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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.core.util.MqttTopicUtil;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttCloudEndpoint implements CloudEndpoint, CloudConnectionManager, DataServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttCloudEndpoint.class);

    private final Set<CloudConnectionListener> registeredCloudConnectionListeners = new CopyOnWriteArraySet<>();
    private final Set<CloudDeliveryListener> registeredCloudDeliveryListeners = new CopyOnWriteArraySet<>();
    private final Map<String, List<CloudSubscriberListener>> registeredCloudSubscriberListeners = new ConcurrentHashMap<>();

    private DataService dataService;

    /*
     * Dependencies
     * 
     */
    public void bindDataServiceInternal(final DataService dataService) {
        this.dataService = dataService;
    }

    public void unbindDataServiceInternal() {
        this.dataService = null;
    }

    /*
     * OSGi Activation Methods
     * 
     */
    public void activateInternal() {
        this.dataService.addDataServiceListener(this);
    }

    public void deactivateInternal() {
        this.dataService.removeDataServiceListener(this);
    }

    /*
     * CloudEndpoint Methods
     * 
     */
    @Override
    public String publish(KuraMessage message) throws KuraException {
        final Map<String, Object> properties = message.getProperties();

        final String topic = extract(properties, MqttCloudEndpointConstants.TOPIC.name(), String.class);
        final int qos = extractOrDefault(properties, MqttCloudEndpointConstants.QOS.name(), 0);
        final boolean retain = extractOrDefault(properties, MqttCloudEndpointConstants.RETAIN.name(), false);
        final int priority = extractOrDefault(properties, MqttCloudEndpointConstants.PRIORITY.name(), 7);

        return String.valueOf(this.dataService.publish(topic, message.getPayload().getBody(), qos, retain, priority));
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.registeredCloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.registeredCloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    @Override
    public void registerSubscriber(Map<String, Object> subscriptionProperties,
            CloudSubscriberListener cloudSubscriberListener) {
        final String topic = extractTopic(subscriptionProperties);
        final int qos = extractOrDefault(subscriptionProperties, MqttCloudEndpointConstants.QOS.name(), 0);

        final List<CloudSubscriberListener> listeners = this.registeredCloudSubscriberListeners.computeIfAbsent(topic,
                t -> new CopyOnWriteArrayList<>());
        listeners.add(cloudSubscriberListener);

        if (listeners.size() == 1) {
            subscribe(topic, qos);
        }
    }

    @Override
    public void unregisterSubscriber(CloudSubscriberListener cloudSubscriberListener) {
        this.registeredCloudSubscriberListeners.entrySet().removeIf(e -> {

            final String topicFilter = e.getKey();
            final List<CloudSubscriberListener> listeners = e.getValue();

            listeners.removeIf(l -> l == cloudSubscriberListener);
            final boolean isEmpty = listeners.isEmpty();

            if (isEmpty) {
                unsubscribe(topicFilter);
                return true;
            } else {
                return false;
            }
        });
    }

    /*
     * CloudConnectionManager Methods
     * 
     */
    @Override
    public void connect() throws KuraConnectException {
        this.dataService.connect();
    }

    @Override
    public void disconnect() throws KuraDisconnectException {
        this.dataService.disconnect(10);
    }

    @Override
    public boolean isConnected() {
        return this.dataService.isConnected();
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.registeredCloudConnectionListeners.add(cloudConnectionListener);

    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.registeredCloudConnectionListeners.remove(cloudConnectionListener);
    }

    /*
     * DataServiceListener Methods
     * 
     */
    @Override
    public void onConnectionEstablished() {
        for (final CloudConnectionListener cloudConnectionListener : this.registeredCloudConnectionListeners) {
            cloudConnectionListener.onConnectionEstablished();
        }
    }

    @Override
    public void onConnectionLost(Throwable arg0) {
        for (final CloudConnectionListener cloudConnectionListener : this.registeredCloudConnectionListeners) {
            cloudConnectionListener.onConnectionLost();
        }
    }

    @Override
    public void onDisconnected() {
        for (final CloudConnectionListener cloudConnectionListener : this.registeredCloudConnectionListeners) {
            cloudConnectionListener.onDisconnected();
        }
    }

    @Override
    public void onDisconnecting() {
        // noop
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        logger.debug("message arrived, topic: {} qos: {}", topic, qos);

        final RawMessage rawMessage = new RawMessage(topic, payload, qos, retained);

        for (Entry<String, List<CloudSubscriberListener>> e : this.registeredCloudSubscriberListeners.entrySet()) {
            if (MqttTopicUtil.isMatched(e.getKey(), topic)) {
                e.getValue().forEach(listener -> listener.onMessageArrived(rawMessage.toKuraMessage()));
            }
        }
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        for (final CloudDeliveryListener listener : this.registeredCloudDeliveryListeners) {
            listener.onMessageConfirmed(String.valueOf(messageId));
        }
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        // noop
    }

    /*
     * Private Methods
     * 
     */
    @SuppressWarnings("unchecked")
    private static final <T> T extract(final Map<String, Object> properties, final String key, final Class<T> clazz)
            throws KuraException {
        final Object raw = properties.get(key);

        if (!clazz.isInstance(raw)) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Failed to extract " + key + " parameter");
        }

        return (T) raw;
    }

    @SuppressWarnings("unchecked")
    private static final <T> T extractOrDefault(final Map<String, Object> properties, final String key,
            final T defaultValue) {
        final Object raw = properties.get(key);

        if (defaultValue.getClass().isInstance(raw)) {
            return (T) raw;
        }

        return defaultValue;
    }

    private static final String extractTopic(final Map<String, Object> subscriptionProperties) {
        String topic;
        try {
            topic = extract(subscriptionProperties, MqttCloudEndpointConstants.TOPIC.name(), String.class);
            MqttTopicUtil.validate(topic, true);
            return topic;
        } catch (KuraException e) {
            throw new IllegalArgumentException("Failed to extract topic from subscription properties");
        }
    }

    private void subscribe(final String topicFilter, final int qos) {
        try {
            logger.info("subscribing to {} with qos {}", topicFilter, qos);
            this.dataService.subscribe(topicFilter, qos);
        } catch (final Exception e) {
            logger.warn("failed to subscribe", e);
        }
    }

    private void unsubscribe(final String topicFilter) {
        try {
            logger.info("unsubscribing from {}", topicFilter);
            this.dataService.unsubscribe(topicFilter);
        } catch (final Exception e) {
            logger.warn("failed to unsubscribe", e);
        }
    }

    private static class RawMessage {

        private final String topic;
        private final byte[] payload;
        private final int qos;
        private final boolean retained;

        private KuraMessage message;

        RawMessage(String topic, byte[] payload, int qos, boolean retained) {
            this.topic = topic;
            this.payload = payload;
            this.qos = qos;
            this.retained = retained;
        }

        public KuraMessage toKuraMessage() {
            if (this.message != null) {
                return this.message;
            }

            final Map<String, Object> messageProperties = new HashMap<>(3);

            messageProperties.put(MqttCloudEndpointConstants.TOPIC.name(), this.topic);
            messageProperties.put(MqttCloudEndpointConstants.QOS.name(), this.qos);
            messageProperties.put(MqttCloudEndpointConstants.RETAIN.name(), this.retained);

            final KuraPayload kuraPayload = new KuraPayload();
            kuraPayload.setBody(this.payload);

            this.message = new KuraMessage(kuraPayload, messageProperties);

            return this.message;
        }
    }
}
