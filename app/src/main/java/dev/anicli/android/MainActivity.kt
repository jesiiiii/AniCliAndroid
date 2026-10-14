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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    var status by remember { mutableStateOf("ابحث عن أنمي بالاسم") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(emptyList<Anime>()) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF17152D), Color(0xFF090B13), Color(0xFF090B13))))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFB7A6FF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color(0xFF151126), fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("AniCli", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("ANIME • YOUR WAY", fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFB7A6FF))
                }
            }
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
                enabled = !loading,
                onClick = {
                    if (query.isBlank()) {
                        status = "اكتب اسم الأنمي أولاً"
                    } else {
                        loading = true
                        status = "جارٍ البحث..."
                        results = emptyList()
                        scope.launch {
                            try {
                                results = AnimeApi.search(query)
                                status = if (results.isEmpty()) "لم تُعثر نتائج. تحقق من إعداد خادم البحث." else "النتائج: ${results.size}"
                            } catch (e: Exception) {
                                status = e.message ?: "حدث خطأ أثناء البحث"
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7A6FF))
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF171126), strokeWidth = 2.dp)
                else Text("بحث عن الأنمي", color = Color(0xFF171126), fontWeight = FontWeight.Bold)
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xAA1C2030), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("حالة البحث", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(status, color = Color(0xFFB5B8C9), fontSize = 13.sp)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = { it.id }) { anime ->
                    Surface(color = Color(0xAA1C2030), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(anime.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            anime.description?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = Color(0xFFB5B8C9), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("نسخة تجريبية • 0.3.0", color = Color(0xFF777D92), fontSize = 11.sp)
        }
    }
}
