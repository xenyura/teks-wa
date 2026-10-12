package com.bangis.tekswa

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Layar yang muncul penuh saat alarm berbunyi
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val id = intent.getIntExtra("id", -1)
        val a = muatAlarm(this).firstOrNull { it.id == id }
        val nama = a?.nama ?: "Catat catatan"
        val jam = if (a != null) jamTeks(a) else ""

        fun matikan() {
            getSystemService(NotificationManager::class.java).cancel(id)
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E4A47))
                        .systemBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Waktunya mencatat", color = Color(0xFFBFE3DA), fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(jam, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        nama,
                        color = Color(0xFFF5B301),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = {
                            matikan()
                            startActivity(Intent(this@AlarmActivity, MainActivity::class.java))
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5B301),
                            contentColor = Color(0xFF2A2000)
                        )
                    ) { Text("Buka catatan", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            matikan()
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) { Text("Matikan", color = Color.White) }
                }
            }
        }
    }
}
