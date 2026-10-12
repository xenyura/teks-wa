package com.bangis.tekswa

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

const val SALURAN = "alarm_catat_v1"
const val AKSI_MATIKAN = "com.bangis.tekswa.MATIKAN"

// Satu alarm: berulang tiap hari pada jam:menit, dengan nama
data class Alarm(val id: Int, val jam: Int, val menit: Int, val nama: String, val aktif: Boolean)

fun jamTeks(a: Alarm): String = String.format(Locale.US, "%02d:%02d", a.jam, a.menit)

fun muatAlarm(c: Context): List<Alarm> {
    val s = c.getSharedPreferences("tekswa", Context.MODE_PRIVATE).getString("alarm", null)
        ?: return emptyList()
    return try {
        val a = JSONArray(s)
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Alarm(o.getInt("id"), o.getInt("jam"), o.getInt("menit"), o.getString("nama"), o.getBoolean("aktif"))
        }
    } catch (e: Exception) {
        emptyList()
    }
}

fun simpanAlarm(c: Context, daftar: List<Alarm>) {
    val a = JSONArray()
    for (x in daftar) {
        a.put(
            JSONObject().put("id", x.id).put("jam", x.jam).put("menit", x.menit)
                .put("nama", x.nama).put("aktif", x.aktif)
        )
    }
    c.getSharedPreferences("tekswa", Context.MODE_PRIVATE).edit().putString("alarm", a.toString()).apply()
}

object Penjadwal {
    private fun waktuBerikut(a: Alarm): Long {
        val sekarang = LocalDateTime.now()
        var t = sekarang.toLocalDate().atTime(a.jam, a.menit)
        if (!t.isAfter(sekarang)) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun tujuan(c: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, id, Intent(c, AlarmReceiver::class.java).putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun jadwalkan(c: Context, a: Alarm) {
        val am = c.getSystemService(AlarmManager::class.java)
        val buka = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(waktuBerikut(a), buka), tujuan(c, a.id))
    }

    fun batalkan(c: Context, id: Int) {
        c.getSystemService(AlarmManager::class.java).cancel(tujuan(c, id))
    }

    fun semua(c: Context) {
        for (a in muatAlarm(c)) {
            if (a.aktif) jadwalkan(c, a) else batalkan(c, a.id)
        }
    }
}

fun siapkanSaluran(c: Context) {
    val nm = c.getSystemService(NotificationManager::class.java)
    val suara = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val atribut = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val saluran = NotificationChannel(SALURAN, "Alarm catat", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Pengingat waktu mencatat"
        setSound(suara, atribut)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    nm.createNotificationChannel(saluran)
}

private fun tampilkan(c: Context, a: Alarm) {
    siapkanSaluran(c)
    val nm = c.getSystemService(NotificationManager::class.java)
    val bendera = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val layarPenuh = PendingIntent.getActivity(
        c, a.id,
        Intent(c, AlarmActivity::class.java).putExtra("id", a.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        bendera
    )
    val buka = PendingIntent.getActivity(
        c, a.id + 100000,
        Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        bendera
    )
    val matikan = PendingIntent.getBroadcast(
        c, a.id + 200000,
        Intent(c, AlarmReceiver::class.java).setAction(AKSI_MATIKAN).putExtra("id", a.id),
        bendera
    )
    val ikon = Icon.createWithResource(c, android.R.drawable.ic_lock_idle_alarm)
    val n = Notification.Builder(c, SALURAN)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Waktunya mencatat")
        .setContentText(a.nama + " (" + jamTeks(a) + ")")
        .setCategory(Notification.CATEGORY_ALARM)
        .setOngoing(true)
        .setAutoCancel(false)
        .setTimeoutAfter(10 * 60 * 1000L)
        .setContentIntent(buka)
        .setFullScreenIntent(layarPenuh, true)
        .addAction(Notification.Action.Builder(ikon, "Matikan", matikan).build())
        .build()
    n.flags = n.flags or Notification.FLAG_INSISTENT
    nm.notify(a.id, n)
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (intent.action == AKSI_MATIKAN) {
            c.getSystemService(NotificationManager::class.java).cancel(id)
            return
        }
        val a = muatAlarm(c).firstOrNull { it.id == id } ?: return
        if (!a.aktif) return
        Penjadwal.jadwalkan(c, a) // jadwalkan lagi untuk besok
        tampilkan(c, a)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        Penjadwal.semua(c)
    }
}
