package com.bangis.tekswa

import android.Manifest
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun punyaIzinNotif(c: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun layarPenuhBoleh(c: Context): Boolean =
    Build.VERSION.SDK_INT < 34 ||
        c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

@Composable
fun LayarAlarm(gelap: Boolean, tick: Int, kembali: () -> Unit) {
    val ctx = LocalContext.current
    val daftar = remember { mutableStateListOf<Alarm>().apply { addAll(muatAlarm(ctx)) } }
    var izinNotif by remember(tick) { mutableStateOf(punyaIzinNotif(ctx)) }
    val layarPenuhOk = remember(tick) { layarPenuhBoleh(ctx) }
    val peluncur = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        izinNotif = it
    }

    val hero = if (gelap) Color(0xFF0A2A2C) else Color(0xFF0E4A47)
    val lembar = if (gelap) Color(0xFF0B1F22) else Color(0xFFF2F6F7)
    val tinta = if (gelap) Color(0xFFE8F1F3) else Color(0xFF12303A)
    val kuning = Color(0xFFF5B301)
    val tintaKuning = Color(0xFF2A2000)
    val hijau = if (gelap) Color(0xFF1FA75E) else Color(0xFF0F7A45)

    fun simpan() {
        simpanAlarm(ctx, daftar.toList())
        Penjadwal.semua(ctx)
    }

    fun mintaIzinNotif() {
        if (Build.VERSION.SDK_INT >= 33) peluncur.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun pilihJam(jam: Int, menit: Int, selesai: (Int, Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> selesai(h, m) }, jam, menit, true).show()
    }

    Column(Modifier.fillMaxSize().background(lembar)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(hero)
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 24.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Alarm catat",
                        color = Color(0xFFD8EFE9),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    TextButton(onClick = kembali) { Text("Kembali", color = Color(0xFFBFE3DA)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Alarm berbunyi tiap hari pada jam yang kamu atur, untuk mengingatkan kamu mencatat.",
                    color = Color(0xFFBFE3DA),
                    fontSize = 14.sp
                )
            }

            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                if (!izinNotif) {
                    Surface(color = kuning, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "Notifikasi belum diizinkan, jadi alarm tidak akan tampil.",
                                color = tintaKuning
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { mintaIzinNotif() },
                                colors = ButtonDefaults.buttonColors(containerColor = hijau, contentColor = Color.White)
                            ) { Text("Izinkan notifikasi") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (!layarPenuhOk) {
                    Surface(color = kuning, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "Agar layar menyala penuh saat alarm berbunyi, aktifkan izin layar penuh. " +
                                    "Tanpa izin ini, alarm tampil sebagai notifikasi biasa.",
                                color = tintaKuning
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    ctx.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:" + ctx.packageName)
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = hijau, contentColor = Color.White)
                            ) { Text("Buka pengaturan") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (daftar.isEmpty()) {
                    Text(
                        "Belum ada alarm. Tekan Tambah alarm di bawah.",
                        color = tinta,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                for (i in daftar.indices) {
                    val a = daftar[i]
                    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(jamTeks(a), color = tinta, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Switch(
                                checked = a.aktif,
                                onCheckedChange = { daftar[i] = a.copy(aktif = it); simpan() }
                            )
                        }
                        OutlinedTextField(
                            value = a.nama,
                            onValueChange = { daftar[i] = a.copy(nama = it); simpan() },
                            label = { Text("Nama alarm") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                pilihJam(a.jam, a.menit) { h, m ->
                                    daftar[i] = a.copy(jam = h, menit = m)
                                    simpan()
                                }
                            }) { Text("Ubah jam") }
                            TextButton(onClick = {
                                Penjadwal.batalkan(ctx, a.id)
                                daftar.removeAt(i)
                                simpan()
                            }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
                        }
                        HorizontalDivider()
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        pilihJam(8, 0) { h, m ->
                            val idBaru = (daftar.maxOfOrNull { it.id } ?: 0) + 1
                            daftar.add(Alarm(idBaru, h, m, "Catat catatan", true))
                            simpan()
                            if (!izinNotif) mintaIzinNotif()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("+ Tambah alarm", color = tinta, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }
}
