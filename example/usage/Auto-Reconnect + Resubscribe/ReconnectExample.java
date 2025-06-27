import io.github.initio.JavaMQTT;

public class ReconnectExample {
    public static void main(String[] args) throws Exception {
        JavaMQTT mqtt = new JavaMQTT(
                "tcp://broker.hivemq.com:1883",
                "reconnect-client-" + System.currentTimeMillis(),
                null,
                Runnable::run
        );

        mqtt.setOnReconnectListener(() -> {
            System.out.println("Reconnected! 🎉 Resubscribing...");
        });

        mqtt.connect(null, null, new JavaMQTT.ConnectionListener() {
            @Override
            public void onSuccess() {
                mqtt.subscribe("topic/reconnect", (topic, payload) -> {
                    System.out.println("Received after reconnect: " + payload);
                });
            }

            @Override
            public void onFailure(Throwable exception) {
                System.err.println("Initial connect failed");
            }
        });
    }
}
