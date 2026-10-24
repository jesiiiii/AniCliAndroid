package dev.anicli.android.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.anicli.android.common.CircularIconButton
import dev.anicli.android.common.CircularProgressBar
import dev.anicli.android.common.SkipTextButton
import dev.anicli.android.util.Dimens.MediumPadding1
import dev.anicli.android.util.Dimens.MediumPadding2
import dev.anicli.android.util.Dimens.ProgressBarSize
import dev.anicli.android.util.Dimens.ProgressStrokeWidth
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageUrl: String?
)

// Arabic onboarding pages; artwork comes straight from the live provider covers.
val uchihaPages = listOf(
    OnboardingPage(
        title = "استكشف عالم الأنمي",
        description = "دوّس في قصص مفصلة وبيانات كاملة عن شخصياتك المفضلة، من أحدث الحلقات إلى الأعمال الكلاسيكية.",
        imageUrl = "https://img.anslayer.com/anime/anime/cover-image/loJWw3uibbRMZ77i1725263015.jpg"
    ),
    OnboardingPage(
        title = "ابحث فوراً بالعربية",
        description = "اكتب اسم أي أنمي بالعربية أو الإنجليزية لتظهر لك النتائج مباشرة مع التقييمات والأغلفة.",
        imageUrl = "https://img.anslayer.com/anime/anime/cover-image/nTNDG1w4h2LZMS0q1725275037.jpg"
    ),
    OnboardingPage(
        title = "شاهد حلقة بحلقة",
        description = "اختر الحلقة، حدد الجودة، وشاهد مباشرة داخل التطبيق بمشغّل مدمج يوفر لك تجربة سلسة.",
        imageUrl = "https://img.anslayer.com/anime/anime/cover-image/9Iosnb3mH7uKrpgq1725360598.jpg"
    )
)

object OnboardingPrefs {
    private const val FILE = "uchiha_prefs"
    private const val KEY_DONE = "onboarding_done"

    fun isOnboardingDone(context: Context): Boolean = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }
}

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0) { uchihaPages.size }
    val scope = rememberCoroutineScope()
    val buttonState by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                uchihaPages.lastIndex -> Icons.Filled.Check to ""
                else -> Icons.AutoMirrored.Filled.ArrowForward to "تخطّي"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Uchiha Anime",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState
        ) { index ->
            OnboardingPageItem(page = uchihaPages[index])
        }

        Spacer(modifier = Modifier.fillMaxHeight(0.08f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediumPadding2),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressBar(
                progress = (pagerState.currentPage + 1f) / uchihaPages.size,
                color = MaterialTheme.colorScheme.primary,
                stroke = ProgressStrokeWidth,
                size = ProgressBarSize
            )
            CircularIconButton(
                icon = buttonState.first,
                contentDescription = if (buttonState.second.isEmpty()) "بدء" else "التالي",
                onClick = {
                    if (pagerState.currentPage == uchihaPages.lastIndex) {
                        OnboardingPrefs.markDone(context)
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(page = pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (buttonState.second.isNotEmpty()) {
            SkipTextButton(
                text = buttonState.second,
                onClick = {
                    OnboardingPrefs.markDone(context)
                    onFinished()
                }
            )
        }
    }
}

@Composable
private fun OnboardingPageItem(page: OnboardingPage) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = page.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }
        Spacer(modifier = Modifier.height(MediumPadding1))
        Text(
            text = page.title,
            modifier = Modifier
                .padding(horizontal = MediumPadding2)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = page.description,
            modifier = Modifier.padding(horizontal = MediumPadding2).width(320.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
