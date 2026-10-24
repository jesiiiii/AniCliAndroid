package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    var selectedAnime by remember { mutableStateOf<Anime?>(null) }
    var episodes by remember { mutableStateOf(emptyList<Episode>()) }
    var episodesLoading by remember { mutableStateOf(false) }
    var streamLoading by remember { mutableStateOf(false) }
    var stream by remember { mutableStateOf<StreamInfo?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF17152D), Color(0xFF090B13), Color(0xFF090B13))))
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                Spacer(Modifier.height(12.dp))
                Text("عالمك، حلقة بعد حلقة", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("ابحث عن الأنمي، اختر حلقة، وشاهد مباشرة", color = Color(0xFFB5B8C9), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("اسم الأنمي") },
                    placeholder = { Text("مثال: Naruto") },
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !loading,
                    onClick = {
                        if (query.isBlank()) {
                            status = "اكتب اسم الأنمي أولاً"
                        } else {
                            loading = true
                            status = "جارٍ البحث..."
                            results = emptyList()
                            selectedAnime = null
                            episodes = emptyList()
                            stream = null
                            scope.launch {
                                try {
                                    results = AnimeApi.search(query)
                                    status = if (results.isEmpty()) "لم تُعثر نتائج." else "النتائج: ${results.size}"
                                } catch (e: Exception) {
                                    status = e.message ?: "حدث خطأ أثناء البحث"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7A6FF))
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF171126), strokeWidth = 2.dp)
                    else Text("بحث عن الأنمي", color = Color(0xFF171126), fontWeight = FontWeight.Bold)
                }
                Text(status, color = Color(0xFFB5B8C9), fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            if (stream != null) {
                item {
                    Text("المشغّل", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    AnimeVideoPlayer(streamUrl = stream!!.url)
                    Text("الجودة: ${stream!!.quality}", color = Color(0xFFB7A6FF), fontSize = 12.sp)
                }
            } else if (streamLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(" جارٍ تجهيز رابط المشاهدة...", color = Color.White, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }

            if (selectedAnime != null) {
                val anime = selectedAnime!!
                item {
                    Text(anime.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("اختر الحلقة التي تريد مشاهدتها", color = Color(0xFFB5B8C9), fontSize = 13.sp)
                }
                if (episodesLoading) {
                    item { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                } else if (episodes.isEmpty()) {
                    item { Text("لا توجد حلقات متاحة لهذا العنوان.", color = Color(0xFFB5B8C9)) }
                } else {
                    items(episodes, key = { "${anime.id}-${it.number}" }) { episode ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !streamLoading) {
                                streamLoading = true
                                stream = null
                                status = "جارٍ تجهيز الحلقة ${episode.number}..."
                                scope.launch {
                                    try {
                                        stream = AnimeApi.resolveStream(
                                            animeId = anime.id,
                                            episode = episode.number,
                                            animeType = anime.animeType
                                        )
                                        status = "تم تجهيز الحلقة ${episode.number}"
                                    } catch (e: Exception) {
                                        status = e.message ?: "تعذّر تشغيل هذه الحلقة"
                                    } finally {
                                        streamLoading = false
                                    }
                                }
                            },
                            color = Color(0xAA1C2030),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${episode.type} ${episode.number}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("▶", color = Color(0xFFB7A6FF), fontSize = 18.sp)
                            }
                        }
                    }
                }
            } else {
                items(results, key = { it.id }) { anime ->
                    Surface(
                        color = Color(0xAA1C2030),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedAnime = anime
                            episodes = emptyList()
                            stream = null
                            episodesLoading = true
                            status = "جارٍ تحميل حلقات ${anime.title}..."
                            scope.launch {
                                try {
                                    episodes = AnimeApi.getEpisodes(anime.id)
                                    status = if (episodes.isEmpty()) "لم تُرجع الخدمة حلقات لهذا الأنمي." else "عدد الحلقات: ${episodes.size}"
                                } catch (e: Exception) {
                                    status = e.message ?: "تعذّر جلب الحلقات"
                                } finally {
                                    episodesLoading = false
                                }
                            }
                        }
                    ) {
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
            item {
                Spacer(Modifier.height(8.dp))
                Text("نسخة تجريبية • 0.4.0", color = Color(0xFF777D92), fontSize = 11.sp)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun AnimeVideoPlayer(streamUrl: String) {
    val context = LocalContext.current
    val player = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().height(230.dp)
    )
}
