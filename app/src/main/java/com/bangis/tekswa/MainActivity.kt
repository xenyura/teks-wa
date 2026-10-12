package com.bangis.tekswa

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

data class Bagian(val t: String, val b: String)

private val HARI = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

fun tanggalHariIni(): String {
    val d = LocalDate.now()
    val hari = HARI[d.dayOfWeek.value - 1]
    return String.format(Locale.US, "%s %02d/%02d/%d", hari, d.dayOfMonth, d.monthValue, d.year)
}

// Teks yang disalin ke WhatsApp (memakai tanda * dan _ untuk tebal dan miring)
fun susunTeks(judul: String, tgl: String, items: List<Bagian>): String {
    val sb = StringBuilder()
    val j = judul.trim()
    if (j.isNotEmpty()) sb.append("*").append(j).append("*\n")
    sb.append("*Tanggal:* _").append(tgl).append("_")
    for (x in items) {
        val t = x.t.trim().uppercase()
        val b = x.b.trim()
        if (t.isEmpty() && b.isEmpty()) continue
        sb.append("\n\n")
        if (t.isNotEmpty()) sb.append("*").append(t).append("*\n")
        sb.append(b)
    }
    return sb.toString()
}

// Tampilan di layar: tebal dan miring sungguhan, tanpa tanda * dan _
fun susunTampilan(judul: String, tgl: String, items: List<Bagian>): AnnotatedString {
    val tebal = SpanStyle(fontWeight = FontWeight.Bold)
    val miring = SpanStyle(fontStyle = FontStyle.Italic)
    return buildAnnotatedString {
        val j = judul.trim()
        if (j.isNotEmpty()) {
            withStyle(tebal) { append(j) }
            append("\n")
        }
        withStyle(tebal) { append("Tanggal:") }
        append(" ")
        withStyle(miring) { append(tgl) }
        for (x in items) {
            val t = x.t.trim().uppercase()
            val b = x.b.trim()
            if (t.isEmpty() && b.isEmpty()) continue
            append("\n\n")
            if (t.isNotEmpty()) {
                withStyle(tebal) { append(t) }
                append("\n")
            }
            append(b)
        }
    }
}

private fun muat(c: Context): Pair<String, List<Bagian>> {
    val p = c.getSharedPreferences("tekswa", Context.MODE_PRIVATE)
    val s = p.getString("data", null)
    if (s != null) {
        try {
            val o = JSONObject(s)
            val a = o.getJSONArray("items")
            val l = (0 until a.length()).map {
                val i = a.getJSONObject(it)
                Bagian(i.getString("t"), i.getString("b"))
            }
            return Pair(o.getString("judul"), l)
        } catch (e: Exception) {
            // data rusak: pakai isi awal
        }
    }
    return Pair(
        "BANG IS",
        listOf(Bagian("TOPI BASEBALL", "Abu tua 1\nMarun 3"), Bagian("POLO GARIS", "Coklat. M6"))
    )
}

private fun simpanData(c: Context, judul: String, items: List<Bagian>) {
    val a = JSONArray()
    for (x in items) a.put(JSONObject().put("t", x.t).put("b", x.b))
    val o = JSONObject().put("judul", judul).put("items", a)
    c.getSharedPreferences("tekswa", Context.MODE_PRIVATE).edit().putString("data", o.toString()).apply()
}

class MainActivity : ComponentActivity() {
    private val tgl = mutableStateOf(tanggalHariIni())
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        siapkanSaluran(this)
        Penjadwal.semua(this)
        setContent { TeksWaApp(tgl, tick.intValue) }
    }

    override fun onResume() {
        super.onResume()
        tgl.value = tanggalHariIni()
        tick.intValue = tick.intValue + 1
        // Membuka aplikasi mematikan alarm yang sedang berbunyi
        getSystemService(android.app.NotificationManager::class.java).cancelAll()
    }
}

@Composable
fun TeksWaApp(tglState: MutableState<String>, tick: Int) {
    val gelap = isSystemInDarkTheme()
    var halaman by remember { mutableStateOf("catat") }
    MaterialTheme(colorScheme = if (gelap) darkColorScheme() else lightColorScheme()) {
        if (halaman == "alarm") {
            BackHandler { halaman = "catat" }
            LayarAlarm(gelap, tick) { halaman = "catat" }
        } else {
            Layar(tglState, gelap) { halaman = "alarm" }
        }
    }
}

@Composable
fun Layar(tglState: MutableState<String>, gelap: Boolean, bukaAlarm: () -> Unit) {
    val ctx = LocalContext.current
    val awal = remember { muat(ctx) }
    var judul by remember { mutableStateOf(awal.first) }
    val items = remember { mutableStateListOf<Bagian>().apply { addAll(awal.second) } }
    var tgl by tglState
    var tersalin by remember { mutableStateOf(false) }
    var tanyaKosong by remember { mutableStateOf(false) }

    val hero = if (gelap) Color(0xFF0A2A2C) else Color(0xFF0E4A47)
    val lembar = if (gelap) Color(0xFF0B1F22) else Color(0xFFF2F6F7)
    val bar = if (gelap) Color(0xFF12292D) else Color.White
    val tinta = if (gelap) Color(0xFFE8F1F3) else Color(0xFF12303A)
    val gelembung = if (gelap) Color(0xFF005C4B) else Color(0xFFD9FDD3)
    val tintaGelembung = if (gelap) Color(0xFFE9FFF7) else Color(0xFF111B21)
    val hijau = if (gelap) Color(0xFF1FA75E) else Color(0xFF0F7A45)
    val kuning = Color(0xFFF5B301)
    val tintaKuning = Color(0xFF2A2000)

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            tgl = tanggalHariIni()
        }
    }
    LaunchedEffect(tersalin) {
        if (tersalin) {
            delay(1600)
            tersalin = false
        }
    }

    fun simpan() = simpanData(ctx, judul, items.toList())

    fun geser(i: Int, d: Int) {
        val j = i + d
        if (j in items.indices) {
            val k = items[i]
            items[i] = items[j]
            items[j] = k
            simpan()
        }
    }

    val salin = {
        val teks = susunTeks(judul, tanggalHariIni(), items)
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("Teks WA", teks))
        tersalin = true
    }

    val kirim = {
        val teks = susunTeks(judul, tanggalHariIni(), items)
        val dasar = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, teks)
        }
        try {
            ctx.startActivity(Intent(dasar).setPackage("com.whatsapp"))
        } catch (e: ActivityNotFoundException) {
            ctx.startActivity(Intent.createChooser(dasar, "Kirim lewat"))
        }
    }

    Column(Modifier.fillMaxSize().background(lembar).imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(hero)
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 28.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Teks WA",
                        color = Color(0xFFD8EFE9),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Row {
                        TextButton(onClick = bukaAlarm) {
                            Text("Alarm", color = Color(0xFFF5B301), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { tanyaKosong = true }) {
                            Text("Kosongkan", color = Color(0xFFBFE3DA))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(color = kuning, shape = RoundedCornerShape(50)) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tgl, color = tintaKuning, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("otomatis hari ini", color = tintaKuning, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = gelembung,
                    shape = RoundedCornerShape(
                        topStart = 14.dp, topEnd = 0.dp, bottomEnd = 14.dp, bottomStart = 14.dp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                ) {
                    Text(
                        susunTampilan(judul, tgl, items),
                        color = tintaGelembung,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }

            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                OutlinedTextField(
                    value = judul,
                    onValueChange = { judul = it; simpan() },
                    label = { Text("Judul teks") },
                    singleLine = true,
                    textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )

                for (i in items.indices) {
                    val x = items[i]
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        OutlinedTextField(
                            value = x.t,
                            onValueChange = { items[i] = x.copy(t = it); simpan() },
                            placeholder = { Text("Judul bagian, mis. TOPI BASEBALL") },
                            singleLine = true,
                            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = x.b,
                            onValueChange = { items[i] = x.copy(b = it); simpan() },
                            placeholder = { Text("Satu item per baris, mis. Abu tua 1") },
                            minLines = 3,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { geser(i, -1) }) { Text("Naik") }
                            TextButton(onClick = { geser(i, 1) }) { Text("Turun") }
                            TextButton(onClick = { items.removeAt(i); simpan() }) {
                                Text("Hapus", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { items.add(Bagian("", "")); simpan() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+ Tambah bagian", color = tinta, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Surface(color = bar, shadowElevation = 8.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = kirim,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, hijau)
                ) {
                    Text("Buka WhatsApp", color = tinta, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = salin,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tersalin) kuning else hijau,
                        contentColor = if (tersalin) tintaKuning else Color.White
                    )
                ) {
                    Text(if (tersalin) "Tersalin ✓" else "Salin teks", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (tanyaKosong) {
        AlertDialog(
            onDismissRequest = { tanyaKosong = false },
            title = { Text("Kosongkan semua isi?") },
            text = { Text("Semua bagian akan dihapus. Judul teks tetap.") },
            confirmButton = {
                TextButton(onClick = {
                    items.clear()
                    items.add(Bagian("", ""))
                    simpan()
                    tanyaKosong = false
                }) { Text("Kosongkan") }
            },
            dismissButton = { TextButton(onClick = { tanyaKosong = false }) { Text("Batal") } }
        )
    }
}
