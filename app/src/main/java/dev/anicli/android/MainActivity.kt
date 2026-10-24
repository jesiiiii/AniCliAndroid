package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.IOException

object Ui {
    val accent = Color(0xFF5B4FE9)
    val accentSoft = Color(0xFFEFEBFF)
    val background = Color(0xFFF7F7FC)
    val card = Color.White
    val textPrimary = Color(0xFF171226)
    val textSecondary = Color(0xFF6E6880)
    val placeholder = Color(0xFFE9E7F5)
    val badge = Color(0xFFFF5A5F)
    val star = Color(0xFFFFB020)
    val border = Color(0xFFE3E1EF)

    val light = lightColorScheme(
        primary = accent,
        background = background,
        surface = card,
        onPrimary = Color.White,
        onBackground = textPrimary,
        onSurface = textPrimary
    )

    fun friendly(e: Throwable, fallback: String): String = when {
        e is SlayerApi.ApiException -> e.message ?: fallback
        e is IOException -> "تعذّر الاتصال بالخادم، تحقق من الإنترنت وأعد المحاولة."
        e.message?.contains("JSON") == true -> "استجابة غير متوقعة من الخادم، جرّب مرة أخرى."
        else -> e.message ?: fallback
    }
}

sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
}

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
                latest = SlayerApi.latestEpisodes(24)
            } catch (e: Exception) {
                error = Ui.friendly(e, "تعذّر تحميل الصفحة الرئيسية.")
                loading = false
                return@launch
            }
            try {
                trending = SlayerApi.trendingAnime(20)
                topRated = SlayerApi.topRatedAnime(20)
            } catch (e: Exception) {
                error = Ui.friendly(e, "تعذّر تحميل الأنميات الشائعة.")
                loading = false
                return@launch
            }
            loading = false
        }
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
                        error = Ui.friendly(e, "تعذّر إكمال البحث.")
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
            MaterialTheme(colorScheme = Ui.light) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    UchihaApp(openPlayer = { animeId, animeTitle, episodeNumber ->
                        startActivity(PlayerActivity.intent(this@MainActivity, animeId, animeTitle, episodeNumber))
                    })
                }
            }
        }
    }
}

@Composable
fun UchihaApp(openPlayer: (String, String, String) -> Unit) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedAnimeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAnimeTitle by rememberSaveable { mutableStateOf("") }

    if (selectedAnimeId != null) {
        DetailsScreen(
            animeId = selectedAnimeId!!,
            onSelectEpisode = { number -> openPlayer(selectedAnimeId!!, selectedAnimeTitle, number) },
            onBack = {
                selectedAnimeId = null
                if (screen == "details") screen = "home"
            }
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
        onOpenSearch = { screen = "search" }
    ) { anime ->
        selectedAnimeId = anime.id
        selectedAnimeTitle = anime.title
    }
}

@Composable
fun HomeScreen(onOpenSearch: () -> Unit, onOpenAnime: (Anime) -> Unit) {
    val vm: HomeViewModel = viewModel()
    LaunchedEffect(Unit) {
        if (vm.latest.isEmpty() && vm.trending.isEmpty() && vm.topRated.isEmpty()) vm.load()
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Ui.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ui.accent),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Uchiha Anime", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ui.textPrimary)
                Text("أنمي بالعربية، حلقة بعد حلقة", fontSize = 12.sp, color = Ui.textSecondary)
            }
        }
        when {
            vm.loading && vm.latest.isEmpty() -> {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Ui.placeholder)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ui.placeholder)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ui.placeholder)
                    )
                }
            }
            vm.error != null && vm.latest.isEmpty() && vm.trending.isEmpty() && vm.topRated.isEmpty() -> {
                ErrorState(message = vm.error.orEmpty()) { vm.load() }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(
                            Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .clickable { onOpenSearch() }
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Ui.card),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = Ui.textSecondary,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                            Text(
                                "ابحث عن أنمي...",
                                modifier = Modifier.padding(horizontal = 10.dp),
                                color = Ui.textSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                    if (vm.latest.isNotEmpty()) {
                        item { HomeSectionTitle("أحدث الحلقات") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(vm.latest, key = { it.animeId + "-" + it.episodeNumber }) { ep ->
                                    PosterCard(
                                        title = ep.animeTitle,
                                        posterUrl = ep.posterUrl,
                                        badge = "حلقة ${ep.episodeNumber}",
                                        onClick = { onOpenAnime(Anime(id = ep.animeId, title = ep.animeTitle)) }
                                    )
                                }
                            }
                        }
                    }
                    if (vm.trending.isNotEmpty()) {
                        item {
                            Text(
                                "الأكثر مشاهدة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ui.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            HomeGridHorizontal(vm.trending, "t", onOpenAnime)
                        }
                    }
                    if (vm.topRated.isNotEmpty()) {
                        item {
                            Text(
                                "الأعلى تقييماً",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ui.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            HomeGridHorizontal(vm.topRated, "r", onOpenAnime)
                        }
                    }
                    item { Spacer(Modifier.height(30.dp)) }
                }
            }
        }
    }
}

@Composable
fun HomeSectionTitle(title: String) {
    Text(
        title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Ui.textPrimary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun HomeGridHorizontal(list: List<Anime>, keyPrefix: String, onOpenAnime: (Anime) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list, key = { keyPrefix + it.id }) { anime ->
            PosterCard(
                title = anime.title,
                posterUrl = anime.posterUrl
            ) { }
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onOpenAnime: (Anime) -> Unit) {
    val vm: SearchViewModel = viewModel()
    var query by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Ui.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Ui.textPrimary)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("اكتب اسم الأنمي (عربي أو إنجليزي)", color = Ui.textSecondary, fontSize = 14.sp)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Ui.card,
                    unfocusedContainerColor = Ui.card,
                    focusedBorderColor = Ui.accent,
                    unfocusedBorderColor = Ui.border
                )
            )
        }
        when {
            vm.loading -> LoadingState("جارٍ البحث...")
            vm.error != null -> ErrorState(message = vm.error.orEmpty()) {
                vm.query.value = query + " "
                vm.query.value = query.trim()
            }
            trimmed(query).isBlank() -> EmptyState(
                iconRes = { },
                title = "ابحث أولاً",
                subtitle = "اكتب اسم أي أنمي لتظهر لك نتائجه هنا بالعربية."
            )
            vm.searched && vm.results.isEmpty() -> EmptyState(
                iconRes = null,
                title = "لا توجد نتائج",
                subtitle = "تأكد من صيغة الاسم، أو جرّب الاسم الإنجليزي للأنمي."
            )
            vm.results.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(vm.results, key = { it.id + "s" }) { anime ->
                    PosterCard(title = anime.title, posterUrl = anime.posterUrl) {
                        onOpenAnime(anime)
                    }
                }
            }
        }
    }
}

private fun trimmed(value: String): String = value.trim()

@Composable
fun DetailsScreen(animeId: String, onSelectEpisode: (String) -> Unit, onBack: () -> Unit) {
    var data by remember { mutableStateOf<AnimeWithEpisodes?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(animeId) {
        try {
            data = SlayerApi.getAnimeWithEpisodes(animeId)
            loading = false
        } catch (e: Exception) {
            error = Ui.friendly(e, "تعذّر تحميل التفاصيل.")
            loading = false
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Ui.background)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Ui.textPrimary)
            }
            Text("تفاصيل الأنمي", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ui.textPrimary)
        }
        when {
            loading -> LoadingState("جارٍ تحميل التفاصيل والحلقات...")
            error != null -> ErrorState(
                message = error.orEmpty(),
                onRetry = { error = null; loading = true }
            )
            data == null -> EmptyState(null, "لا توجد بيانات", "تعذّر العثور على هذا الأنمي في المصدر.")
            else -> {
                val info = data!!
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = info.anime.posterUrl,
                                contentDescription = "غلاف الأنمي",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(0.68f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Ui.placeholder)
                            )
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(
                                    info.anime.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ui.textPrimary
                                )
                                info.anime.titleEn.takeIf { it.isNotBlank() && it != info.anime.title }?.let {
                                    Text(it, fontSize = 13.sp, color = Ui.textSecondary)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (info.anime.rating.isNotBlank()) {
                                        Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = Ui.star)
                                        Text(
                                            " ${info.anime.rating}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Ui.textPrimary
                                        )
                                    }
                                    InfoChip(info.anime.animeType)
                                }
                                if (info.anime.genres.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        info.anime.genres,
                                        fontSize = 12.sp,
                                        color = Ui.textSecondary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (info.anime.description?.isNotBlank() == true) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    info.anime.description.orEmpty(),
                                    fontSize = 14.sp,
                                    color = Ui.textSecondary,
                                    maxLines = if (expanded) 60 else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(onClick = { expanded = !expanded }) {
                                    Text(if (expanded) "إخفاء" else "المزيد", color = Ui.accent)
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "الحلقات (${info.episodes.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ui.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (info.episodes.isEmpty()) {
                        item {
                            EmptyState(
                                null,
                                "لا توجد حلقات",
                                "قد لا تكون الحلقات متاحة بعد في المصدر المختار."
                            )
                        }
                    } else {
                        items(info.episodes, key = { ep -> ep.number + "-" + info.anime.id }) { ep ->
                            EpisodeCard(episode = ep, onPlay = { onSelectEpisode(ep.number) })
                        }
                    }
                    item { Spacer(Modifier.height(30.dp)) }
                }
            }
        }
    }
}

@Composable
fun EpisodeCard(episode: Episode, onPlay: (Episode) -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ui.card)
            .clickable { onPlay(episode) }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ui.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(episode.number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ui.accent)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "الحلقة ${episode.number}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ui.textPrimary
                )
                episode.title.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = Ui.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Ui.accent)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("شاهدها", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    if (text.isBlank()) return
    Box(
        Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Ui.accentSoft)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, color = Ui.accent, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LoadingState(message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = Ui.accent)
        Text(message, color = Ui.textSecondary, fontSize = 14.sp)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("تعذّر إكمال الطلب", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ui.textPrimary)
        Text(message, color = Ui.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Ui.accent)
        ) {
            Text("إعادة المحاولة", color = Color.White)
        }
    }
}

@Composable
fun EmptyState(iconRes: (@Composable () -> Unit)?, title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        iconRes?.invoke()
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ui.textPrimary)
        Text(
            subtitle,
            fontSize = 13.sp,
            color = Ui.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PosterCard(title: String, posterUrl: String?, badge: String = "", onClick: () -> Unit) {
    Column(
        Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = posterUrl,
                contentDescription = "غلاف $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ui.placeholder)
            )
            if (badge.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ui.badge)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ui.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
