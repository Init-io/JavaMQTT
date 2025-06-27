// Inside your Android Activity
mqtt = new JavaMQTT(
    "ssl://your-broker.com:8883",
            "android-client-id",
    getFilesDir().getAbsolutePath(),
command -> new Handler(Looper.getMainLooper()).post(command)
);

        mqtt.connect("username", "password", new JavaMQTT.ConnectionListener() {
    @Override
    public void onSuccess() {
        Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
        mqtt.subscribe("led/control", (topic, payload) -> {
            runOnUiThread(() -> toggleButton.setText(payload.equals("1") ? "Turn OFF" : "Turn ON"));
        });
    }

    @Override
    public void onFailure(Throwable exception) {
        Toast.makeText(MainActivity.this, "Failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
    }
});
