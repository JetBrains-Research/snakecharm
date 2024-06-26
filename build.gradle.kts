@file:Suppress("SpellCheckingInspection")

import org.jetbrains.changelog.exceptions.MissingVersionException
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.IntelliJPluginConstants.INITIALIZE_INTELLIJ_PLUGIN_TASK_NAME
import org.jetbrains.intellij.IntelliJPluginConstants.INSTRUMENT_CODE_TASK_NAME
import org.jetbrains.intellij.IntelliJPluginConstants.INSTRUMENT_TEST_CODE_TASK_NAME
import org.jetbrains.intellij.propertyProviders.IntelliJPlatformArgumentProvider
import org.jetbrains.intellij.propertyProviders.LaunchSystemArgumentProvider
import org.jetbrains.intellij.propertyProviders.PluginPathArgumentProvider
import org.jetbrains.intellij.tasks.InitializeIntelliJPluginTask
import org.jetbrains.intellij.tasks.InstrumentCodeTask
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil.exists
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")

    // Kotlin support latest: 1.9.23
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"

    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    // This plugin allows you to build plugins for IntelliJ platform using specific
    // IntelliJ SDK and bundled plugins.
    id("org.jetbrains.intellij") version "1.17.3"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.2.0"
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
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.23")
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

    register("cucumber") {
        // TODO: this config doesn't work at the moment, with Java > 8 we need to configure jvm modules and it isn't trivial
        //  here. In general required args could be found in [pycharmPC-LATEST-EAP-SNAPSHOT]/product-info.json

        // Cucumber tests are executed using `AllCucumberFeaturesTest` JUnit runner
        // this task is an alternative if `AllCucumberFeaturesTest` stop working for
        // some reason, normally we don't need this
        dependsOn("assemble", "compileTestKotlin")

        //TODO: Attempt to fix: export somehow 'sun.awt'  '--add-exports' could really mean that you have to '--add-opens'
        // See: https://github.com/JetBrains/gradle-intellij-plugin/blob/ecd496160c989b0129eebb7ebb9a5f3608182645/src/main/kotlin/org/jetbrains/intellij/IntelliJPlugin.kt#L1252

        doLast {
            javaexec {
                val ideDirProvider = setupDependencies.flatMap { task -> task.idea.map { it.classes } }
                val sandboxDirProvider = intellij.sandboxDir.map {
                    project.file(it)
                }
                val configDirectoryProvider = sandboxDirProvider.map {
                    it.resolve("config-test").apply { mkdirs() }
                }
                val systemDirectoryProvider = sandboxDirProvider.map {
                    it.resolve("system-test").apply { mkdirs() }
                }
                val pluginsDirectoryProvider = sandboxDirProvider.map {
                    it.resolve("plugins-test").apply { mkdirs() }
                }
                val instrumentedCodeTaskProvider = project.tasks.named<InstrumentCodeTask>(INSTRUMENT_CODE_TASK_NAME)
                val instrumentedTestCodeTaskProvider = project.tasks.named<InstrumentCodeTask>(INSTRUMENT_TEST_CODE_TASK_NAME)
                val instrumentedCodeOutputsProvider = project.provider {
                    project.files(instrumentedCodeTaskProvider.map { it.outputDir.asFile })
                }
                val instrumentedTestCodeOutputsProvider = project.provider {
                    project.files(instrumentedTestCodeTaskProvider.map { it.outputDir.asFile })
                }
                val initializeIntellijPluginTaskProvider = project.tasks.named<InitializeIntelliJPluginTask>(INITIALIZE_INTELLIJ_PLUGIN_TASK_NAME)
                val coroutinesJavaAgentPathProvider = initializeIntellijPluginTaskProvider.flatMap {
                    it.coroutinesJavaAgentPath
                }

                jvmArgumentProviders.add(IntelliJPlatformArgumentProvider(
                        ideDirProvider.get().toPath(),
                        coroutinesJavaAgentPathProvider.get(),
                        this
                ))

                jvmArgumentProviders.add(
                        LaunchSystemArgumentProvider(
                                ideDirProvider.get().toPath(),
                                configDirectoryProvider.get(),
                                systemDirectoryProvider.get(),
                                pluginsDirectoryProvider.get(),
                                listOf("SnakeCharm")
                        )
                )
                jvmArgumentProviders.add(PluginPathArgumentProvider(pluginsDirectoryProvider.get()))

                mainClass.set("io.cucumber.core.cli.Main")
                classpath = project.sourceSets.test.get().runtimeClasspath
                systemProperties(
////                    "idea.config.path" to "${intellij.sandboxDir.get()}/config-test",
////                    "idea.system.path" to "${intellij.sandboxDir.get()}/system-test",
////                    "idea.plugins.path" to "${intellij.sandboxDir.get()}/plugins-test",
                    "idea.force.use.core.classloader" to "true",
                )
                args = listOf(
                    "--plugin", "pretty", "--glue", "features.glue", "--tags", "not @ignore", "src/test/resources"
                )
            }
        }
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
        // original, non-instrumented JAR
        // :runIde, :test tasks launches the plugin from *.jar, so it is required also for development
        dependsOn("buildWrappersBundle")
        from("${project.buildDir}/bundledWrappers/smk-wrapper-storage-bundled.cbor")
    }

    instrumentedJar {
        // modified (instrumented) JAR
        // :runIde, :test tasks launches the plugin from *.jar, so it is required also for development
        dependsOn("buildWrappersBundle")
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