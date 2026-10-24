package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.draw.clip
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
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when {
                vm.loading && vm.latest.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = 180.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                vm.error != null && vm.latest.isEmpty() -> {
                    ErrorState(
                        title = "مشكلة في التحميل",
                        message = vm.error.orEmpty(),
                        onRetry = { vm.load() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "U",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(Modifier.size(12.dp))
                                Text(
                                    "Uchiha Anime",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = onOpenSearch) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = "بحث",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSearch() },
                                singleLine = true,
                                placeholder = {
                                    Text("ابحث عن أنمي (عربي أو إنجليزي)...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        if (vm.latest.isNotEmpty()) {
                            item {
                                Text(
                                    "أحدث الحلقات",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            items(vm.latest, key = { "l${it.animeId}-${it.episodeNumber}" }) { ep ->
                                LatestEpisodeCard(ep) {
                                    onOpenAnime(Anime(id = ep.animeId, title = ep.animeTitle))
                                }
                            }
                        }
                        if (vm.trending.isNotEmpty()) {
                            item {
                                Text(
                                    "الأكثر مشاهدة",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            items(vm.trending, key = { "t${it.id}" }) { anime ->
                                AnimeCard(anime = anime, onClick = { onOpenAnime(anime) })
                            }
                        }
                        if (vm.topRated.isNotEmpty()) {
                            item {
                                Text(
                                    "الأعلى تقييماً",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            items(vm.topRated, key = { "r${it.id}" }) { anime ->
                                AnimeCard(anime = anime, onClick = { onOpenAnime(anime) })
                            }
                        }
                        item {
                            Text(
                                "المصدر: Anime Slayer موبايل • كل الروابط حقيقية من المصدر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Reference AnimeCard layout: pill rating, bold title, two-line synopsis. */
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
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceDim,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(CardCornerRadius)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = "غلاف ${anime.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(CardPosterSize)
                    .clip(RoundedCornerShape(CardCornerRadius))
            )
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(PillCornerRadius)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = badgeOverride ?: anime.rating.ifBlank { "— " },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.genres.ifBlank { anime.animeType },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
