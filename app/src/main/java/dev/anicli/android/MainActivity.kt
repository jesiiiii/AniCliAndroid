package dev.anicli.android

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val Ink = Color(0xFF202438)
private val Muted = Color(0xFF687087)
private val Accent = Color(0xFF6650C8)
private val Canvas = Color(0xFFF7F7FC)
private val Card = Color.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Accent,
                    onPrimary = Color.White,
                    background = Canvas,
                    surface = Card,
                    onSurface = Ink,
                    secondary = Color(0xFFEBE7FF)
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    UchihaHome()
                }
            }
        }
    }
}

@Composable
private fun UchihaHome() {
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ابحث عن أنمي بالاسم أو بالعربية") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(emptyList<Anime>()) }
    var selectedAnime by remember { mutableStateOf<Anime?>(null) }
    var episodes by remember { mutableStateOf(emptyList<Episode>()) }
    var episodesLoading by remember { mutableStateOf(false) }
    var streamLoading by remember { mutableStateOf(false) }
    var stream by remember { mutableStateOf<StreamInfo?>(null) }
    var lastEpisode by remember { mutableStateOf<Episode?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        if (query.isBlank()) {
            status = "اكتب اسم الأنمي أولاً."
            return
        }
        loading = true
        status = "جارٍ البحث في المصدر..."
        results = emptyList()
        selectedAnime = null
        episodes = emptyList()
        stream = null
        lastEpisode = null
        scope.launch {
            try {
                results = AnimeApi.search(query)
                status = if (results.isEmpty()) "لم يُعثر على نتائج مطابقة." else "عُثر على ${results.size} نتيجة."
            } catch (e: Exception) {
                status = e.message ?: "تعذّر الاتصال بالمصدر. تحقق من الإنترنت ثم أعد المحاولة."
            } finally {
                loading = false
            }
        }
    }

    fun loadEpisodes(anime: Anime) {
        selectedAnime = anime
        episodes = emptyList()
        stream = null
        lastEpisode = null
        episodesLoading = true
        status = "جارٍ تحميل الحلقات..."
        scope.launch {
            try {
                episodes = AnimeApi.getEpisodes(anime.id)
                status = if (episodes.isEmpty()) "لا توجد حلقات متاحة في المصدر لهذا العنوان." else "الحلقات المتاحة: ${episodes.size}"
            } catch (e: Exception) {
                status = e.message ?: "تعذّر جلب الحلقات."
            } finally {
                episodesLoading = false
            }
        }
    }

    fun loadStream(anime: Anime, episode: Episode) {
        lastEpisode = episode
        streamLoading = true
        stream = null
        status = "جارٍ تجهيز رابط المشاهدة..."
        scope.launch {
            try {
                stream = AnimeApi.resolveStream(anime.id, episode.number, anime.animeType)
                showPlayer = true
                status = "تم تجهيز الحلقة ${episode.number}."
            } catch (e: Exception) {
                status = e.message ?: "تعذّر تجهيز الفيديو. يمكنك إعادة المحاولة."
            } finally {
                streamLoading = false
            }
        }
    }

    BackHandler(enabled = showPlayer || selectedAnime != null) {
        if (showPlayer) {
            showPlayer = false
        } else {
            selectedAnime = null
            episodes = emptyList()
            stream = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Accent, shape = RoundedCornerShape(15.dp), modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("う", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Uchiha Anime", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("أنميك المفضل، في مكان واحد", fontSize = 12.sp, color = Muted)
                }
            }

            if (showPlayer && stream != null) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    TextButton(onClick = { showPlayer = false }) { Text("→ العودة إلى الحلقات") }
                    Text(selectedAnime?.title.orEmpty(), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    AnimeVideoPlayer(streamUrl = stream!!.url)
                    Text("الجودة المتاحة: ${stream!!.quality}", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("إذا لم يبدأ التشغيل، أعد طلب رابط الحلقة من القائمة.", color = Muted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
                ) {
                    item {
                        Text(
                            if (selectedAnime == null) "اكتشف عالم الأنمي" else selectedAnime!!.title,
                            fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink
                        )
                        Text(
                            if (selectedAnime == null) "ابحث في المصدر المتصل، ثم افتح العنوان واختر الحلقة." else "اختر حلقة متاحة من المصدر الحقيقي.",
                            fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )
                    }

                    if (selectedAnime == null) {
                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("اسم الأنمي") },
                                placeholder = { Text("مثال: Naruto أو ون بيس") },
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { search() })
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { search() },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                else Text("بحث عن الأنمي", fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(status, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                if (!loading && results.isEmpty() && query.isNotBlank()) {
                                    TextButton(onClick = { search() }) { Text("إعادة المحاولة") }
                                }
                            }
                        }
                        items(results, key = { it.id }) { anime ->
                            Surface(
                                color = Card,
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().clickable { loadEpisodes(anime) }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (anime.posterUrl != null) {
                                        AsyncImage(
                                            model = anime.posterUrl,
                                            contentDescription = "غلاف ${anime.title}",
                                            modifier = Modifier.size(width = 86.dp, height = 116.dp)
                                        )
                                    } else {
                                        Surface(color = Color(0xFFECEAF5), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(width = 86.dp, height = 116.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Text("Anime", color = Accent, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(anime.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                                        anime.description?.let {
                                            Spacer(Modifier.height(5.dp))
                                            Text(it, color = Muted, fontSize = 12.sp, maxLines = 3)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("عرض التفاصيل والحلقات  ←", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            TextButton(onClick = {
                                selectedAnime = null
                                episodes = emptyList()
                                stream = null
                            }) { Text("→ العودة إلى نتائج البحث") }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(status, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                if (!episodesLoading) TextButton(onClick = { loadEpisodes(selectedAnime!!) }) { Text("تحديث الحلقات") }
                            }
                        }
                        if (episodesLoading) {
                            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                        } else if (episodes.isEmpty()) {
                            item { Text("لا توجد حلقات معروضة. يمكنك تحديث القائمة أو تجربة عنوان آخر.", color = Muted, fontSize = 14.sp) }
                        } else {
                            items(episodes, key = { "${selectedAnime!!.id}-${it.number}" }) { episode ->
                                Surface(
                                    color = Card,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = !streamLoading) {
                                        loadStream(selectedAnime!!, episode)
                                    }
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${episode.type} ${episode.number}", color = Ink, fontWeight = FontWeight.Bold)
                                            Text("اضغط لطلب رابط المشاهدة", color = Muted, fontSize = 12.sp)
                                        }
                                        Text(if (streamLoading && lastEpisode == episode) "…" else "▶", color = Accent, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                        if (!streamLoading && lastEpisode != null && stream == null) {
                            item {
                                Button(onClick = { selectedAnime?.let { a -> lastEpisode?.let { e -> loadStream(a, e) } } }, modifier = Modifier.fillMaxWidth()) {
                                    Text("إعادة محاولة تشغيل الحلقة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeVideoPlayer(streamUrl: String) {
    val context = LocalContext.current
    var speed by remember(streamUrl) { mutableFloatStateOf(1f) }
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
    Column {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().height(235.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) { Text("−10 ث") }
            OutlinedButton(onClick = { player.seekTo(player.currentPosition + 10_000) }) { Text("+10 ث") }
            Spacer(Modifier.weight(1f))
            Text("السرعة", color = Muted, fontSize = 12.sp)
            listOf(0.5f, 0.75f, 1f, 1.5f, 2f).forEach { value ->
                TextButton(
                    onClick = {
                        speed = value
                        player.playbackParameters = PlaybackParameters(value)
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(if (speed == value) "${value}x" else "${value}x", color = if (speed == value) Accent else Muted, fontSize = 11.sp)
                }
            }
        }
    }
}
