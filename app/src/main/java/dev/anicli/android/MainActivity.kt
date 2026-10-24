package dev.anicli.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
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
    val destination = selectedAnimeId?.let { "details:$it" } ?: screen

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                initialOffsetX = { width -> width / 7 }
            ) + fadeIn(animationSpec = spring(stiffness = 500f))) togetherWith
                (slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f),
                    targetOffsetX = { width -> -width / 12 }
                ) + fadeOut(animationSpec = spring(stiffness = 500f)))
        },
        label = "UchihaScreenTransition"
    ) { target ->
        when {
            target.startsWith("details:") -> {
                val animeId = target.removePrefix("details:")
                DetailsScreen(
                    animeId = animeId,
                    onSelectEpisode = { number -> openPlayer(animeId, selectedAnimeTitle, number) },
                    onBack = {
                        selectedAnimeId = null
                        screen = "home"
                    }
                )
            }
            target == "search" -> SearchScreen(onBack = { screen = "home" }) { anime ->
                selectedAnimeId = anime.id
                selectedAnimeTitle = anime.title
            }
            target == "favorites" -> FavoritesScreen(
                onBack = { screen = "home" },
                onHome = { screen = "home" },
                onSearch = { screen = "search" },
                onSettings = { screen = "settings" },
                onOpenAnime = { anime ->
                    selectedAnimeId = anime.id
                    selectedAnimeTitle = anime.title
                }
            )
            target == "settings" -> SettingsScreen(
                onBack = { screen = "home" },
                onHome = { screen = "home" },
                onSearch = { screen = "search" },
                onFavorites = { screen = "favorites" }
            )
            else -> HomeScreen(
                onOpenSearch = { screen = "search" },
                onOpenFavorites = { screen = "favorites" },
                onOpenSettings = { screen = "settings" },
                onHome = { screen = "home" }
            ) { anime ->
                selectedAnimeId = anime.id
                selectedAnimeTitle = anime.title
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onOpenAnime: (Anime) -> Unit
) {
    val context = LocalContext.current
    val favorites = remember { FavoriteAnimeStore.getAll(context).sortedBy { it.title.lowercase() } }
    Column(Modifier.fillMaxSize().statusBarsPadding().background(Ui.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Ui.textPrimary) }
            Text("المفضلة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ui.textPrimary)
            Spacer(Modifier.weight(1f))
            Text("${favorites.size}", color = Ui.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(end = 14.dp))
        }
        if (favorites.isEmpty()) {
            EmptyState(null, "قائمتك فارغة", "أضف الأنميات إلى المفضلة لتظهر هنا مرتبة.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(favorites, key = { "favorite-" + it.id }) { anime ->
                    PosterCard(title = anime.title, posterUrl = anime.posterUrl, rating = anime.rating) { onOpenAnime(anime) }
                }
            }
        }
        HomeBottomBar(onHome, onSearch, {}, onSettings, "favorites")
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onFavorites: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("uchiha_settings", 0) }
    var autoNext by remember { mutableStateOf(prefs.getBoolean("auto_next", true)) }
    Column(Modifier.fillMaxSize().statusBarsPadding().background(Ui.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Ui.textPrimary) }
            Text("الإعدادات", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ui.textPrimary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("تشغيل الحلقة التالية تلقائياً", color = Ui.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("ينتقل للحلقة التالية عند انتهاء الحالية.", color = Ui.textSecondary, fontSize = 12.sp)
                }
                Switch(checked = autoNext, onCheckedChange = {
                    autoNext = it
                    prefs.edit().putBoolean("auto_next", it).apply()
                })
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(16.dp)) {
                Column {
                    Text("استئناف المشاهدة", color = Ui.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("يحفظ التطبيق موضع الحلقة محلياً ويكمل منه عند فتحها مجدداً.", color = Ui.textSecondary, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(16.dp)) {
                Column {
                    Text("Uchiha Anime", color = Ui.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("مشغّل مدمج • واجهة عربية • حفظ محلي للتقدم", color = Ui.textSecondary, fontSize = 12.sp)
                }
            }
        }
        HomeBottomBar(onHome, onSearch, onFavorites, onBack, "settings")
    }
}

@Composable
fun HomeScreen(onOpenSearch: () -> Unit, onOpenFavorites: () -> Unit, onOpenSettings: () -> Unit, onHome: () -> Unit, onOpenAnime: (Anime) -> Unit) {
    val vm: HomeViewModel = viewModel()
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoriteAnimeStore.getAll(context)) }
    LaunchedEffect(Unit) { favorites = FavoriteAnimeStore.getAll(context) }
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    if (vm.trending.isNotEmpty()) {
                        item {
                            FeaturedAnimeCard(anime = vm.trending.first(), onClick = { onOpenAnime(vm.trending.first()) })
                        }
                    }
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
                    if (favorites.isNotEmpty()) {
                        item { HomeSectionTitle("مفضلتك") }
                        item { HomeGridHorizontal(favorites, "f", onOpenAnime) }
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
                }
            }
        }
        HomeBottomBar(onHome = onHome, onSearch = onOpenSearch, onFavorites = onOpenFavorites, onSettings = onOpenSettings, selected = "home")
    }
}

@Composable
fun FeaturedAnimeCard(anime: Anime, onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(218.dp)
            .clip(RoundedCornerShape(26.dp)).background(Color(0xFF201747)).clickable(onClick = onClick)
    ) {
        AsyncImage(model = anime.posterUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.42f })
        Box(Modifier.fillMaxSize().background(Color(0x99201747)))
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp).fillMaxWidth(0.76f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFDED8FF)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("اختيارنا لك", color = Color(0xFF4332B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(anime.title, color = Color.White, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("اكتشف التفاصيل والحلقات المتاحة", color = Color(0xFFE6E1F7), fontSize = 12.sp)
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Ui.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("استكشف الأنمي", color = Ui.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(14.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x55FFFFFF)).padding(horizontal = 9.dp, vertical = 6.dp)) {
            Text("UCHIHA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun HomeBottomBar(
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    selected: String
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp)).background(Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarItem(Icons.Default.Home, "الرئيسية", selected == "home", onHome)
        BottomBarItem(Icons.Default.Search, "بحث", selected == "search", onSearch)
        BottomBarItem(Icons.Default.Favorite, "المفضلة", selected == "favorites", onFavorites)
        BottomBarItem(Icons.Default.Settings, "الإعدادات", selected == "settings", onSettings)
    }
}

@Composable
fun BottomBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f), label = "BottomItemScale")
    Column(
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp)).clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(if (selected) Ui.accentSoft else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Ui.accent else Ui.textSecondary, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Ui.accent else Ui.textSecondary, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
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
                posterUrl = anime.posterUrl,
                onClick = { onOpenAnime(anime) }
            )
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
    val context = LocalContext.current
    var isFavorite by remember(animeId) { mutableStateOf(FavoriteAnimeStore.contains(context, animeId)) }
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
                                TextButton(onClick = {
                                    isFavorite = FavoriteAnimeStore.toggle(context, info.anime)
                                }) {
                                    Text(if (isFavorite) "★ إزالة من المفضلة" else "☆ إضافة للمفضلة", color = Ui.accent)
                                }
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "PosterPressScale"
    )
    Column(
        Modifier
            .width(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
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
