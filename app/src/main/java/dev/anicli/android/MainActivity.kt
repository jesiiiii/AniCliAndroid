package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFB7A6FF),
                    background = Color(0xFF090B13),
                    surface = Color(0xFF171A28)
                )
            ) {
                AniCliHome()
            }
        }
    }
}

@Composable
private fun AniCliHome() {
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("اكتب اسم الأنمي للبدء") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF17152D), Color(0xFF090B13), Color(0xFF090B13))
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFB7A6FF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color(0xFF151126), fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("AniCli", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("ANIME • YOUR WAY", fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFB7A6FF))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("عالمك، حلقة بعد حلقة", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("ابحث عن الأنمي الذي تريد مشاهدته", color = Color(0xFFB5B8C9), fontSize = 14.sp)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("اسم الأنمي") },
                placeholder = { Text("مثال: Naruto") },
                shape = RoundedCornerShape(18.dp)
            )

            Button(
                onClick = {
                    status = if (query.isBlank()) "اكتب اسم الأنمي أولاً" else "واجهة البحث جاهزة، وسيُربط مصدر البيانات في الخطوة التالية."
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7A6FF))
            ) {
                Text("بحث عن الأنمي", color = Color(0xFF171126), fontWeight = FontWeight.Bold)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xAA1C2030),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("جاهز للانطلاق", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(status, color = Color(0xFFB5B8C9), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("نسخة تجريبية • 0.1.0", color = Color(0xFF777D92), fontSize = 11.sp)
        }
    }
}
