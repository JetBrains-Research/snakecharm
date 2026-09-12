@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import org.gradle.api.logging.Logging
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants.Configurations
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import kotlin.io.path.isDirectory

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
    val kotlinxSerializationPlatformVersion = libs.versions.kotlinxSerializationPlatform.get()
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinPlatformVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinPlatformVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinPlatformVersion")

        // Same class of problem as the stdlib above, different library. The platform bundles
        // kotlinx-serialization-core 1.9.0 (lib/intellij.libraries.kotlinx.serialization.core.jar) and
        // its classes carry serializers generated against that ABI; our `kotlinxCbor` dependency drags
        // core onto the runtime/test classpath, where -- the Gradle test classpath being flat rather
        // than plugin-classloader-scoped -- ours wins. Platform-generated serializers then call methods
        // that do not exist in the older core and die with
        // "AbstractMethodError at PluginGeneratedSerialDescriptor.kt", which TestLoggerFactory turns
        // into a test failure, across whole swathes of otherwise unrelated scenarios. Forcing core (and
        // cbor, so the pair stays consistent) to the platform's version fixes the direction of the
        // skew: a newer core runs older generated code fine.
        //
        // Our own `kotlinxCbor` already resolves to this version (the catalog points it at the same
        // key), so today these forces are a no-op; they are what keeps a transitive dependency from
        // dragging a different serialization version in and reintroducing the skew.
        // Measured on #577 (2026.2), where the same skew was live: forcing this removed 101 failures.
        // See #587.
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationPlatformVersion")
        force("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$kotlinxSerializationPlatformVersion")
    }
}


// The platform types whose IDE *is* a Python IDE, i.e. the ones that bundle the Python plugin (and
// therefore its `helpers` directory) as part of the distribution. Anything else (IDEA + the external
// Python plugin) is laid out differently. Goes through the plugin's own enum rather than re-listing
// the codes, so a typo in `platformType` fails loudly here instead of silently picking "not PyCharm".
val isPyCharmPlatform = IntelliJPlatformType.fromCode(gradlePropertyWithPriorityToSystemProperty("platformType")) in
        setOf(
            IntelliJPlatformType.PyCharmCommunity,
            IntelliJPlatformType.PyCharmProfessional,
            IntelliJPlatformType.DataSpell,
        )

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
        //
        // NB: on "PY"/"DS" the compile classpath is `Pythonid` (Python Professional), while
        // plugin.xml declares only `<depends>PythonCore</depends>` and the `else ->` branch still
        // targets IDEA + the community Python plugin. So the compiler no longer rejects a
        // Professional-only Python API used from `src/main`: it compiles, and the tests pass (their
        // classpath is flat), but it would throw NoClassDefFoundError for users on IDEA + PythonCore.
        // Since 2026.1 there is no community PyCharm artifact to build against, so keep that
        // restriction in mind by hand -- everything in `src/main` must stay within PythonCore's API.
        when (platformType) {
            "PC" -> bundledPlugin("PythonCore")
            // NB: keep these codes in sync with `isPyCharmPlatform` above -- they are the same
            // question asked twice. "DS" (DataSpell) is a Python IDE built on Professional, so it
            // bundles `Pythonid` like "PY" does; anything reaching `else` is IDEA + the external
            // Python plugin. There is no "PD" code (see IntelliJPlatformType), and one used to be
            // listed here: `fromCode` would have thrown at configuration time long before the
            // branch could ever be taken.
            "PY", "DS" -> {
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

        // 2026.2 moved PythonHelpersLocator into its own content module and made it resolve helpers
        // through the `com.jetbrains.python.pythonHelpersLocator` extension point, whose only
        // implementation (PythonHelpersLocatorDefault) is registered by that module. Without it the
        // EP is absent in the test application and PyTypeShed init dies with "Missing extension
        // point: com.jetbrains.python.pythonHelpersLocator", taking the whole cucumber suite with it.
        bundledModule("intellij.python.community.helpersLocator")

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

    sandboxContainer = file("${project.rootDir}/.sandbox${if (isPyCharmPlatform) "_pycharm" else ""}")

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
            // EAP snapshots, over the same range the manifest claims. Hardcoding a wider range here
            // makes `verifyPlugin` fail against IDEs that could never install the plugin: the verifier
            // honours this list, not pluginSinceBuild/pluginUntilBuild.
            select {
                types = listOf(IntelliJPlatformType.PyCharmProfessional)
                channels = listOf(ProductRelease.Channel.EAP, ProductRelease.Channel.RELEASE)
                sinceBuild = gradleProperty("pluginSinceBuild")
                untilBuild = gradleProperty("pluginUntilBuild")
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

// The production wrappers bundle needs a local snakemake-wrappers checkout (see DEVELOPER.md); CI
// provides one. See #571.
val wrappersRepoPath = gradlePropertyOptional("snakemakeWrappersRepoPath")?.takeIf { it.isNotBlank() }
val wrappersBundleFile = layout.buildDirectory.file("bundledWrappers/smk-wrapper-storage-bundled.cbor")

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

        // When the property is unset, skip with a warning instead of failing buildPlugin/verifyPlugin
        // for contributors who don't have a snakemake-wrappers checkout. See issue #571.
        //
        // Skip only when it is *unset*. If it is set but wrong (a typo, or a renamed CI checkout) the
        // task still runs and SmkWrapperCrawler fails loudly, as before -- silently publishing a plugin
        // with no wrapper metadata is a much worse outcome than a broken build.
        onlyIf {
            if (wrappersRepoPath == null) {
                logger.warn(
                    "buildWrappersBundle: snakemakeWrappersRepoPath is not set; " +
                        "skipping wrappers bundle (the built plugin will omit bundled wrappers, so wrapper " +
                        "name completion will be unavailable). " +
                        "Pass -PsnakemakeWrappersRepoPath=<snakemake-wrappers checkout> to include them. See #571."
                )
                // Drop a bundle left by an earlier run that did have the property, so prepareSandbox
                // cannot pack a stale one whose embedded repo version disagrees with gradle.properties.
                wrappersBundleFile.get().asFile.delete()
            }
            wrappersRepoPath != null
        }

        // Declared so `prepareSandbox` can wire itself to this task by its output (which carries the
        // task dependency) instead of a hand-written `dependsOn` plus a literal path. The crawler
        // reads a whole external repo, so there is nothing cheap to hash as an input -- never claim
        // to be up to date rather than risk shipping a silently stale bundle.
        outputs.file(wrappersBundleFile)
        outputs.upToDateWhen { false }

        args(
            wrappersRepoPath ?: "",
            gradleProperty("snakemakeWrappersRepoVersion").get(),
            wrappersBundleFile.get(),
            layout.projectDirectory.file("snakemake_api.yaml")
        )
        maxHeapSize = System.getenv("SNAKECHARM_TEST_HEAP") ?: "1024m" // TC agents are small; override locally, e.g. SNAKECHARM_TEST_HEAP=8g
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
        maxHeapSize = System.getenv("SNAKECHARM_TEST_HEAP") ?: "1024m" // TC agents are small; override locally, e.g. SNAKECHARM_TEST_HEAP=8g
    }


    prepareSandbox {
        // Pack the wrappers bundle into the plugin. Wiring to the *task* rather than to a path carries
        // the task dependency, keeps buildWrappersBundle in the graph so its `onlyIf` still logs the
        // "no wrappers bundled" warning, and packs nothing when that `onlyIf` skipped it -- the skip
        // deletes any bundle an earlier run left behind, so there is no stale file to pick up.
        from(named("buildWrappersBundle")) {
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

        // The suite is heap-hungry: the light fixture caches a project and a mock SDK per descriptor
        // and nothing releases them, so a bad run dies with OutOfMemoryError. Only override when asked
        // to -- with `maxHeapSize` left unset, IntelliJPlatformArgumentProvider passes the IDE's own
        // vmoptions -Xmx (2 GB) instead, and a hardcoded default here would silently cap it lower.
        System.getenv("SNAKECHARM_TEST_HEAP")?.let { maxHeapSize = it }

        // Narrow a local run to tagged scenarios without editing AllCucumberFeaturesTest:
        // CUCUMBER_TAGS='@here' ./gradlew test --tests "features.AllCucumberFeaturesTest"
        // `cucumber.filter.tags` *replaces* @CucumberOptions(tags = "not @ignore") rather than
        // intersecting with it, so re-apply that filter here -- otherwise CUCUMBER_TAGS='@here' also
        // runs the @ignore'd scenarios that happen to carry @here.
        // `?.takeIf { ... }`: an exported-but-empty CUCUMBER_TAGS would otherwise build the tag
        // expression "not @ignore and ()", which cucumber rejects with a parse error instead of
        // running the whole suite.
        System.getenv("CUCUMBER_TAGS")?.takeIf { it.isNotBlank() }?.let {
            systemProperty("cucumber.filter.tags", "not @ignore and ($it)")
        }

        // The 2026.1 Python plugin ships its code as v2 content modules under
        // plugins/python-ce/lib/modules/. That breaks PythonHelpersLocator's jar-path lookup for the
        // Python helpers root (it expects the jar directly under `lib/`, and throws
        // "IllegalStateException: .../python-ce/lib/modules should be lib directory"), which crashes
        // PyTypeShed's lazy init and therefore every test that infers Python types. The locator
        // consults the `idea.python.helpers.path` system property first, so point it at the bundled
        // helpers directory explicitly.
        // Only PyCharm distributions bundle the helpers there; on other platform types (IDEA + the
        // external Python plugin) that directory doesn't exist, and setting the property to a bogus
        // path is worse than not setting it — the locator takes it verbatim, skipping the layout
        // check that would otherwise report the problem.
        // On a PyCharm platform the directory is expected to exist, so a miss there means the layout
        // moved (e.g. a `platformLocalPath` install, or a future repackaging). Say so — otherwise the
        // run just dies with the "should be lib directory" IllegalStateException above and nothing
        // hints that the jvmArg was silently skipped.
        jvmArgumentProviders += CommandLineArgumentProvider {
            val pythonHelpersPath = intellijPlatform.platformPath.resolve("plugins/python-ce/helpers")
            if (pythonHelpersPath.isDirectory()) {
                return@CommandLineArgumentProvider listOf("-Didea.python.helpers.path=$pythonHelpersPath")
            }
            if (isPyCharmPlatform) {
                Logging.getLogger("snakecharm").warn(
                    "Python helpers not found at $pythonHelpersPath, so -Didea.python.helpers.path is not set. " +
                            "Tests that infer Python types will fail with " +
                            "\"IllegalStateException: ... should be lib directory\"."
                )
            }
            emptyList()
        }

        reports {
            // turn off html reports... windows can't handle certain cucumber test name characters.
            junitXml.required.set(true)
            html.required.set(false)
        }
    }

    printProductsReleases {
        // Both channels: EAP answers "what is coming", RELEASE answers "what is the newest build I
        // could target right now", and the two have different newest builds. EAP alone is actively
        // misleading -- with 2026.2.2 (262.10315.174) already out, an EAP-only run reported
        // 262.8665.97 as the newest 262, i.e. a build *older* than the one being built against.
        channels = listOf(ProductRelease.Channel.RELEASE, ProductRelease.Channel.EAP)
        // Follow `platformType` rather than hardcoding one: PyCharm Community (`PC`) publishes
        // nothing from 2025.3 on, so a hardcoded `PyCharmCommunity` would report "no newer release"
        // forever instead of listing the platform we actually build against.
        types = listOf(IntelliJPlatformType.fromCode(gradlePropertyWithPriorityToSystemProperty("platformType")))
        untilBuild = provider { null }
    }
}