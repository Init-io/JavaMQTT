# 🐝 JavaMQTT – MQTT so simple, even your toaster could use it.

Yo, welcome to **JavaMQTT** – a minimalist MQTT wrapper for Java and Android devs who just want to send data, not write dissertations on `IMqttToken`. No AndroidX, no `MqttAndroidClient` drama. Just vibes and messages flying over TCP/SSL.

![banner]([https://media.giphy.com/media/3o7aD4hTgFLzEbVvDi/giphy.gif](https://templates.peakboard.com/Intralogistics-Order-Parts-with-MQTT/img/peakboard-mqtt-dashboards.gif))

---

## ✨ Why JavaMQTT?

Because Paho is powerful but looks like it was designed to make you cry at 3 AM.

So we:
- Wrapped it ✨ cleanly
- Hid the scary stuff (like `SSLSocketFactory`)
- Made the API cute, concise, and production-ready
- And yeah... it *just works* 🚀

---

## 📦 Features

- ✅ Pure Java (works on Android and Java SE)
- ✅ Uses `MqttAsyncClient` for non-blocking swag
- ✅ Auto reconnect & auto resubscribe
- ✅ QoS configurable (0/1/2 like a boss)
- ✅ Retained message support
- ✅ Global & per-topic message listeners
- ✅ Callback on reconnect
- ✅ Safe threading with executor pools
- ✅ SSL support with optional "trust all" (for testing... pls don’t ship to production with that 😭)

---

## 🧪 Installation

Use [JitPack](https://jitpack.io) or Maven Central (your call, king 👑).

```gradle
implementation 'io.github.initio:JavaMQTT:1.0.0'
````

---

## ⚡ Usage

### 🔌 Connecting

```java
JavaMQTT mqtt = new JavaMQTT(
    "ssl://broker.hivemq.com:8883",
    "my-client-id",
    getFilesDir().getAbsolutePath(),  // For Android
    command -> new Handler(Looper.getMainLooper()).post(command)
);

mqtt.connect("username", "password", new JavaMQTT.ConnectionListener() {
    public void onSuccess() {
        Log.d("MQTT", "Connected 🎉");
    }

    public void onFailure(Throwable e) {
        Log.e("MQTT", "Bruh, connect failed 💀: " + e.getMessage());
    }
});
```

---

### 📬 Subscribe

```java
mqtt.subscribe("device/led1", (topic, payload) -> {
    Log.d("MQTT", "Got message on " + topic + ": " + payload);
});
```

---

### 📤 Publish

```java
mqtt.put("device/led1", "1");           // Send without retain
mqtt.putRetain("device/led1", "0");     // Send with retain
```

---

### ♻️ Auto Resubscribe

Set a listener so whenever reconnect happens, you resub everything:

```java
mqtt.setOnReconnectListener(() -> {
    mqtt.subscribe("device/led1", listener);
    mqtt.subscribe("device/fan", anotherListener);
});
```

---

### 🌍 Global Listener

Catch all messages in one place (if you're lazy or building a message router):

```java
mqtt.setGlobalListener((topic, payload) -> {
    Log.d("MQTT", "Yo we got " + payload + " from " + topic);
});
```

---

### 😴 Disconnect

```java
mqtt.disconnect();  // Peace out 🕊️
mqtt.close();       // Clean up memory like a responsible adult
```

---

## 🔐 SSL Warning

We’re using a "trust-all" SSL context for testing/dev only. For real-world apps, plug in a proper cert-based factory. Your data deserves respect. 😤

---

## 🧠 Pro Tip

> “MqttAsyncClient is life. Blocking is cringe.”
> — Sun Tzu, *The Art of Threading*

---

## 👀 Example App?

Check [`/examples/`](./examples/) if you’re still confused. Or just DM the lib author on GitHub like it’s 2010 StackOverflow.

---

## 🧼 License

MIT. Do whatever. Just don’t use this to control actual nukes or your grandma’s pacemaker.

---

## 🦄 Author

Made by [@SiamRayhan](https://instagram.com/thesiamrayhan) – lover of fast libs, cleaner APIs, and unnecessarily long README files.

---

 If you like this lib, ⭐ it. If not, maybe meditate or touch grass.
