# Beta testing guide

For the maintainer: how to run a small beta. Testers get the Indonesian section below.

## Running it

- **Who.** 5–8 testers across: one older user at a large font size, one budget Xiaomi/Oppo/Vivo
  phone, one regular masjid-goer, one English user if possible.
- **Install.** Send them the latest `muslimdaily-<version>.apk` from GitHub Releases. Updates then
  arrive through Settings → Pembaruan.
- **Watch, don't ask.** In a short call or in person, ask them to do the three tasks below while
  thinking aloud. Note where they hesitate or tap the wrong thing; don't help unless they're stuck
  for a minute.
- **Collect.** Reports arrive at the address in `muslimdaily.reportEmail` (gradle.properties) with
  the diagnostics and any crash reports attached.
- **Triage.** Fix only what blocks a task or makes a reminder unreliable; log the rest as issues.
  Ship a 0.5.x, then repeat with the same testers after a week.

Reading a report's diagnostics:

| Symptom in "Recently fired" | Likely cause | What to tell the tester |
|---|---|---|
| The reminder is missing entirely | The OEM killed the app, or the phone was off | Follow the dontkillmyapp.com link for their brand; allow autostart |
| Present but `+300s` or more, `exact=false` | Exact alarms not allowed, Doze | Allow exact alarms; turn off battery optimisation for the app |
| Present and on time, but they saw nothing | Notifications off or the channel muted | Check "Muted channels" in the report |

## Untuk penguji (kirimkan bagian ini)

Assalamu'alaikum, terima kasih sudah mau mencoba Muslim Daily.

**Pasang.** Unduh berkas APK yang dikirim, buka, dan izinkan pemasangan dari sumber ini bila
diminta.

**Coba tiga hal ini:**
1. Buka aplikasi pertama kali dan siapkan lokasi serta pengingat.
2. Ikuti satu hari: centang shalat yang Anda kerjakan dan baca dzikir pagi atau petang lewat
   tombol Mulai.
3. Cari doa masuk rumah, lalu dengarkan audionya.

**Biarkan pengingat menyala** setidaknya seminggu, dan perhatikan apakah adzan berbunyi tepat
waktu.

**Kirim masukan** kapan saja lewat Pengaturan → Masukan dan diagnosa → Kirim masukan. Email akan
terbuka dengan data teknis di bawahnya; Anda bisa membaca dan menghapus bagian mana pun sebelum
mengirim. Bila aplikasi pernah berhenti mendadak, aplikasi akan menawarkan untuk mengirim laporan
saat dibuka lagi.

Ceritakan juga hal kecil: kata yang membingungkan, tombol yang sulit ditemukan, atau doa yang
menurut Anda keliru.
