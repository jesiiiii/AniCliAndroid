package dev.anicli.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

private data class FigCraftGenre(val label: String, val terms: List<String>)
private val FigCraftGenres = listOf(
    FigCraftGenre("الكل", emptyList()),
    FigCraftGenre("خيال", listOf("fantasy", "fiction", "خيال", "فانتازيا")),
    FigCraftGenre("أكشن", listOf("action", "أكشن", "اكشن")),
    FigCraftGenre("شونين", listOf("shounen", "shonen", "شونين")),
    FigCraftGenre("مغامرة", listOf("adventure", "مغامرة")),
    FigCraftGenre("دراما", listOf("drama", "دراما")),
    FigCraftGenre("كوميديا", listOf("comedy", "كوميديا"))
)

@Composable
internal fun FigCraftCategoriesScreen(
    anime: List<Anime>,
    onOpenAnime: (Anime) -> Unit
) {
    var selectedGenre by rememberSaveable { mutableStateOf("الكل") }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = anime.distinctBy { it.id }.filter { item ->
        val q = query.trim()
        val matchesQuery = q.isBlank() ||
            item.title.contains(q, ignoreCase = true) ||
            item.titleEn.contains(q, ignoreCase = true)
        val genre = FigCraftGenres.firstOrNull { it.label == selectedGenre }
        val genreText = item.genres.lowercase()
        val matchesGenre = selectedGenre == "الكل" ||
            (genre?.terms?.any { term -> genreText.contains(term.lowercase()) } == true) ||
            item.genres.contains(selectedGenre, ignoreCase = true)
        matchesQuery && matchesGenre
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "الأنميات",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "اعثر على حكايتك التالية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Rounded.Search,
                        contentDescription = "اكتشاف الأنمي",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                placeholder = { Text("ابحث عن أنمي...") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = "بحث") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(FigCraftGenres, key = { it.label }) { genre ->
                    val selected = selectedGenre == genre.label
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(50)
                            )
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .figCraftSpringClickable { selectedGenre = genre.label }
                            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                    ) {
                        Text(
                            genre.label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (selectedGenre == "الكل") "التصنيفات" else selectedGenre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${filtered.size} عنوان",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    CenterState(
                        if (anime.isEmpty()) "نحمّل الأنميات" else "لا توجد نتائج",
                        if (anime.isEmpty()) "ستظهر الأعمال هنا بعد اكتمال تحميل البيانات." else "غيّر التصنيف أو جرّب كلمة بحث أخرى."
                    )
                }
            }
        } else {
            items(filtered.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    pair.forEach { item ->
                        FigCraftAnimeGridCard(
                            anime = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenAnime(item) }
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FigCraftAnimeGridCard(
    anime: Anime,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .figCraftSpringClickable(onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(196.dp)
                .clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = "غلاف ${anime.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxWidth().height(84.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color(0xE6000000))))
            )
            if (anime.rating.isNotBlank()) {
                Text(
                    "★ ${anime.rating}",
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color(0xD9000000))
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                anime.animeType.ifBlank { "أنمي" },
                modifier = Modifier.align(Alignment.BottomStart).padding(9.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                anime.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                anime.titleEn.ifBlank { anime.genres.ifBlank { "اكتشف التفاصيل" } },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun FigCraftProfileScreen(onOpenSettings: () -> Unit) {
    var infoDialog by rememberSaveable { mutableStateOf<String?>(null) }
    val dialogMessage = when (infoDialog) {
        "الحساب" -> "الملف الحالي محلي على جهازك، ولا يتطلب إنشاء حساب أو تسجيل دخول."
        "التنزيلات" -> "إدارة التنزيلات دون اتصال غير متاحة في النسخة الحالية، لذلك لن نعرض أرقام تنزيلات وهمية."
        "الإشعارات" -> "لم تُربط خدمة إشعارات الحلقات الجديدة بعد."
        "الدعم" -> "لم يُضف رابط دعم رسمي إلى التطبيق بعد."
        else -> null
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                "ملفي الشخصي",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier.size(78.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.mangekyo_sharingan_itachi),
                        contentDescription = "مانغيكيو شارينغان",
                        modifier = Modifier.size(58.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("إيتاشي أوتشيها", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                    Text("Genin • Rogue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("UCHIHA ANIME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Text("التفضيلات", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
        }
        item {
            ProfileActionRow(
                title = "الحساب",
                subtitle = "ملف المشاهدة المحلي",
                icon = Icons.Rounded.Person,
                onClick = { infoDialog = "الحساب" }
            )
        }
        item {
            ProfileActionRow(
                title = "التفضيلات",
                subtitle = "المظهر والتشغيل وتجربة المشاهدة",
                icon = Icons.Rounded.Settings,
                onClick = onOpenSettings
            )
        }
        item {
            ProfileActionRow(
                title = "جودة الفيديو",
                subtitle = "إدارة خيارات المشاهدة من الإعدادات",
                icon = Icons.Rounded.Star,
                onClick = onOpenSettings
            )
        }
        item {
            ProfileActionRow(
                title = "التنزيلات",
                subtitle = "حالة ميزة المشاهدة دون اتصال",
                icon = Icons.Rounded.FileDownload,
                onClick = { infoDialog = "التنزيلات" }
            )
        }
        item {
            ProfileActionRow(
                title = "الإشعارات",
                subtitle = "تنبيهات الحلقات الجديدة",
                icon = Icons.Rounded.Notifications,
                onClick = { infoDialog = "الإشعارات" }
            )
        }
        item {
            ProfileActionRow(
                title = "الدعم",
                subtitle = "معلومات المساعدة والتواصل",
                icon = Icons.Rounded.SupportAgent,
                onClick = { infoDialog = "الدعم" }
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("حول التطبيق", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                Text("الإصدار 1.1", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text(
                "Uchiha Anime يجمع تفاصيل الأعمال والحلقات المتاحة في واجهة عربية مصممة للمشاهدة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp)
            )
        }
    }
    if (infoDialog != null && dialogMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text(infoDialog.orEmpty(), fontWeight = FontWeight.Bold) },
            text = { Text(dialogMessage, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { infoDialog = null }) {
                    Text("حسنًا")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ProfileActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .figCraftSpringClickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("‹", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Modifier.figCraftSpringClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "figcraft-press-scale"
    )
    val haptics = LocalHapticFeedback.current
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}


@Composable
internal fun FigCraftFavoritesScreen(
    favorites: List<Anime>,
    onOpenAnime: (Anime) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("المفضلة", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                Text("أعمالك المحفوظة على هذا الجهاز", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (favorites.isEmpty()) {
            item {
                CenterState("قائمتك فارغة", "افتح صفحة أي أنمي واضغط على رمز القلب لإضافته إلى المفضلة.")
            }
        } else {
            items(favorites.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    pair.forEach { anime ->
                        FigCraftAnimeGridCard(anime = anime, modifier = Modifier.weight(1f), onClick = { onOpenAnime(anime) })
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
