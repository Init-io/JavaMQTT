import io.github.initio.JavaMQTT;

public class SecureExample {
    public static void main(String[] args) throws Exception {
        JavaMQTT mqtt = new JavaMQTT(
                "ssl://your-broker.com:8883",
                "secure-client-" + System.currentTimeMillis(),
                "mqtt-store", // Your persistence directory
                Runnable::run
        );

        mqtt.connect("yourUser", "yourPassword", new JavaMQTT.ConnectionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Securely connected ✅");
                mqtt.putRetain("secure/topic", "🔐 Hello over SSL!");
            }

            @Override
            public void onFailure(Throwable exception) {
                System.err.println("Secure connect failed: " + exception.getMessage());
            }
        });
    }
}
