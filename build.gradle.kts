@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants.Configurations
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

fun gradlePropertyOptional(key: String) = project.findProperty(key)?.toString()
fun gradleProperty(key: String) = providers.gradleProperty(key)
fun gradlePropertyWithPriorityToSystemProperty(key: String): String {
    val envVarName = key.uppercase()
    val envVar = System.getenv(envVarName)
    if (envVar != null) {
        logger.warn("Using env variable for '$envVar': $envVar")
        return envVar
    }
    val sysProperty = System.getenv(key)
    if (sysProperty != null) {
        logger.warn("Using system property for '$key': $sysProperty (env variable '$envVarName' not found)")
        return sysProperty
    }
    val gradleProperty = providers.gradleProperty(key).get()
    logger.warn("Using gradle property for '$key': $gradleProperty (env variable '$envVarName' and system property for '$key' not found)")
    return gradleProperty
}

plugins {
    // Java support
    id("java")

    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.serialization) // Kotlin Serialization support

    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin - https://github.com/JetBrains/gradle-intellij-plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin - https://github.com/JetBrains/gradle-changelog-plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = gradleProperty("pluginGroup").get()
version = if (gradleProperty("pluginPreReleaseSuffix").get().isEmpty()) {
    "${gradleProperty("pluginVersion").get()}${gradleProperty("pluginPreReleaseSuffix").get()}"
} else {
    "${gradleProperty("pluginVersion").get()}${gradleProperty("pluginPreReleaseSuffix").get()}.${gradleProperty("pluginBuildCounter").get()}"
}

/// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(gradleProperty("javaVersion").get().toInt())
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(gradleProperty("javaVersion").get())
    }
}

// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Align the *runtime* Kotlin standard library with the one bundled in the target IntelliJ Platform
// (2026.1 / build 261 ships Kotlin 2.3.20). Our build compiles with an older Kotlin, and its
// kotlin-stdlib is otherwise pulled onto the runtime/test classpath (via `kotlinStdlibJdk8`,
// `kotlin-reflect`, `kotlin-test-junit`). That older stdlib's coroutine stack-trace recovery cannot
// read the v2 `@DebugMetadata` emitted by the platform's 2.3.20-compiled classes and throws
// "Debug metadata version mismatch. Expected: 1, got 2", which crashes the coroutine machinery and
// hangs the test IDE during project setup. Forcing the newer stdlib (which understands both metadata
// versions) fixes it. We deliberately scope this to runtime classpath configurations only (matched
// case-insensitively so that both the production `runtimeClasspath` and `testRuntimeClasspath` are
// covered): putting a stdlib newer than the compiler on the compile classpath would trip Kotlin's
// metadata-version check.
configurations.matching { it.name.endsWith("RuntimeClasspath", ignoreCase = true) }.configureEach {
    val kotlinPlatformVersion = libs.versions.kotlinPlatform.get()
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinPlatformVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinPlatformVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinPlatformVersion")
    }
}


dependencies {
    implementation(libs.kotlinStdlibJdk8)
    implementation(libs.kotlinxCbor)

    testImplementation(platform(libs.junit))
    testImplementation(platform(libs.cucumber))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit")
    testImplementation(libs.kotlinTestJunit)
    testImplementation(libs.kotlinReflect)
    testImplementation(libs.opentest4j)

    intellijPlatform {
        val platformType = gradlePropertyWithPriorityToSystemProperty("platformType")

        val platformLocalPath = gradlePropertyOptional("platformLocalPath")
        if (platformLocalPath != null) {
            if (!file(platformLocalPath).exists()) {
                logger.error("Custom platfrom path not exist: $platformLocalPath")
            } else {
                logger.warn("Using custom platfrom path: $platformLocalPath")
            }
            //See https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#intellij-extension-localpath
            local(platformLocalPath)
        } else {
            val platformVersion = gradlePropertyWithPriorityToSystemProperty("platformVersion")
            val isSnapshot = platformVersion.endsWith("-SNAPSHOT")
            logger.warn("Use IntelliJ Platform Version: ${platformType}-${platformVersion}. SNAPSHOT: $isSnapshot")
            create(platformType, platformVersion) {
                useInstaller = !isSnapshot
            }
        }

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        when (platformType) {
            "PC" -> bundledPlugin("PythonCore")
            "PY", "PD" -> {
                bundledPlugin("Pythonid")

                // TODO??? cleanup? check tests runing or not:
                // Workaround: https://youtrack.jetbrains.com/issue/PY-51535/PluginException-when-using-Python-Plugin-213-x-version#focus=Comments-27-5439344.0-0
                bundledPlugin("com.intellij.platform.images")
            }
            // E.g. IDEA + require python plugin
            else -> plugin(gradleProperty("pythonPlugin"))
        }
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(gradleProperty("platformBundledPlugins").get().split(',').map(String::trim).filter(String::isNotEmpty))

        // Spellchecker was extracted from the platform core into a separate module (with its own
        // classloader) in 2025.2+. We directly use its API (spellchecker.bundledDictionaryProvider),
        // so declare it explicitly.
        // https://plugins.jetbrains.com/docs/intellij/api-changes-list-2025.html
        bundledModule("intellij.spellchecker")
        // In the unified 2026.1 platform the `SpellCheckingInspection` tool itself is provided by the
        // Grazie ("Natural Languages") plugin, not core. Needed so spellchecker-integration tests can
        // enable that inspection in the sandbox.
        bundledPlugin("tanvd.grazi")

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
        // See
        // * https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-types.html#TestFrameworkType-Plugin
        // * https://youtrack.jetbrains.com/issue/PY-83223/Python-Plugin-Test-Framework
        //  E.g.: testFramework(TestFrameworkType.Plugin.Ruby)
        //testFramework(TestFrameworkType.JUnit5)
    }
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
    buildSearchableOptions = true
    instrumentCode = true
    projectName = project.name

    val platformType = gradlePropertyWithPriorityToSystemProperty("platformType")
    val isPyCharm = platformType == "PC" || platformType == "PY" || platformType == "PD"
    sandboxContainer = file("${project.rootDir}/.sandbox${if (isPyCharm) "_pycharm" else ""}")

    pluginConfiguration {
        name = gradleProperty("pluginName")

        version = project.version.toString()

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        // here use plugin version to w/o EAP or build suffix, just major to match changenotes!
        changeNotes = gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    // XXX: our previos logic was different: 1) unreleased 2) plugin 3) latest
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = gradleProperty("pluginSinceBuild")
            untilBuild = gradleProperty("pluginUntilBuild")
        }
    }


    publishing {
        token.set(gradleProperty("intellijPublishToken"))

        // plugin version is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf("${project.version}".split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    pluginVerification {
        ides {
            // releases based on since/until builds
            recommended()
            // EAP snapshots
            select {
                types = listOf(IntelliJPlatformType.PyCharmProfessional)
                channels = listOf(ProductRelease.Channel.EAP, ProductRelease.Channel.RELEASE)
                sinceBuild = "242"
                untilBuild = "301.*"
            }
        }
    }
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

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

qodana {}

kotlin {
    // Extension level
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks {

    runIde {
//        // Test plugin dynamic unload:
//        // See https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html#diagnosing-leaks
//        // * Set to true registry property: ide.plugins.snapshot.on.unload.fail
//        // * uncomment
//        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions")
//
        jvmArgumentProviders += CommandLineArgumentProvider {
            // listOf("-Dname=value")
            listOf("-Dfus.internal.test.mode=true", "-Didea.is.internal=true")
        }
    }

    wrapper {
        gradleVersion = gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    register<JavaExec>("buildWrappersBundle") {
        dependsOn("compileKotlin", "compileJava")

        mainClass.set("com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler")

        classpath = files(project.sourceSets.main.map { it.runtimeClasspath }) +
                configurations[Configurations.INTELLIJ_PLATFORM_TEST_CLASSPATH]
        enableAssertions = true

        args(
            gradleProperty("snakemakeWrappersRepoPath").get(),
            gradleProperty("snakemakeWrappersRepoVersion").get(),
            layout.buildDirectory.file("bundledWrappers/smk-wrapper-storage-bundled.cbor").get(),
            layout.projectDirectory.file("snakemake_api.yaml")
        )
        maxHeapSize = "1024m" // Not much RAM is available on TC agents
    }

    register<JavaExec>("buildTestWrappersBundle") {
        // XXX: we could re-use wrappers bundle task for production here and just pass:
        //  `-PsnakemakeWrappersRepoPath=testData/wrappers_storage' gradle arg
        // P.S: Wrappers bundle task for production always executed before tests in order to get JAR file

        // Builds storage based on test data
        dependsOn("compileKotlin", "compileJava")

        mainClass.set("com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler")

        classpath = files(project.sourceSets.main.map { it.runtimeClasspath }) +
                configurations[Configurations.INTELLIJ_PLATFORM_TEST_CLASSPATH]
        enableAssertions = true

        args(
            layout.projectDirectory.file("testData/wrappers_storage"),
            "test",
            layout.buildDirectory.file("bundledWrappers/smk-wrapper-storage.test.cbor").get(),
            layout.projectDirectory.file("snakemake_api.yaml")
        )
        maxHeapSize = "1024m" // Not much RAM is available on TC agents
    }


    prepareSandbox {
        // Pack wrappers bundle into plugin:
        dependsOn("buildWrappersBundle")

        from(layout.buildDirectory.file("bundledWrappers/smk-wrapper-storage-bundled.cbor")) {
            into(pluginName.map { "$it/extra" })
        }
        from(layout.projectDirectory.file("snakemake_api.yaml")) {
            into(pluginName.map { "$it/extra" })
        }
    }

    test {
        isScanForTestClasses = false
        // Only run tests from classes that end with "Test"
        include("**/*Test.class")
//        include("**/SnakeFileTypeTest.class")  // Uncomment to disable gradle tests
//        include("**/AllCucumberFeaturesTest.class")  // Uncomment to disable gradle tests

        dependsOn("buildTestWrappersBundle")

        // The 2026.1 Python plugin ships its code as v2 content modules under
        // plugins/python-ce/lib/modules/. That breaks PythonHelpersLocator's jar-path lookup for the
        // Python helpers root (it expects the jar directly under `lib/`, and throws
        // "IllegalStateException: .../python-ce/lib/modules should be lib directory"), which crashes
        // PyTypeShed's lazy init and therefore every test that infers Python types. The locator
        // consults the `idea.python.helpers.path` system property first, so point it at the bundled
        // helpers directory explicitly.
        jvmArgumentProviders += CommandLineArgumentProvider {
            val pythonHelpersPath = intellijPlatform.platformPath.resolve("plugins/python-ce/helpers")
            listOf("-Didea.python.helpers.path=$pythonHelpersPath")
        }

        reports {
            // turn off html reports... windows can't handle certain cucumber test name characters.
            junitXml.required.set(true)
            html.required.set(false)
        }
    }

    printProductsReleases {
        channels = listOf(ProductRelease.Channel.EAP)
        types = listOf(IntelliJPlatformType.PyCharmCommunity)
        untilBuild = provider { null }

        doLast {
            val latestEap = productsReleases.get().max()
        }
    }
}