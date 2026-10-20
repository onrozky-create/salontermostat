package tr.ev.akilliev;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "salon_app";
    private SharedPreferences prefs;
    private LinearLayout root, bar;
    private WebView web;
    private TextView statusLine;
    private Button voiceButton;
    private TextToSpeech tts;
    private boolean ttsReady = false, voiceEnabled = false;
    private String baseUrl = "http://192.168.4.1";
    private String lastState = "";
    private boolean wasOnline = false;
    private int failedPolls = 0;
    private final Handler handler = new Handler();
    private boolean activityVisible = true;

    private final Runnable monitor = new Runnable() {
        @Override public void run() {
            if (!activityVisible) return;
            new StatusTask().execute(baseUrl + "/status?_=" + System.currentTimeMillis());
            handler.postDelayed(this, 5000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.rgb(10,18,32));
            getWindow().setNavigationBarColor(Color.rgb(10,18,32));
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        baseUrl = prefs.getString("url", "http://192.168.4.1");
        voiceEnabled = prefs.getBoolean("voice", false);
        buildUi();
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override public void onInit(int result) {
                if (result == TextToSpeech.SUCCESS) {
                    int lang = tts.setLanguage(new Locale("tr", "TR"));
                    ttsReady = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED;
                    updateVoiceButton();
                    if (voiceEnabled && ttsReady) speak("Salon termostatı hazır.");
                }
            }
        });
        web.loadUrl(baseUrl + "/");
    }

    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(10,18,32));
        bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(6,6,6,6); bar.setBackgroundColor(Color.rgb(17,29,47));
        voiceButton = makeButton("🔇 Ses kapalı"); voiceButton.setOnClickListener(v -> toggleVoice());
        Button weather = makeButton("☀ Hava"); weather.setOnClickListener(v -> showWeather());
        Button address = makeButton("⚙ Bağlantı"); address.setOnClickListener(v -> askAddress());
        bar.addView(voiceButton, new LinearLayout.LayoutParams(0, dp(44), 1.2f));
        bar.addView(weather, new LinearLayout.LayoutParams(0, dp(44), .8f));
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(44), .9f));
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        statusLine = new TextView(this); statusLine.setText("Salon modülüne bağlanılıyor…"); statusLine.setTextColor(Color.rgb(184,204,229)); statusLine.setTextSize(12); statusLine.setPadding(12,5,12,5); statusLine.setBackgroundColor(Color.rgb(13,23,38));
        root.addView(statusLine, new LinearLayout.LayoutParams(-1, dp(28)));
        web = new WebView(this); web.setBackgroundColor(Color.rgb(10,18,32));
        WebSettings s = web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setLoadsImagesAutomatically(true); if (android.os.Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient());
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
    }

    private Button makeButton(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(12); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.rgb(45,78,119)); return b;
    }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }

    private void toggleVoice() {
        voiceEnabled = !voiceEnabled; prefs.edit().putBoolean("voice", voiceEnabled).apply(); updateVoiceButton();
        if (voiceEnabled) {
            if (!ttsReady) { Toast.makeText(this, "Telefonda Türkçe ses motoru bulunamadı. Android ayarlarından Türkçe ses yükleyin.", Toast.LENGTH_LONG).show(); return; }
            speak("Türkçe sesli bildirimler açıldı. Salon termostatı hazır.");
            new StatusTask().execute(baseUrl + "/status?_=" + System.currentTimeMillis());
        } else if (tts != null) tts.stop();
    }
    private void updateVoiceButton() {
        if (voiceButton == null) return;
        voiceButton.setText(voiceEnabled ? (ttsReady ? "🔊 Türkçe ses açık" : "🔊 Ses açık · Türkçe ses yok") : "🔇 Ses kapalı");
    }
    private void speak(String text) {
        if (voiceEnabled && ttsReady && tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private String detectState(String html) {
        String h = html.toLowerCase(new Locale("tr", "TR"));
        boolean gas = h.contains("gaz alarmı") || h.contains("doğal gaz alarmı");
        boolean smoke = h.contains("duman alarmı");
        if (gas && smoke) return "Dikkat. Doğal gaz ve duman alarmı aktif. Güvenli bir yere geçin.";
        if (gas) return "Dikkat. Doğal gaz alarmı aktif. Güvenli bir yere geçin.";
        if (smoke) return "Dikkat. Duman alarmı aktif.";
        if (h.contains("açık pencere olasılığı") || h.contains("açık pencere uyarısı")) return "Açık pencere uyarısı. Oda sıcaklığı hızlı düşüyor.";
        if (h.contains("güncel veri alınamıyor") || h.contains("sensör bulunamadı") || h.contains("sensör arızası")) return "Uyarı. Sensör bağlantısında sorun var.";
        return "";
    }

    private class StatusTask extends AsyncTask<String, Void, String[]> {
        @Override protected String[] doInBackground(String... u) {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection)new URL(u[0]).openConnection(); c.setConnectTimeout(3500); c.setReadTimeout(3500); c.setRequestMethod("GET");
                InputStream in = c.getInputStream(); BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8")); StringBuilder out = new StringBuilder(); String line;
                while ((line = br.readLine()) != null) out.append(line).append('\n'); br.close(); return new String[]{"OK", out.toString()};
            } catch (Exception e) { return new String[]{"ERR", e.toString()}; }
            finally { if (c != null) c.disconnect(); }
        }
        @Override protected void onPostExecute(String[] r) {
            if (isFinishing()) return;
            if ("OK".equals(r[0])) {
                failedPolls = 0; statusLine.setText("● Salon modülüne bağlı · Sesli bildirim izleniyor");
                if (!wasOnline && voiceEnabled) speak("Salon termostatı bağlantısı kuruldu."); wasOnline = true;
                String state = detectState(r[1]);
                if (!state.equals(lastState)) { if (!state.isEmpty()) speak(state); else if (!lastState.isEmpty()) speak("Önceki alarm sona erdi."); lastState = state; }
            } else {
                failedPolls++;
                if (failedPolls >= 2) { statusLine.setText("● Salon modülüne ulaşılamıyor · Wi-Fi ve IP adresini kontrol edin"); if (wasOnline && voiceEnabled) speak("Salon termostatı bağlantısı kesildi. Wi-Fi veya elektrik kesintisi olabilir."); wasOnline = false; }
            }
        }
    }

    private void askAddress() {
        final EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Örnek: http://192.168.1.50"); input.setText(baseUrl); input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Salon ESP32 bağlantısı").setMessage("ESP32'nin ekranda veya seri monitörde görünen IP adresini yaz. http:// ile başlat.").setView(input)
            .setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet ve bağlan", (d,w) -> {
                String x = input.getText().toString().trim(); if (!x.startsWith("http://") && !x.startsWith("https://")) x = "http://" + x; while (x.endsWith("/")) x = x.substring(0,x.length()-1);
                baseUrl = x; prefs.edit().putString("url", baseUrl).apply(); web.loadUrl(baseUrl + "/"); statusLine.setText("Yeni adrese bağlanılıyor…");
            }).show();
    }

    private void showWeather() {
        final TextView result = new TextView(this); result.setText("Hava durumu alınıyor…"); result.setTextColor(Color.WHITE); result.setTextSize(16); result.setPadding(dp(20),dp(20),dp(20),dp(20));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Denizli hava durumu").setView(result).setPositiveButton("Kapat", null).create(); dialog.show();
        new AsyncTask<Void,Void,String>() {
            @Override protected String doInBackground(Void... v) {
                HttpURLConnection c = null;
                try {
                    URL u = new URL("https://api.open-meteo.com/v1/forecast?latitude=37.7765&longitude=29.0864&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=5&timezone=auto");
                    c = (HttpURLConnection)u.openConnection(); c.setConnectTimeout(7000); c.setReadTimeout(7000); BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8")); StringBuilder sb = new StringBuilder(); String line; while((line=br.readLine())!=null) sb.append(line); br.close();
                    JSONObject j = new JSONObject(sb.toString()), cur=j.getJSONObject("current"), day=j.getJSONObject("daily");
                    StringBuilder out = new StringBuilder(); out.append("Denizli · Şu an\n").append(Math.round(cur.getDouble("temperature_2m"))).append(" °C · Hissedilen ").append(Math.round(cur.getDouble("apparent_temperature"))).append(" °C\nNem: ").append(cur.getInt("relative_humidity_2m")).append("% · Rüzgâr: ").append(Math.round(cur.getDouble("wind_speed_10m"))).append(" km/sa\nYağış: ").append(day.getJSONArray("precipitation_probability_max").optInt(0,0)).append("%\n\n5 günlük tahmin:\n");
                    JSONArray dates=day.getJSONArray("time"), max=day.getJSONArray("temperature_2m_max"), min=day.getJSONArray("temperature_2m_min"), rain=day.getJSONArray("precipitation_probability_max");
                    for(int i=0;i<dates.length();i++) out.append(dates.getString(i)).append("  ").append(Math.round(max.getDouble(i))).append("° / ").append(Math.round(min.getDouble(i))).append("° · Yağış ").append(rain.optInt(i,0)).append("%\n");
                    out.append("\nKıyafet önerisi: "); double feels=cur.getDouble("apparent_temperature"); if(feels<=9) out.append("Mont ve sıcak tutan katman önerilir."); else if(feels<=16) out.append("İnce mont veya hırka al."); else if(feels<=23) out.append("Katmanlı, rahat kıyafet uygun."); else out.append("Hafif, nefes alan kıyafet seç."); if(rain.optInt(0,0)>=40) out.append(" Şemsiye al."); return out.toString();
                } catch(Exception e) { return "Hava durumu alınamadı. İnternet bağlantısını kontrol et.\n" + e.getMessage(); }
                finally { if(c!=null)c.disconnect(); }
            }
            @Override protected void onPostExecute(String s) { result.setText(s); }
        }.execute();
    }

    @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onResume() { super.onResume(); activityVisible = true; handler.removeCallbacks(monitor); handler.post(monitor); }
    @Override protected void onPause() { activityVisible = false; handler.removeCallbacks(monitor); super.onPause(); }
    @Override protected void onDestroy() { activityVisible = false; handler.removeCallbacks(monitor); if (web != null) web.destroy(); if (tts != null) { tts.stop(); tts.shutdown(); } super.onDestroy(); }
}
