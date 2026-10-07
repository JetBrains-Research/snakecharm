pluginManagement {
    repositories {
        // On CI, route through JetBrains' cache-redirector to avoid Maven Central / Gradle Plugin
        // Portal 429 rate limits -- the `plugins { ... }` block below resolves through these
        // repositories, and that resolution is what failed with a 429 on repo.maven.apache.org.
        // Skipped locally so IDE Gradle sync isn't slowed by the extra hop.
        if (System.getenv("TEAMCITY_VERSION") != null) {
            maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
            maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        }
        gradlePluginPortal()
        mavenCentral()
        //TODO: Uncomment to allow intellij gradle plugin unpublished versions:
        //maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
rootProject.name = "snakecharm"