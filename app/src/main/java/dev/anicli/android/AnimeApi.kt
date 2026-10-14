package dev.anicli.android

/**
 * The Android app must call a backend proxy owned by the project.
 * Do not put the source API's AUTH_SECRET or private upstream token in this APK.
 *
 * Configure the proxy URL only after deploying the backend and verifying its
 * authentication and response schema.
 */
object AnimeApiConfig {
    // Replace with the HTTPS URL of the project's secured proxy.
    const val BASE_URL = "https://YOUR-SECURE-PROXY.example/"
}
