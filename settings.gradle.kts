pluginManagement {
    repositories {
        // ALPHA NOTE: the Kuira SDK + contract plugin aren't on Maven Central
        // yet, so mavenLocal() resolves them from a local publish for now.
        // Remove this line once the alpha is on Central.
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // ALPHA NOTE: mavenLocal() first so io.github.kuiralabs:* resolves from
        // a local publish until the alpha is on Maven Central. Remove then.
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "BBoard"
include(":app")
