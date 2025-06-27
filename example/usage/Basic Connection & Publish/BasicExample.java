import io.github.initio.JavaMQTT;

public class BasicExample {
    public static void main(String[] args) throws Exception {
        JavaMQTT mqtt = new JavaMQTT(
                "tcp://broker.hivemq.com:1883",
                "basic-client-" + System.currentTimeMillis(),
                null,
                Runnable::run // Main thread callback executor
        );

        mqtt.connect(null, null, new JavaMQTT.ConnectionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Connected!");
                mqtt.put("test/topic", "Hello MQTT!");
            }

            @Override
            public void onFailure(Throwable exception) {
                System.err.println("Connection failed: " + exception.getMessage());
            }
        });
    }
}
