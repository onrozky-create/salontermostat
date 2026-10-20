SALON TERMOSTATI - ANDROID UYGULAMA KAYNAK PROJESİ
===================================================

Bu bir Android Studio kaynak projesidir; derlenmiş APK değildir. Bu çalışma ortamında Android SDK/Gradle olmadığı için APK üretimi burada yapılamadı.

ÖZELLİKLER
- Minimum Android API 19 (Android 4.4 / Galaxy Note II hedeflenerek)
- ESP32 web panelini uygulama içinde açar.
- Uygulama içinden ESP32 IP adresi/adresi değiştirilebilir.
- Android TextToSpeech ile yerel Türkçe sesli alarm izleme.
- Doğal gaz, duman, açık pencere, sensör sorunu ve bağlantı kesilmesi/gelmesi durumlarını /status metninden izler.
- Uygulama içi hava durumu ekranı Open-Meteo üzerinden güncel veri ve 5 günlük tahmin gösterir; ilk sürümde Denizli sabittir.
- Ses tercihi ve ESP32 adresi telefonda saklanır.
- Mevcut salon ESP32'siyle HTTP üzerinden konuşur. ESP32 yazılımı değiştirilmez.

KURULUM / DERLEME
1. Android Studio kurulu bir bilgisayarda bu klasörü aç.
2. Android SDK 34 ve JDK 17 kurulu olmalı.
3. Gradle eşitlemesi tamamlanınca Build > Build APK(s) seç.
4. APK genellikle app/build/outputs/apk/debug/app-debug.apk yolunda oluşur.
5. Telefonda bilinmeyen uygulama yükleme iznini verip APK'yı kur.

İLK AÇILIŞ
- Üstteki “Bağlantı” düğmesine bas.
- ESP32 panelinin IP adresini yaz (ör. http://192.168.1.50).
- ESP32'nin yerel Wi-Fi ağına bağlan.
- “Türkçe ses açık” düğmesine dokun. Telefonun metinden konuşmaya ayarlarında Türkçe ses verisi bulunmalıdır.

ÖNEMLİ SINIRLAR
- Bu kaynak proje burada Android SDK/Gradle bulunmadığı için derlenemedi ve fiziksel telefonda test edilmedi.
- Android 4.4 WebView modern JavaScript özelliklerinin bazılarını desteklemez. APK içindeki yerel hava durumu ve yerel TTS butonları bunun için ayrı yapılmıştır; eski WebView üzerinde web panelindeki bazı yeni görsel/JavaScript özellikleri yine kısıtlı kalabilir.
- Türkçe TTS cihazda Türkçe ses verisi yoksa konuşmayabilir; Android TTS ayarlarından Türkçe ses yüklenmelidir.
- Sesli izleme uygulama açık/ön plandayken çalışır. Bu ilk sürüm arka plan servisi değildir; uygulama kapatıldığında kesinti bildirimini garanti etmez.
- Hava durumu için internet gerekir. Bu ilk sürümde şehir Denizli'dir.
- Bu uygulama güvenlik açısından gaz alarmının tek uyarı kanalı olarak kullanılmamalıdır. Donanımın yerel alarm davranışını değiştirmez.


UZAKTAN ERİŞİM - ÖNEMLİ
----------------------
Modem WAN adresi 100.92.79.251, CGNAT aralığındadır. Bu nedenle modemde port yönlendirme açmak tek başına uzaktan erişim sağlamaz. Evde sürekli açık ikinci cihaz olmadığından, bu proje tek başına CGNAT'ı aşamaz. En kolay yol internet sağlayıcısından CGNAT dışı gerçek/public IPv4 istemektir (statik olması şart olmayabilir; sağlayıcıya sorun). Sağlayıcı bunu verirse modem üzerinde güvenli erişim/port yönlendirme ayrıca planlanmalıdır; ESP32 web panelini kimlik doğrulamasız şekilde doğrudan internete açmayın. Alternatif olarak dışarıya bağlantı kuran bir ara sunucu gerekir ve bu ayrı sunucu hizmeti/kurulumu demektir.

NOTE 2 UYUMLULUK
----------------
Uygulama minSdk 19 olarak ayarlanmıştır ve paneli Android WebView içinde açar. Ancak ESP32 web panelinin kendi JavaScript/CSS kodunda Note 2'nin eski WebView'inin desteklemediği özellikler varsa, bunlar yalnızca APK'yı sarmalayarak tamamen düzeltilemez; panel kodunda ayrıca eski WebView uyumluluk düzeltmesi gerekir. Bu kaynak proje fiziksel Galaxy Note II üzerinde test edilmemiştir.
