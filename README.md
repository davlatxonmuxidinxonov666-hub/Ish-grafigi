# Ish grafigi — Android ilova

Smena grafigi, oylik/yillik ish kunlari va soatlari hisobi, ish kunlari uchun haqiqiy budilnik
(ilova yopiq yoki telefon qulflangan bo'lsa ham chaladi).

## APK olish
Har safar `main` shoxchasiga o'zgarish yuklanganda GitHub Actions APK yig'adi.
Tayyor fayl: **Releases** bo'limida `IshGrafigi.apk`.

## Tuzilishi
- `app/src/main/assets/index.html` — interfeys (grafik, hisob, budilnik, sozlamalar)
- `AlarmStore`, `AlarmReceiver`, `AlarmActivity` — telefon budilnigi (AlarmManager.setAlarmClock)
- `BootReceiver` — telefon qayta yoqilganda budilnikni tiklaydi
- `res/raw/melody.wav` — marimba kuyi
