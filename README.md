# Uchiha Anime

تطبيق Android عربي لاكتشاف الأنمي والبحث عنه ومشاهدة الحلقات داخل التطبيق.

## الحالة الحالية

- واجهة عربية مع دعم RTL وثيم فاتح.
- بحث مباشر عبر خدمة الخلفية المنشورة.
- عرض نتائج البحث وأغلفتها وتفاصيلها المتاحة.
- جلب الحلقات باستخدام معرّف الأنمي من المصدر.
- تشغيل الروابط التي يحلّها الخادم عبر AndroidX Media3 ExoPlayer.
- عناصر تحكم للمشغل تشمل التشغيل/الإيقاف وشريط التقدم، والتقديم أو التأخير 10 ثوانٍ، وسرعات من 0.5x إلى 2x.
- بيانات اعتماد المصدر محفوظة على الخادم، وليست ضمن APK.

## البناء

يتطلب البناء JDK 17 وAndroid SDK 35 وGradle 8.11.1.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

ملف APK الناتج: app/build/outputs/apk/debug/app-debug.apk

## تنزيل APK

كل دفع إلى فرع main يشغّل GitHub Actions لبناء APK ونشره كـ Artifact لمدة 30 يومًا. افتح تبويب Actions في المستودع، واختر آخر تشغيل ناجح لسير العمل Build Android APK، ثم نزّل Artifact المسمى Uchiha-Anime-debug-apk.

https://github.com/jesiiiii/AniCliAndroid/actions

## خدمة الخلفية

- عنوان الخدمة: https://anicli-secure-api-lzhrek.v2.appdeploy.ai/
- المسارات المستخدمة: /api/health، /api/search?q=...، /api/episodes?animeId=...، /api/stream?animeId=...&episode=...&type=...&quality=...
- يتطلب تشغيلها إعداد السر ANI_CLI_AR_AUTH_SECRET في مخزن أسرار منصة النشر.

## حدود الاختبار

نجاح بناء APK لا يثبت وحده أن كل مصدر بث خارجي متاح أو أن كل رابط فيديو يعمل على كل شبكة. يجب اختبار رحلة البحث والحلقات والبث على جهاز فعلي عند توفر الشبكة؛ لا يُعلن تشغيل فيديو حقيقي ناجحًا قبل هذا التحقق.

## الترخيص

يحتوي المشروع على تكامل مع مشروع المصدر المفتوح ani-cli-arabic (https://github.com/np4abdou1/ani-cli-arabic)، المرخّص بموجب GPL-3.0. راجع تراخيص المكونات التي تُستخدم أو تُوزّع مع التطبيق.
