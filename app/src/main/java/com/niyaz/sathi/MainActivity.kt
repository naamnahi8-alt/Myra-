 package com.niyaz.sathi
import android.Manifest.permission.*
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import org.json.*
import java.net.*
import java.util.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    lateinit var out: TextView; lateinit var key: EditText; lateinit var tts: TextToSpeech
    var sr: SpeechRecognizer? = null
    val h = Handler(Looper.getMainLooper())
    val apps = mapOf("youtube" to "com.google.android.youtube", "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android", "facebook" to "com.facebook.katana",
        "gmail" to "com.google.android.gm", "maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome", "camera" to "com.android.camera")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(arrayOf(RECORD_AUDIO, READ_CONTACTS), 1)
        tts = TextToSpeech(this, this)
        val p = getSharedPreferences("s", 0)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 100, 40, 40); setBackgroundColor(0xFF0B0B0C.toInt()) }
        root.addView(TextView(this).apply { text = "🎙️ Voice Sathi"; textSize = 24f; setTextColor(0xFFE0B040.toInt()) })
        key = EditText(this).apply { hint = "Gemini API key yahan daalo"; setText(p.getString("k", "")); setTextColor(-1); setHintTextColor(0xFF888888.toInt()); setSingleLine() }
        root.addView(key)
        root.addView(Button(this).apply { text = "🎤"; textSize = 40f; setOnClickListener { p.edit().putString("k", key.text.toString().trim()).apply(); listen() } },
            LinearLayout.LayoutParams(300, 300).apply { gravity = Gravity.CENTER; topMargin = 40; bottomMargin = 40 })
        out = TextView(this).apply { textSize = 18f; setTextColor(-1); text = "Mic dabao aur bolo: 'Rahul ko call karo', 'YouTube kholo', '5 minute timer', ya koi bhi sawal." }
        root.addView(ScrollView(this).apply { addView(out) })
        setContentView(root)
        onNewIntent(intent)
    }
    override fun onNewIntent(i: Intent?) { super.onNewIntent(i); if (i?.getBooleanExtra("listen", false) == true) h.postDelayed({ listen() }, 600) }
    override fun onInit(s: Int) { tts.language = Locale("hi", "IN") }
    fun say(t: String) { out.text = t; tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "x") }

    fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { say("Is phone mein voice recognition nahi hai"); return }
        sr?.destroy(); sr = SpeechRecognizer.createSpeechRecognizer(this)
        sr!!.setRecognitionListener(object : RecognitionListener {
            override fun onResults(r: Bundle) { r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { handle(it) } }
            override fun onError(e: Int) { out.text = "Dobara bolo (error $e)" }
            override fun onReadyForSpeech(p: Bundle?) { out.text = "Sun raha hoon…" }
            override fun onBeginningOfSpeech() {}; override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}; override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}; override fun onEvent(t: Int, b: Bundle?) {}
        })
        sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN"))
    }

    fun phoneOf(q: String): String? {
        Regex("\\d{8,13}").find(q.replace(" ", ""))?.let { return it.value }
        contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use {
            while (it.moveToNext()) { val n = it.getString(0).lowercase().split(" ")[0]; if (n.isNotEmpty() && q.contains(n)) return it.getString(1) }
        }
        return null
    }

    fun handle(t: String) {
        val q = t.lowercase(); out.text = "🗣 $t"
        try {
            if (Regex("call|कॉल|फोन|phone").containsMatchIn(q)) {
                val n = phoneOf(q); if (n == null) say("Contact nahi mila") else { say("Call laga raha hoon"); startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) }; return }
            if (q.contains("whatsapp") && phoneOf(q) != null) {
                val msg = q.replace(Regex("whatsapp|message|bhejo|ko|\\d+"), "").trim()
                say("WhatsApp khol raha hoon"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91${phoneOf(q)!!.takeLast(10)}?text=" + Uri.encode(msg)))); return }
            Regex("(\\d+)\\s*(minute|min|मिनट|second|सेकंड)").find(q)?.let {
                val n = it.groupValues[1].toInt() * (if (it.groupValues[2].startsWith("s") || it.groupValues[2] == "सेकंड") 1 else 60)
                say("Timer shuru"); startActivity(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, n).putExtra(AlarmClock.EXTRA_SKIP_UI, true)); return }
            if (Regex("open|kholo|खोलो|chalu").containsMatchIn(q)) for ((k, pkg) in apps) if (q.contains(k)) {
                packageManager.getLaunchIntentForPackage(pkg)?.let { say("$k khol raha hoon"); startActivity(it); return } }
            Regex("youtube (?:par|pe|पर)? ?(.+)").find(q)?.let { say("YouTube par dhundh raha hoon"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com/results?search_query=" + Uri.encode(it.groupValues[1])))); return }
            if (q.contains("time") || q.contains("samay") || q.contains("समय")) { say("Abhi " + java.text.SimpleDateFormat("h:mm a", Locale("hi")).format(Date()) + " baje hain"); return }
        } catch (e: Exception) { say("Ye nahi ho paya: ${e.message}"); return }
        ask(t)
    }

    fun ask(q: String) {
        out.text = "…"
        Thread {
            try {
                val c = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("x-goog-api-key", key.text.toString().trim())
                fun parts(s: String) = JSONArray().put(JSONObject().put("text", s))
                val body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts", parts("Tum Voice Sathi ho, ek dost jaisa Hindi voice assistant. User ki bhasha mein 3 vaakya se chhota jawab do, bina markdown ke.")))
                    .put("contents", JSONArray().put(JSONObject().put("parts", parts(q))))
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                val s = (if (c.responseCode < 400) c.inputStream else c.errorStream).bufferedReader().readText()
                val t = JSONObject(s).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                h.post { say(t) }
            } catch (e: Exception) { h.post { say("AI se jawab nahi mila. API key check karo.") } }
        }.start()
    }
}
