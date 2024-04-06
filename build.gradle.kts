@file:Suppress("SpellCheckingInspection")

import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.IntelliJPluginConstants.INSTRUMENT_CODE_TASK_NAME
import org.jetbrains.intellij.IntelliJPluginConstants.INSTRUMENT_TEST_CODE_TASK_NAME
import org.jetbrains.intellij.propertyProviders.IntelliJPlatformArgumentProvider
import org.jetbrains.intellij.propertyProviders.LaunchSystemArgumentProvider
import org.jetbrains.intellij.propertyProviders.PluginPathArgumentProvider
import org.jetbrains.intellij.tasks.InstrumentCodeTask
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil.exists
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")

    // Kotlin support
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"

    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    // This plugin allows you to build plugins for IntelliJ platform using specific
    // IntelliJ SDK and bundled plugins.
    id("org.jetbrains.intellij") version "1.17.3"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.0.0"
}

group = properties("pluginGroup")
version = if (properties("pluginPreReleaseSuffix").isEmpty()) {
    "${properties("pluginVersion")}${properties("pluginPreReleaseSuffix")}"
} else {
    "${properties("pluginVersion")}${properties("pluginPreReleaseSuffix")}.${properties("pluginBuildCounter")}"
}

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("io.cucumber:cucumber-java:7.8.1")
    testImplementation("io.cucumber:cucumber-junit:7.8.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.10")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.8.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.4.1")
}

// Test plugin dynamic unload:
// See https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html#diagnosing-leaks
// * Set to true registry property: ide.plugins.snapshot.on.unload.fail
// * uncomment
// tasks {
//    runIde {
//        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions")
//    }
//}


// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    val platformType = properties("platformType")

    pluginName.set(properties("pluginName"))

    val platformLocalPath = project.findProperty("platformLocalPath") as? String
    if (platformLocalPath != null) {
        if (!exists(platformLocalPath)) {
            logger.error("Custom platfrom path not exist: $platformLocalPath")
        } else {
            logger.warn("Using custom platfrom path: $platformLocalPath")
        }
        //See https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#intellij-extension-localpath
        localPath.set(platformLocalPath)
        downloadSources.set(false)
    } else {
        version.set(properties("platformVersion"))
        logger.warn("Use version: ${version.get()}")
        downloadSources.set(properties("platformDownloadSources").toBoolean())
    }

    type.set(platformType)
    updateSinceUntilBuild.set(true)

    val isPyCharm = platformType == "PC" || platformType == "PY" || platformType == "PD"
    sandboxDir.set("${project.rootDir}/.sandbox${if (isPyCharm) "_pycharm" else ""}")
    ideaDependencyCachePath.set("${project.rootDir}/.idea_distrib_cache")  // Useful for Windows due to short cmdline path

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    val platformPlugins = ArrayList<String>()
    when (platformType) {
        "PC" -> platformPlugins.add("python-ce")
        "PY", "PD" -> {
            platformPlugins.add("python")
            // Workaround: https://youtrack.jetbrains.com/issue/PY-51535/PluginException-when-using-Python-Plugin-213-x-version#focus=Comments-27-5439344.0-0
            platformPlugins.add("com.intellij.platform.images")
        }

        else -> platformPlugins.add(properties("pythonPlugin"))
    }
    platformPlugins.addAll(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    plugins.set(platformPlugins)
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
// Configuration: https://github.com/JetBrains/gradle-changelog-plugin#configuration
changelog {
    // Helps to organize content in CHANGLOG.md. Could generate change notes from it.

    version.set(project.version.toString())
    headerParserRegex.set("""(\d+\.\d+.(\d+|SNAPSHOT)(-\w+)?)""".toRegex())

    // Optionally generate changed commits list url.
    repositoryUrl.set("https://github.com/JetBrains-Research/snakecharm")  // url to compare commits beetween previous and current release

    // default values:
    // combinePreReleases.set(true) // default; Combines pre-releases (like 1.0.0-alpha, 1.0.0-beta.2) into the final release note when patching.
    // header.set(provider { "[${version.get()}] - ${date()}" })
    // groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    // itemPrefix.set("-") // default
    // path.set(file("CHANGELOG.md").canonicalPath)  // default value
    // keepUnreleasedSection.set(true) // default
    // unreleasedTerm.set("[Unreleased]") // default
}


// Set the JVM language level used to compile sources and generate files
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(properties("javaVersion")))
    }
}

tasks {
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = it
                languageVersion = "1.7"
                // see https://plugins.jetbrains.com/docs/intellij/kotlin.html#kotlin-standard-library
                apiVersion = "1.6"
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(projectDir.resolve("README.md").readText().lines().run {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            if (!containsAll(listOf(start, end))) {
                throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
            }
            subList(indexOf(start) + 1, indexOf(end))
        }.joinToString("\n").run { markdownToHTML(this) })

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            provider {
                with(changelog) {
                    val log = try {
                        getUnreleased()
                    } catch (e: MissingVersionException) {
                        getOrNull(version.toString()) ?: getLatest()
                    }
                    renderItem(
                        log,
                        org.jetbrains.changelog.Changelog.OutputType.HTML,
                    )
                }
            },
        )
    }

    runPluginVerifier {
        ideVersions.set(
            properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty)
        )
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(properties("intellijPublishToken"))
        // plugin version is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf("$version".split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    register("buildWrappersBundle") {
        dependsOn("compileKotlin", "compileJava")
        doLast {
            javaexec {
                mainClass.set("com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler")
                classpath =
                    project.sourceSets.main.get().runtimeClasspath + files(setupDependencies.get().idea.get().jarFiles)
                enableAssertions = true
                args = listOf(
                    properties("snakemakeWrappersRepoPath"),
                    properties("snakemakeWrappersRepoVersion"),
                    "${project.buildDir}/bundledWrappers/smk-wrapper-storage-bundled.cbor"
                )
                maxHeapSize = "1024m" // Not much RAM is available on TC agents
            }
        }
    }

    jar {
        dependsOn("buildWrappersBundle")
        // :runIde, :test tasks launches the plugin from *.jar, so it is required also for development
        from("${project.buildDir}/bundledWrappers/smk-wrapper-storage-bundled.cbor")
    }

    register("buildTestWrappersBundle") {
        // XXX: we could re-use wrappers bundle task for production here and just pass:
        //  `-PsnakemakeWrappersRepoPath=testData/wrappers_storage' gradle arg
        // P.S: Wrappers bundle task for production always executed before tests in order to get JAR file

        // Builds storage based on test data
        dependsOn("compileKotlin", "compileJava")
        doLast {
            javaexec {
                mainClass.set("com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler")
                classpath =
                    project.sourceSets.main.get().runtimeClasspath + files(setupDependencies.get().idea.get().jarFiles)
                enableAssertions = true
                args = listOf(
                    "${project.projectDir}/testData/wrappers_storage",
                    "$test",
                    "${project.buildDir}/bundledWrappers/smk-wrapper-storage.test.cbor"
                )
                maxHeapSize = "1024m" // Not much RAM is available on TC agents
            }
        }
    }

    test {
        val test by getting(Test::class) {
            isScanForTestClasses = false
            // Only run tests from classes that end with "Test"
            include("**/*Test.class")
//            include("**/SnakeFileTypeTest.class")  // Uncomment to disable gradle tests
//            include("**/AllCucumberFeaturesTest.class")  // Uncomment to disable gradle tests
        }

        dependsOn("buildTestWrappersBundle")
        reports {
            // turn off html reports... windows can't handle certain cucumber test name characters.
            junitXml.required.set(true)
            html.required.set(false)
        }
    }
}