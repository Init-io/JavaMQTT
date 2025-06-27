import io.github.initio.JavaMQTT;

public class SubscribeExample {
    public static void main(String[] args) throws Exception {
        JavaMQTT mqtt = new JavaMQTT(
                "tcp://broker.hivemq.com:1883",
                "sub-client-" + System.currentTimeMillis(),
                null,
                Runnable::run
        );

        mqtt.connect(null, null, new JavaMQTT.ConnectionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Connected!");
                mqtt.subscribe("my/topic", (topic, payload) -> {
                    System.out.println("Got message: " + payload + " from topic: " + topic);
                });
            }

            @Override
            public void onFailure(Throwable exception) {
                System.err.println("Connection failed: " + exception.getMessage());
            }
        });
    }
}
