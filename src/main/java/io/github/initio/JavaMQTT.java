package io.github.initio;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.*;

public class JavaMQTT {

    private static final Logger logger = LoggerFactory.getLogger(JavaMQTT.class);

    private final MqttAsyncClient client;
    private final MqttConnectOptions options;
    private final ExecutorService executor;
    private final Executor callbackExecutor;

    private int qos = 1;
    private final ConcurrentHashMap<String, MessageListener> topicListeners = new ConcurrentHashMap<>();
    private volatile MessageListener globalListener;
    private volatile Runnable onReconnectListener;
    private final Object connectLock = new Object();
    private volatile boolean isConnecting = false;

    public interface ConnectionListener {
        void onSuccess();
        void onFailure(Throwable exception);
    }

    public interface MessageListener {
        void onMessage(String topic, String payload);
    }

    public JavaMQTT(String serverUri, String clientId, String persistenceDir, Executor callbackExecutor) throws MqttException {
        this.client = new MqttAsyncClient(serverUri, clientId,
                persistenceDir != null ? new MqttDefaultFilePersistence(persistenceDir) : new MemoryPersistence());
        this.options = new MqttConnectOptions();
        this.executor = Executors.newFixedThreadPool(4);
        this.callbackExecutor = callbackExecutor != null ? callbackExecutor : Executors.newCachedThreadPool();

        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);
        options.setMaxInflight(100);

        if (serverUri.startsWith("ssl://")) {
            options.setSocketFactory(getTrustAllSSLSocketFactory());
        }

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                logger.info("Connected to MQTT broker: {}, reconnect: {}", serverURI, reconnect);
                if (onReconnectListener != null) callbackExecutor.execute(onReconnectListener);
                if (reconnect) resubscribeAll();
            }

            @Override
            public void connectionLost(Throwable cause) {
                logger.warn("Connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                final String payload = new String(message.getPayload());
                logger.debug("Message arrived on topic {}: {}", topic, payload);
                callbackExecutor.execute(() -> {
                    try {
                        MessageListener listener = topicListeners.get(topic);
                        if (listener != null) listener.onMessage(topic, payload);
                        if (globalListener != null) globalListener.onMessage(topic, payload);
                    } catch (Exception e) {
                        logger.error("Error in message callback", e);
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.debug("Delivery complete for message");
            }
        });
    }

    public void connect(String username, String password, ConnectionListener listener) {
        synchronized (connectLock) {
            if (isConnecting || client.isConnected()) return;
            isConnecting = true;
        }

        executor.execute(() -> {
            try {
                if (username != null) options.setUserName(username);
                if (password != null) options.setPassword(password.toCharArray());
                client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        logger.info("MQTT connect success");
                        isConnecting = false;
                        if (listener != null) callbackExecutor.execute(listener::onSuccess);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        logger.error("Connect exception", exception);
                        isConnecting = false;
                        if (listener != null) callbackExecutor.execute(() -> listener.onFailure(exception));
                    }
                });
            } catch (MqttException e) {
                logger.error("Immediate connect failure", e);
                isConnecting = false;
                if (listener != null) callbackExecutor.execute(() -> listener.onFailure(e));
            }
        });
    }

    public void put(String topic, String value, boolean retain) {
        executor.execute(() -> {
            try {
                if (client.isConnected()) {
                    MqttMessage message = new MqttMessage(value.getBytes());
                    message.setQos(qos);
                    message.setRetained(retain);
                    client.publish(topic, message);
                    logger.debug("Published to {}: {}", topic, value);
                } else {
                    logger.warn("Publish failed, client not connected");
                }
            } catch (MqttException e) {
                logger.error("Publish exception", e);
            }
        });
    }

    public void put(String topic, String value) {
        put(topic, value, false);
    }

    public void putRetain(String topic, String value) {
        put(topic, value, true);
    }

    public void subscribe(String topic, MessageListener listener, int qos) {
        executor.execute(() -> {
            try {
                if (client.isConnected()) {
                    client.subscribe(topic, qos);
                    topicListeners.put(topic, listener);
                    logger.info("Subscribed to topic: {} with QoS {}", topic, qos);
                } else {
                    logger.warn("Subscribe failed, client not connected");
                }
            } catch (MqttException e) {
                logger.error("Subscribe exception", e);
            }
        });
    }

    public void subscribe(String topic, MessageListener listener) {
        subscribe(topic, listener, qos);
    }

    public void unsubscribe(String topic) {
        executor.execute(() -> {
            try {
                if (client.isConnected()) {
                    client.unsubscribe(topic);
                    topicListeners.remove(topic);
                    logger.info("Unsubscribed from topic: {}", topic);
                }
            } catch (MqttException e) {
                logger.error("Unsubscribe exception", e);
            }
        });
    }

    public void setGlobalListener(MessageListener listener) {
        globalListener = listener;
    }

    public void setOnReconnectListener(Runnable listener) {
        onReconnectListener = listener;
    }

    private void resubscribeAll() {
        for (Map.Entry<String, MessageListener> entry : topicListeners.entrySet()) {
            try {
                client.subscribe(entry.getKey(), qos);
                logger.info("Resubscribed to topic: {}", entry.getKey());
            } catch (MqttException e) {
                logger.error("Failed to resubscribe to topic: {}", entry.getKey(), e);
            }
        }
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                    logger.info("Disconnected from MQTT broker");
                }
            } catch (MqttException e) {
                logger.error("Disconnect exception", e);
            }
        });
    }

    public void setQos(int qos) {
        if (qos >= 0 && qos <= 2) {
            this.qos = qos;
        } else {
            logger.warn("Invalid QoS level: {}", qos);
        }
    }

    public void close() {
        try {
            disconnect();
            client.close();
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            logger.info("JavaMQTT resources cleaned up");
        } catch (MqttException | InterruptedException e) {
            logger.error("Error during cleanup", e);
        }
    }

    private SSLSocketFactory getTrustAllSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSLSocketFactory", e);
        }
    }
}
