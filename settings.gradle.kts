pluginManagement {
    repositories {
        // Mirror mirrors first: maven.google.com artifacts resolve from dl.google.com endpoints
        // that are unreachable on some mobile networks (404 placeholders). Official repos stay
        // configured as fallbacks so standard networks are unaffected.
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "AniCliAndroid"
include(":app")
