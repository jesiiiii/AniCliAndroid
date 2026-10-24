package dev.anicli.android

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.anicli.android.ui.theme.UchihaTheme
import kotlinx.coroutines.delay
import java.util.Locale

class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ANIME_ID = "animeId"
        const val EXTRA_ANIME_TITLE = "animeTitle"
        const val EXTRA_EPISODE_NUMBER = "episodeNumber"

        fun intent(context: Context, animeId: String, animeTitle: String, episodeNumber: String): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ANIME_ID, animeId)
                putExtra(EXTRA_ANIME_TITLE, animeTitle)
                putExtra(EXTRA_EPISODE_NUMBER, episodeNumber)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val animeId = intent.getStringExtra(EXTRA_ANIME_ID).orEmpty()
        val animeTitle = intent.getStringExtra(EXTRA_ANIME_TITLE).orEmpty()
        val episodeNumber = intent.getStringExtra(EXTRA_EPISODE_NUMBER).orEmpty()
        if (animeId.isBlank() || episodeNumber.isBlank()) {
            finish()
            return
        }
        setContent {
            UchihaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    PlayerScreen(
                        animeId = animeId,
                        animeTitle = animeTitle,
                        episodeNumber = episodeNumber,
                        onFinish = { finish() },
                        onOrientation = { landscape ->
                            requestedOrientation = if (landscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

private data class ResolveState(
    val resolving: Boolean = true,
    val streams: List<StreamOption> = emptyList(),
    val selected: Int = -1,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    animeId: String,
    animeTitle: String,
    episodeNumber: String,
    onFinish: () -> Unit,
    onOrientation: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var resolveState by remember { mutableStateOf(ResolveState()) }
    var activeUrl by remember { mutableStateOf<String?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    var nextEpisodeNumber by remember { mutableStateOf<String?>(null) }
    var autoAdvanced by remember { mutableStateOf(false) }
    val player = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(animeId, episodeNumber) {
        nextEpisodeNumber = runCatching {
            val episodes = SlayerApi.getAnimeWithEpisodes(animeId).episodes.sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
            val currentIndex = episodes.indexOfFirst { it.number == episodeNumber }
            episodes.getOrNull(currentIndex + 1)?.number
        }.getOrNull()
    }

    // An expired or dead link is retried through a fresh resolve attempt.
    LaunchedEffect(attempt) {
        resolveState = ResolveState()
        playbackError = null
        activeUrl = null
        val resolved = try {
            SlayerApi.resolveStreams(animeId, episodeNumber)
        } catch (e: Exception) {
            resolveState = ResolveState(resolving = false, error = PlayerUi.friendly(e, "تعذّر تجهيز الحلقة."))
            return@LaunchedEffect
        }
        if (resolved.streams.isEmpty()) {
            resolveState = ResolveState(
                resolving = false,
                error = resolved.diagnostic ?: "لم يتم العثور على روابط مشاهدة لهذه الحلقة."
            )
            return@LaunchedEffect
        }
        resolveState = ResolveState(resolving = false, streams = resolved.streams, selected = 0)
        // Start the first available stream automatically; the quality chips remain available for switching.
        activeUrl = resolved.streams.first().url
    }

    // a chosen link is verified before and after hand-off so a dead chip never produces a black screen
    LaunchedEffect(activeUrl) {
        val url = activeUrl ?: return@LaunchedEffect
        val ok = SlayerApi.verifyStream(url)
        if (!ok) {
            val currentIndex = resolveState.streams.indexOfFirst { it.url == url }
            if (resolveState.selected == -1 || resolveState.selected == currentIndex) {
                val nextIndex = (currentIndex + 1).coerceAtMost(resolveState.streams.size - 1)
                if (currentIndex >= 0 && nextIndex != currentIndex) {
                    activeUrl = resolveState.streams[nextIndex].url
                    resolveState = resolveState.copy(selected = nextIndex)
                    return@LaunchedEffect
                }
                playbackError = "الروابط المتوفرة لا تعمل حالياً، جرّب جودة أخرى أو أعد المحاولة لاحقاً."
            } else {
                playbackError = "الجودة المختارة غير متاحة الآن، اختر جودة أخرى أو أعد المحاولة."
                resolveState = resolveState.copy(selected = -1)
            }
            activeUrl = null
            return@LaunchedEffect
        }
        val savedPosition = PlaybackProgress.get(context, animeId, episodeNumber)
        val currentPosition = player.currentPosition.takeIf { it > 5_000L } ?: savedPosition
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        if (currentPosition > 0L) player.seekTo(currentPosition)
        player.playWhenReady = true
        playbackError = null
    }

    // Persist a checkpoint locally while watching and when playback reaches the end.
    LaunchedEffect(player, animeId, episodeNumber) {
        while (true) {
            PlaybackProgress.save(
                context,
                animeId,
                episodeNumber,
                player.currentPosition,
                player.duration.coerceAtLeast(0L)
            )
            if (player.playbackState == Player.STATE_ENDED) {
                PlaybackProgress.save(context, animeId, episodeNumber, player.duration, player.duration)
            }
            delay(2_000)
        }
    }

    LaunchedEffect(fullscreen) { onOrientation(fullscreen) }

    DisposableEffect(player) {
        val activity = context as? ComponentActivity
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        val playbackListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val autoNextEnabled = context.getSharedPreferences("uchiha_settings", 0).getBoolean("auto_next", true)
                if (playbackState == Player.STATE_ENDED && autoNextEnabled && !autoAdvanced) {
                    val next = nextEpisodeNumber
                    if (!next.isNullOrBlank()) {
                        autoAdvanced = true
                        PlaybackProgress.save(context, animeId, episodeNumber, player.currentPosition, player.duration.coerceAtLeast(0L))
                        context.startActivity(PlayerActivity.intent(context, animeId, animeTitle, next))
                        onFinish()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "انقطع الاتصال أثناء التشغيل. تحقق من الإنترنت ثم أعد المحاولة."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "صيغة الفيديو من هذا الخادم غير مدعومة. جرّب خادماً أو جودة أخرى."
                    else -> "تعذّر تشغيل هذا الرابط. جرّب خادماً آخر أو اضغط إعادة المحاولة."
                }
            }
        }
        player.addListener(playbackListener)
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            player.removeListener(playbackListener)
            activity?.lifecycle?.removeObserver(observer)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            player.release()
        }
    }


    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF100D1A))
            .statusBarsPadding()
    ) {
        if (!fullscreen) {
            PlayerTopBar(
                title = "$animeTitle • الحلقة $episodeNumber",
                fullscreen = fullscreen,
                onBack = onFinish,
                onToggleFullscreen = { fullscreen = true }
            )
        }
        Box(
            Modifier
                .then(if (fullscreen) Modifier.fillMaxWidth().weight(1f) else Modifier.height(240.dp))
                .clip(RoundedCornerShape(if (fullscreen) 0.dp else 16.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
            if (resolveState.resolving) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("جارٍ تجهيز الحلقة...", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        when {
            resolveState.resolving -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = PlayerUi.accent)
                    Text("جارٍ تجهيز رابط المشاهدة...", color = Color(0xFFC9C2DC), fontSize = 14.sp)
                }
            }
            playbackError != null -> {
                PlayerErrorBlock(
                    title = "حدث خطأ أثناء التشغيل",
                    message = playbackError.orEmpty(),
                    onRetry = { activeUrl = null; attempt += 1 }
                )
            }
            resolveState.error != null -> {
                PlayerErrorBlock(
                    title = "تعذّر تشغيل الحلقة",
                    message = resolveState.error.orEmpty(),
                    onRetry = { attempt += 1 }
                )
                if (resolveState.streams.isNotEmpty()) {
                    QualityChipsRow(
                        streams = resolveState.streams,
                        selectedIndex = resolveState.selected,
                        onSelect = { index ->
                            resolveState = resolveState.copy(selected = index)
                            activeUrl = resolveState.streams[index].url
                        }
                    )
                }
            }
            activeUrl != null -> {
                if (!fullscreen) {
                    QualityChipsRow(
                        streams = resolveState.streams,
                        selectedIndex = resolveState.selected,
                        onSelect = { index ->
                            resolveState = resolveState.copy(selected = index)
                            playbackError = null
                            player.seekTo(0)
                            activeUrl = null
                            activeUrl = resolveState.streams[index].url
                        }
                    )
                    PlayerControlBar(player, onFullscreen = { fullscreen = true })
                }
            }
            else -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("اختر جودة لتشغيل الحلقة", color = Color(0xFF9A93AE), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PlayerErrorBlock(title: String, message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            message,
            color = Color(0xFFC7C2D6),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PlayerUi.accent)
        ) {
            Text("إعادة المحاولة", color = Color.White)
        }
    }
}

@Composable
fun PlayerTopBar(
    title: String,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
        }
        Text(
            title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (fullscreen) "إنهاء ملء الشاشة" else "ملء الشاشة",
                tint = Color.White
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis < 0) return "--:--"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun PlayerControlBar(player: ExoPlayer, onFullscreen: () -> Unit) {
    var playWhenReady by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var positionMillis by remember { mutableLongStateOf(0L) }
    var durationMillis by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var heldPosition by remember { mutableLongStateOf(0L) }

    val listener = remember(player) {
        object : Player.Listener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                playWhenReady = player.playWhenReady
            }

            override fun onPlayWhenReadyChanged(playWhenReadyValue: Boolean, reason: Int) {
                playWhenReady = playWhenReadyValue
            }
        }
    }
    DisposableEffect(player) {
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!seeking) {
                positionMillis = player.currentPosition
                durationMillis = player.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF171226))
            .padding(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2440))
                    .clickable { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("-10ث", color = Color(0xFFE6E1F2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {
                val next = !player.playWhenReady
                player.playWhenReady = next
                playWhenReady = next
            }) {
                Icon(
                    if (playWhenReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playWhenReady) "إيقاف مؤقت" else "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2440))
                    .clickable { player.seekTo(player.currentPosition + 10_000) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("+10ث", color = Color(0xFFE6E1F2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { speedMenuOpen = true }) {
                    Icon(Icons.Default.Speed, contentDescription = "سرعة التشغيل", tint = Color(0xFFE6E1F2))
                    if (playbackSpeed != 1f) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PlayerUi.accent)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                DropdownMenu(
                    expanded = speedMenuOpen,
                    onDismissRequest = { speedMenuOpen = false }
                ) {
                    listOf(0.5f, 0.75f, 1f, 1.5f, 2f).forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (option == 1f) "عادي (1x)" else "$option x",
                                    color = if (option == playbackSpeed) PlayerUi.accent else Color(0xFF2B2440)
                                )
                            },
                            onClick = {
                                playbackSpeed = option
                                player.playbackParameters = PlaybackParameters(option)
                                speedMenuOpen = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onFullscreen) {
                Icon(Icons.Default.Fullscreen, contentDescription = "ملء الشاشة", tint = Color(0xFFE6E1F2))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatTime(if (seeking) heldPosition else positionMillis),
                color = Color(0xFFB4ADD0),
                fontSize = 11.sp
            )
            Slider(
                value = if (seeking) {
                    (heldPosition.toFloat() / durationMillis.coerceAtLeast(1)).coerceIn(0f, 1f)
                } else {
                    (positionMillis.toFloat() / durationMillis.coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                onValueChange = { fraction ->
                    if (durationMillis > 0) {
                        seeking = true
                        heldPosition = (fraction * durationMillis).toLong()
                    }
                },
                onValueChangeFinished = {
                    if (durationMillis > 0) {
                        player.seekTo(heldPosition)
                        positionMillis = heldPosition
                    }
                    seeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = PlayerUi.accent,
                    activeTrackColor = PlayerUi.accent,
                    inactiveTrackColor = Color(0xFF312A47)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            Text(
                formatTime(durationMillis),
                color = Color(0xFFB4ADD0),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QualityChipsRow(streams: List<StreamOption>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp)) {
        Text(
            "الجودة والخوادم",
            color = Color(0xFFB9B2D2),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(streams) { index, stream ->
                val selected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) PlayerUi.accent else Color(0xFF1C1730),
                    modifier = Modifier.clickable { onSelect(index) }
                ) {
                    Text(
                        stream.quality.ifBlank { "تلقائي" },
                        color = if (selected) Color.White else Color(0xFFC9C2DC),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

/** Player-local palette: dark cinema surface, warm gold accents from the Uchiha scheme. */
private object PlayerUi {
    val accent = Color(0xFFD7A43B)

    fun friendly(e: Throwable, fallback: String): String = when {
        e is SlayerApi.ApiException -> e.message ?: fallback
        e is java.io.IOException -> "تعذّر الاتصال بالخادم، تحقق من الإنترنت وأعد المحاولة."
        else -> e.message ?: fallback
    }
}
