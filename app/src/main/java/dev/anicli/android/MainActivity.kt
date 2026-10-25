package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.LocalIndication
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
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UchihaApp() {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedAnimeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAnimeTitle by rememberSaveable { mutableStateOf("") }
    var selectedAnimeSharedKey by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = selectedAnimeId != null || screen == "search") {
        if (selectedAnimeId != null) selectedAnimeId = null else screen = "home"
    }
    val destination = when {
        selectedAnimeId != null -> "details:${selectedAnimeId}"
        screen == "search" -> "search"
        else -> "home"
    }

    SharedTransitionLayout {
        val sharedScope = this
        AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (fadeIn(tween(190)) + slideInHorizontally(initialOffsetX = { it / 18 })) togetherWith
                (fadeOut(tween(130)) + slideOutHorizontally(targetOffsetX = { -it / 28 }))
        },
        label = "uchiha-screen-transition"
    ) { target ->
        when {
            target.startsWith("details:") -> {
                val animeId = target.removePrefix("details:")
                DetailsScreen(
                    animeId = animeId,
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = this,
                    sharedElementKey = selectedAnimeSharedKey.ifBlank { "anime-poster:details:$animeId" },
                    onSelectEpisode = { number ->
                        context.startActivity(
                            PlayerActivity.intent(context, animeId, selectedAnimeTitle, number)
                        )
                    },
                    onBack = { selectedAnimeId = null }
                )
            }
            target == "search" -> SearchScreen(
                onBack = { screen = "home" },
                sharedTransitionScope = sharedScope,
                animatedVisibilityScope = this
            ) { anime, sharedKey ->
                selectedAnimeId = anime.id
                selectedAnimeTitle = anime.title
                selectedAnimeSharedKey = sharedKey
            }
            else -> HomeScreen(
                sharedTransitionScope = sharedScope,
                animatedVisibilityScope = this,
                onOpenSearch = { screen = "search" },
                onOpenAnime = { anime, sharedKey ->
                    selectedAnimeId = anime.id
                    selectedAnimeTitle = anime.title
                    selectedAnimeSharedKey = sharedKey
                }
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpenSearch: () -> Unit,
    onOpenAnime: (Anime, String) -> Unit
) {
    val vm: HomeViewModel = viewModel()
    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollBlur by animateDpAsState(if (homeListState.isScrollInProgress) 1.6.dp else 0.dp, label = "poster-scroll-blur")
    LaunchedEffect(Unit) {
        if (vm.latest.isEmpty() && vm.trending.isEmpty() && vm.topRated.isEmpty()) vm.load()
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { CapsuleNavigationBar(selected = "home", onHome = {}, onSearch = onOpenSearch) }) { innerPadding ->
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
                state = homeListState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                Text("Uchiha", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                                Text("Anime", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            MangekyoSharingan(Modifier.size(58.dp))
                        }
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).clickable { onOpenSearch() }.padding(horizontal = 18.dp, vertical = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("ابحث عن عالمك القادم...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Text("بحث", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (vm.trending.isNotEmpty()) {
                    item {
                        val dayIndex = java.time.LocalDate.now().toEpochDay().mod(vm.trending.size.toLong()).toInt()
                        val featured = vm.trending[dayIndex]
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(258.dp).clip(RoundedCornerShape(28.dp)).clickable { onOpenAnime(featured, "anime-poster:featured:${featured.id}") }) {
                            SlayerPosterImage(posterUrl = featured.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().sharedPosterElement(sharedTransitionScope, animatedVisibilityScope, "anime-poster:featured:${featured.id}"))
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
                                        PosterTile(title = ep.animeTitle, titleEnglish = ep.animeTitle, badge = "الحلقة ${ep.episodeNumber}", rating = ep.rating ?: "—", modifier = Modifier.weight(1f), blurRadius = scrollBlur, posterUrl = ep.posterUrl, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, sharedElementKey = "anime-poster:latest:${ep.animeId}") {
                                            onOpenAnime(Anime(id = ep.animeId, title = ep.animeTitle, posterUrl = ep.posterUrl, rating = ep.rating), "anime-poster:latest:${ep.animeId}")
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
                                    pair.forEach { anime -> PosterTile(anime.title, anime.titleEn, anime.animeType, anime.rating ?: "—", Modifier.weight(1f), blurRadius = scrollBlur, posterUrl = anime.posterUrl, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, sharedElementKey = "anime-poster:trending:${anime.id}") { onOpenAnime(anime, "anime-poster:trending:${anime.id}") } }
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
                                    pair.forEach { anime -> PosterTile(anime.title, anime.titleEn, anime.animeType, anime.rating, Modifier.weight(1f), blurRadius = scrollBlur, posterUrl = anime.posterUrl, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, sharedElementKey = "anime-poster:top-rated:${anime.id}") { onOpenAnime(anime, "anime-poster:top-rated:${anime.id}") } }
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

/** Poster images come directly from the Slayer API response. */
@Composable
private fun SlayerPosterImage(posterUrl: String?, contentDescription: String?, contentScale: ContentScale, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (!posterUrl.isNullOrBlank()) {
            AsyncImage(model = posterUrl, contentDescription = contentDescription, contentScale = contentScale, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.Star, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun CapsuleNavigationBar(selected: String, onHome: () -> Unit, onSearch: () -> Unit) {
    val capsule = RoundedCornerShape(50)
    val homePillColor by animateColorAsState(
        targetValue = if (selected == "home") MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "home-pill-selection"
    )
    val searchPillColor by animateColorAsState(
        targetValue = if (selected == "search") MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search-pill-selection"
    )
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 10.dp).clip(capsule).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val homeColor by animateColorAsState(
            targetValue = if (selected == "home") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(150),
            label = "home-icon-tint"
        )
        val searchColor by animateColorAsState(
            targetValue = if (selected == "search") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(150),
            label = "search-icon-tint"
        )
        Row(Modifier.weight(1f).clip(capsule).background(homePillColor).springClickable(onClick = onHome).padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Home, contentDescription = "الرئيسية", tint = homeColor, modifier = Modifier.size(21.dp))
            AnimatedVisibility(visible = selected == "home", enter = fadeIn(tween(130)) + expandHorizontally(), exit = fadeOut(tween(90)) + shrinkHorizontally()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(7.dp))
                    Text("الرئيسية", color = homeColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(Modifier.weight(1f).clip(capsule).background(searchPillColor).springClickable(onClick = onSearch).padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = "بحث", tint = searchColor, modifier = Modifier.size(21.dp))
            AnimatedVisibility(visible = selected == "search", enter = fadeIn(tween(130)) + expandHorizontally(), exit = fadeOut(tween(90)) + shrinkHorizontally()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(7.dp))
                    Text("بحث", color = searchColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PosterTile(title: String, titleEnglish: String = title, badge: String?, rating: String, modifier: Modifier = Modifier, blurRadius: androidx.compose.ui.unit.Dp = 0.dp, posterUrl: String? = null, sharedTransitionScope: SharedTransitionScope? = null, animatedVisibilityScope: AnimatedVisibilityScope? = null, sharedElementKey: String? = null, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().springClickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            SlayerPosterImage(posterUrl = posterUrl, contentDescription = "غلاف $title", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(blurRadius).sharedPosterElement(sharedTransitionScope, animatedVisibilityScope, sharedElementKey))
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
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            angle.animateTo(360f, animationSpec = tween(durationMillis = 900))
            angle.snapTo(0f)
            delay(2000)
        }
    }
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.47f
        drawCircle(Color(0xFFB80F24), radius = radius, center = Offset(cx, cy))
        rotate(angle.value, pivot = Offset(cx, cy)) {
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AnimeCard(
    anime: Anime,
    badgeOverride: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementKey: String? = null
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
                SlayerPosterImage(
                    posterUrl = anime.posterUrl,
                    contentDescription = "غلاف ${anime.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().sharedPosterElement(
                        sharedTransitionScope, animatedVisibilityScope, sharedElementKey
                    )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpenAnime: (Anime, String) -> Unit
) {
    val vm: SearchViewModel = viewModel()
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        bottomBar = { CapsuleNavigationBar(selected = "search", onHome = onBack, onSearch = {}) }
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
                        AnimeCard(
                            anime = anime,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedElementKey = "anime-poster:search:${anime.id}",
                            onClick = { onOpenAnime(anime, "anime-poster:search:${anime.id}") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailsScreen(
    animeId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedElementKey: String,
    onSelectEpisode: (String) -> Unit,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf<AnimeWithEpisodes?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var retryKey by remember { mutableStateOf(0) }
    LaunchedEffect(animeId, retryKey) {
        loading = true
        error = null
        try {
            data = SlayerApi.getAnimeWithEpisodes(animeId)
        } catch (e: Exception) {
            error = e.message ?: "تعذّر تحميل التفاصيل."
        } finally {
            loading = false
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                error != null -> ErrorState(
                    title = "تعذّر تحميل التفاصيل",
                    message = error.orEmpty(),
                    onRetry = { retryKey += 1; loading = true; error = null }
                )
                else -> {
                    val info = data ?: return@Box
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            Box(Modifier.fillMaxWidth().height(410.dp).clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)).background(Color(0xFFF1EEEE))) {
                                SlayerPosterImage(posterUrl = info.anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(34.dp))
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)))
                                IconButton(onClick = onBack, modifier = Modifier.padding(14.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.42f), CircleShape)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                                }
                                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 26.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(Modifier.width(112.dp).height(158.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        SlayerPosterImage(
                                            posterUrl = info.anime.posterUrl,
                                            contentDescription = "بوستر ${info.anime.title}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().sharedPosterElement(
                                                sharedTransitionScope, animatedVisibilityScope, sharedElementKey
                                            )
                                        )
                                    }
                                    Column(Modifier.weight(1f).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(info.anime.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Start, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        if (info.anime.titleEn.isNotBlank() && info.anime.titleEn != info.anime.title) {
                                            Text(info.anime.titleEn, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Icon(Icons.Rounded.Star, contentDescription = "التقييم", tint = Color(0xFFFFD166), modifier = Modifier.size(18.dp))
                                            Text(info.anime.rating.ifBlank { "—" }, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Button(onClick = { info.episodes.firstOrNull()?.let { onSelectEpisode(it.number) } }, enabled = info.episodes.isNotEmpty(), shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, disabledContainerColor = Color.DarkGray)) {
                                            Text("▶  ابدأ المشاهدة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val genres = info.anime.genres.split(",", "،", "/", "|")
                                        .map { it.trim() }.filter { it.isNotBlank() }.distinct().take(6)
                                    val tags = if (genres.isNotEmpty()) genres else listOf(info.anime.animeType.ifBlank { "أنمي" })
                                    tags.forEach { genre ->
                                        Text(
                                            genre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFE7E9),
                                            maxLines = 1,
                                            modifier = Modifier.clip(RoundedCornerShape(50))
                                                .background(Color(0xFF720D1A))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(info.anime.rating.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(info.anime.animeType.ifBlank { "أنمي" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("القصة", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    info.anime.description.orEmpty().ifBlank { "لا يوجد وصف متاح لهذا الأنمي." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (expanded) 60 else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if ((info.anime.description?.length ?: 0) > 180) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Text(if (expanded) "عرض أقل" else "قراءة المزيد", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("الحلقات", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                                    Text("${info.episodes.size} حلقة متاحة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (info.episodes.isEmpty()) {
                            item { CenterState("لا توجد حلقات", "قد لا تكون الحلقات متاحة بعد في المصدر المختار.") }
                        } else {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(info.episodes, key = { ep -> ep.number + "-" + info.anime.id }) { ep ->
                                        Card(
                                            onClick = { onSelectEpisode(ep.number) },
                                            modifier = Modifier.widthIn(min = 142.dp, max = 170.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                            ),
                                            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(
                                                Modifier.padding(14.dp).height(88.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Box(
                                                    Modifier.clip(RoundedCornerShape(10.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("الحلقة ${ep.number}", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    ep.title.ifBlank { "شاهد الحلقة الآن" },
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text("▶ تشغيل", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
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

/**
 * Lightweight press feedback built on Compose interactions. It keeps click handling native,
 * scales only while pressed, and emits one subtle haptic on activation rather than per frame.
 */
@Composable
private fun Modifier.springClickable(
    onClick: () -> Unit,
    hapticOnClick: Boolean = true
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press-scale"
    )
    val haptics = LocalHapticFeedback.current
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current
        ) {
            if (hapticOnClick) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedPosterElement(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    key: String?
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null || key.isNullOrBlank()) {
        return this
    }
    return with(sharedTransitionScope) {
        this@sharedPosterElement.sharedElement(
            rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}
