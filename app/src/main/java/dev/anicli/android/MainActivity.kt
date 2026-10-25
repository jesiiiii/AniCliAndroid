package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.anicli.android.onboarding.OnboardingPrefs
import dev.anicli.android.onboarding.OnboardingScreen
import dev.anicli.android.ui.theme.UchihaTheme
import dev.anicli.android.util.Dimens.CardCornerRadius
import dev.anicli.android.util.Dimens.CardPosterSize
import dev.anicli.android.util.Dimens.PillCornerRadius
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.IOException

class HomeViewModel : ViewModel() {
    var latest by mutableStateOf<List<SlayerApi.LatestScreenEpisode>>(emptyList())
        private set
    var trending by mutableStateOf<List<Anime>>(emptyList())
        private set
    var topRated by mutableStateOf<List<Anime>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                latest = SlayerApi.latestEpisodes(12)
            } catch (e: Exception) {
                error = friendly(e, "تعذّر تحميل الصفحة الرئيسية.")
                loading = false
                return@launch
            }
            try {
                trending = SlayerApi.trendingAnime(10)
                topRated = SlayerApi.topRatedAnime(10)
            } catch (e: Exception) {
                error = friendly(e, "تعذّر تحميل الأنميات الشائعة.")
                loading = false
                return@launch
            }
            loading = false
        }
    }

    private fun friendly(e: Throwable, fallback: String): String = when {
        e is SlayerApi.ApiException -> e.message ?: fallback
        e is IOException -> "تعذّر الاتصال بالخادم، تحقق من الإنترنت وأعد المحاولة."
        else -> e.message ?: fallback
    }
}

class SearchViewModel : ViewModel() {
    val query = MutableStateFlow("")
    var results by mutableStateOf<List<Anime>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var searched by mutableStateOf(false)
        private set

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            query.debounce(450).collect { text ->
                val trimmed = text.trim()
                if (trimmed.isBlank()) {
                    results = emptyList(); searched = false; error = null; loading = false
                } else {
                    loading = true; error = null
                    try {
                        results = SlayerApi.search(trimmed)
                        searched = true
                        loading = false
                    } catch (e: Exception) {
                        results = emptyList()
                        error = e.message ?: "تعذّر إكمال البحث."
                        loading = false
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UchihaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val context = LocalContext.current
                    var onboarded by remember {
                        mutableStateOf(OnboardingPrefs.isOnboardingDone(context))
                    }
                    if (!onboarded) {
                        OnboardingScreen(onFinished = { onboarded = true })
                        return@CompositionLocalProvider
                    }
                    UchihaApp()
                }
            }
        }
    }
}

@Composable
fun UchihaApp() {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedAnimeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAnimeTitle by rememberSaveable { mutableStateOf("") }

    if (selectedAnimeId != null) {
        DetailsScreen(
            animeId = selectedAnimeId!!,
            onSelectEpisode = { number ->
                context.startActivity(
                    PlayerActivity.intent(context, selectedAnimeId!!, selectedAnimeTitle, number)
                )
            },
            onBack = { selectedAnimeId = null }
        )
        return
    }
    if (screen == "search") {
        SearchScreen(onBack = { screen = "home" }) { anime ->
            selectedAnimeId = anime.id
            selectedAnimeTitle = anime.title
        }
        return
    }
    HomeScreen(
        onOpenSearch = { screen = "search" },
        onOpenAnime = { anime ->
            selectedAnimeId = anime.id
            selectedAnimeTitle = anime.title
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSearch: () -> Unit, onOpenAnime: (Anime) -> Unit) {
    val vm: HomeViewModel = viewModel()
    LaunchedEffect(Unit) {
        if (vm.latest.isEmpty() && vm.trending.isEmpty() && vm.topRated.isEmpty()) vm.load()
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        when {
            vm.loading && vm.latest.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("نجهّز لك عالم الأنمي", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            vm.error != null && vm.latest.isEmpty() -> Box(Modifier.padding(innerPadding)) {
                ErrorState("تعذّر تحميل الصفحة", vm.error.orEmpty()) { vm.load() }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MangekyoSharingan(Modifier.size(54.dp))
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("UCHIHA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black)
                                Text("Anime", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                            }
                            IconButton(onClick = onOpenSearch, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.primary)) {
                                Icon(Icons.Rounded.Search, contentDescription = "بحث عن أنمي", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.primary).clickable { onOpenSearch() }.padding(horizontal = 18.dp, vertical = 17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                Text("ابحث عن عالمك القادم...", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Text("⌕", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
                if (vm.trending.isNotEmpty()) {
                    item {
                        val featured = vm.trending.first()
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(238.dp).clip(RoundedCornerShape(28.dp)).clickable { onOpenAnime(featured) }) {
                            AsyncImage(model = featured.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xE5101734)))))
                            Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("اختيار المحرّر", color = Color(0xFFFF9C86), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(featured.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("★  ${featured.rating}", color = Color(0xFFFFD6A5), style = MaterialTheme.typography.labelLarge)
                                    Text("اكتشف التفاصيل  ↗", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                if (vm.latest.isNotEmpty()) {
                    item { SectionHeading("وصل حديثاً", "أحدث الحلقات", "جديد") }
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            vm.latest.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { ep ->
                                        PosterTile(title = ep.animeTitle, posterUrl = ep.posterUrl, badge = "الحلقة ${ep.episodeNumber}", rating = ep.rating, modifier = Modifier.weight(1f)) {
                                            onOpenAnime(Anime(id = ep.animeId, title = ep.animeTitle, posterUrl = ep.posterUrl, rating = ep.rating))
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (vm.trending.isNotEmpty()) {
                    item { SectionHeading("على الرادار", "الأكثر مشاهدة", "ترند") }
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            vm.trending.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { anime -> PosterTile(anime.title, anime.posterUrl, anime.animeType, anime.rating, Modifier.weight(1f)) { onOpenAnime(anime) } }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (vm.topRated.isNotEmpty()) {
                    item { SectionHeading("مختارة بعناية", "الأعلى تقييماً", "TOP") }
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            vm.topRated.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { anime -> PosterTile(anime.title, anime.posterUrl, anime.animeType, anime.rating, Modifier.weight(1f)) { onOpenAnime(anime) } }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                            Text("U", color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text("Uchiha Anime", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("قصص تستحق أن تُعاش", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(eyebrow: String, title: String, tag: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
        }
        Text(tag, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PosterTile(title: String, posterUrl: String, badge: String?, rating: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            AsyncImage(model = posterUrl, contentDescription = "غلاف $title", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(0.35.dp))
            Box(Modifier.fillMaxWidth().height(104.dp).align(Alignment.BottomCenter).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
            if (!badge.isNullOrBlank()) {
                Text(badge, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF04444).copy(alpha = 0.94f)).padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (rating.isNotBlank() && rating != "—") {
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.48f)).padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("★", color = Color(0xFFFFD1D1), style = MaterialTheme.typography.labelMedium)
                    Text(rating, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
    }
}

@Composable
private fun MangekyoSharingan(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mangekyo-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(5200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "mangekyo-angle"
    )
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.47f
        drawCircle(Color(0xFFB80F24), radius = radius, center = Offset(cx, cy))
        rotate(angle, pivot = Offset(cx, cy)) {
            drawCircle(Color(0xFF17131A), radius = radius * 0.78f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.055f))
            drawCircle(Color(0xFF17131A), radius = radius * 0.20f, center = Offset(cx, cy))
            repeat(3) { index ->
                rotate(index * 120f, pivot = Offset(cx, cy)) {
                    drawCircle(Color(0xFF17131A), radius = radius * 0.14f, center = Offset(cx, cy - radius * 0.53f))
                    drawCircle(Color(0xFF17131A), radius = radius * 0.105f, center = Offset(cx + radius * 0.18f, cy - radius * 0.37f))
                }
            }
        }
        drawCircle(Color(0xFFFAF8F4), radius = radius * 0.07f, center = Offset(cx, cy))
    }
}

/** Cinematic poster card: editorial typography, clean surfaces, and a confident accent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeCard(
    anime: Anime,
    badgeOverride: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 96.dp, max = 104.dp)
                    .height(136.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = "غلاف ${anime.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeOverride ?: "اختيار مميز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.genres.ifBlank { anime.animeType.ifBlank { "مسلسل أنمي" } },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = anime.rating.ifBlank { "تعرّف على الأنمي" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            Box(
                Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestEpisodeCard(
    episode: SlayerApi.LatestScreenEpisode,
    onClick: () -> Unit
) {
    AnimeCard(
        anime = Anime(
            id = episode.animeId,
            title = episode.animeTitle,
            posterUrl = episode.posterUrl,
            animeType = "الحلقة الجديدة ${episode.episodeNumber}",
            rating = episode.rating.ifBlank { "—" }
        ),
        badgeOverride = "ح ${episode.episodeNumber}",
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onOpenAnime: (Anime) -> Unit) {
    val vm: SearchViewModel = viewModel()
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; vm.query.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    singleLine = true,
                    placeholder = {
                        Text("اكتب اسم الأنمي...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
            when {
                vm.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                vm.error != null -> ErrorState(
                    title = "تعذّر البحث",
                    message = vm.error.orEmpty(),
                    onRetry = {
                        vm.query.value = vm.query.value.trim() + " "
                        vm.query.value = vm.query.value.trim()
                    }
                )
                query.isBlank() -> CenterState("اكتب اسم الأنمي", "ابحث بالعربية أو الإنجليزية لتظهر النتائج هنا.")
                vm.searched && vm.results.isEmpty() -> CenterState(
                    "لا توجد نتائج",
                    "تأكد من صيغة الاسم أو جرّب الاسم الإنجليزي للأنمي."
                )
                vm.results.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                ) {
                    item {
                        Text(
                            "النتائج (${vm.results.size})",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(vm.results, key = { it.id + "s" }) { anime ->
                        AnimeCard(anime = anime, onClick = { onOpenAnime(anime) })
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsScreen(animeId: String, onSelectEpisode: (String) -> Unit, onBack: () -> Unit) {
    var data by remember { mutableStateOf<AnimeWithEpisodes?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val openEpisode: (String) -> Unit = { number ->
        onSelectEpisode(number)
    }
    LaunchedEffect(animeId) {
        try {
            data = SlayerApi.getAnimeWithEpisodes(animeId)
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "تعذّر تحميل التفاصيل."
            loading = false
        }
    }
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> ErrorState(
                    title = "تعذّر تحميل التفاصيل",
                    message = error.orEmpty(),
                    onRetry = { error = null; loading = true }
                )
                else -> {
                    val info = data ?: return@Box
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = info.anime.posterUrl,
                                    contentDescription = "غلاف الأنمي",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = info.anime.title,
                                    style = MaterialTheme.typography.displaySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                info.anime.titleEn.takeIf { it.isNotBlank() && it != info.anime.title }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    info.anime.animeType.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = " • ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = info.anime.rating.ifBlank { "—" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier.padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = "نبذة",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = info.anime.description.orEmpty().ifBlank { "لا يوجد وصف متاح لهذا الأنمي." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (expanded) 60 else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (info.anime.description?.length ?: 0 > 180) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Text(
                                            if (expanded) "إخفاء" else "المزيد",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (info.anime.genres.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = info.anime.genres,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                text = "الحلقات (${info.episodes.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                        if (info.episodes.isEmpty()) {
                            item {
                                CenterState(
                                    "لا توجد حلقات",
                                    "قد لا تكون الحلقات متاحة بعد في المصدر المختار."
                                )
                            }
                        } else {
                            items(info.episodes, key = { ep -> ep.number + "-" + info.anime.id }) { ep ->
                                EpisodeRow(episode = ep, onPlay = { number -> openEpisode(number) })
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(episode: Episode, onPlay: (String) -> Unit) {
    Card(
        onClick = { onPlay(episode.number) },
        modifier = Modifier.padding(horizontal = 20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceDim,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(CardCornerRadius)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    episode.number,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "الحلقة ${episode.number}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (episode.title.isNotBlank()) {
                    Text(
                        episode.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                "▶ تشغيل",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CenterState(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(title: String, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 400.dp)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(90.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("إعادة المحاولة")
        }
    }
}
