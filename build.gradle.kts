@file:Suppress("SpellCheckingInspection")

import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants.Configurations
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil.exists
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()
fun propertiesNullable(key: String) = project.findProperty(key)?.toString()
fun properties2(key: String) = providers.gradleProperty(key)

plugins {
    // Java support
    id("java")

    // Kotlin support latest: 1.9.25
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.serialization") version "1.9.25"

    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    // This plugin allows you to build plugins for IntelliJ platform using specific
    // IntelliJ SDK and bundled plugins.
    id("org.jetbrains.intellij.platform") version "2.1.0"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = if (properties("pluginPreReleaseSuffix").isEmpty()) {
    "${properties("pluginVersion")}${properties("pluginPreReleaseSuffix")}"
} else {
    "${properties("pluginVersion")}${properties("pluginPreReleaseSuffix")}.${properties("pluginBuildCounter")}"
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}


dependencies {
    // TODO: refactor using aliases
    testImplementation("io.cucumber:cucumber-java:7.8.1")
    testImplementation("io.cucumber:cucumber-junit:7.8.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.25")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.25")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.4.1")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    intellijPlatform {
        val platformType = properties("platformType")

        val platformLocalPath = propertiesNullable("platformLocalPath")
        if (platformLocalPath != null) {
            if (!exists(platformLocalPath)) {
                logger.error("Custom platfrom path not exist: $platformLocalPath")
            } else {
                logger.warn("Using custom platfrom path: $platformLocalPath")
            }
            //See https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#intellij-extension-localpath
            local(platformLocalPath)
        } else {
            logger.warn("Use version: ${properties("platformVersion")}")
            create(platformType, properties("platformVersion"))
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
            else -> plugin(properties("pythonPlugin"))
        }
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(properties("platformBundledPlugins").split(',').map(String::trim).filter(String::isNotEmpty))

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
    buildSearchableOptions = true
    instrumentCode = true
    projectName = project.name

    val platformType = properties("platformType")
    val isPyCharm = platformType == "PC" || platformType == "PY" || platformType == "PD"
    sandboxContainer = file("${project.rootDir}/.sandbox${if (isPyCharm) "_pycharm" else ""}")

    pluginConfiguration {
        name = properties2("pluginName")

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
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
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
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }


    publishing {
        token.set(properties("intellijPublishToken"))

        // plugin version is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf("$version".split('-').getOrElse(1) { "default" }.split('.').first()))
        // TODO: is ok to use version? or project.version? or ?
        //channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
            select {
                types = listOf(IntelliJPlatformType.PyCharmCommunity, IntelliJPlatformType.PyCharmProfessional)
                channels = listOf(ProductRelease.Channel.EAP, ProductRelease.Channel.RELEASE)
                sinceBuild = "242"
                untilBuild = "243.*"
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

java {
    // TODO: not sure, do we really need this, let's try to cleanup?
    // Set bytecode version project level
    val javaVersion = JavaVersion.toVersion(properties("javaVersion"))
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {

//    runIde {
//        // Test plugin dynamic unload:
//        // See https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html#diagnosing-leaks
//        // * Set to true registry property: ide.plugins.snapshot.on.unload.fail
//        // * uncomment
//        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions")
//
//        jvmArgumentProviders += CommandLineArgumentProvider {
//            listOf("-Dname=value")
//        }
//    }

    withType<KotlinCompile> {
        // Set Bycode version on task-level
        // TODO: not sure, do we really need this, let's try to cleanup?
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(properties("javaVersion"))
        }


        kotlinOptions {
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }

    withType<JavaCompile> {
        // Set Bycode version on task-level
        // TODO: not sure, do we really need this, let's try to cleanup?
        val javaVersion = providers.gradleProperty("javaVersion").get()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
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
            providers.gradleProperty("snakemakeWrappersRepoPath").get(),
            providers.gradleProperty("snakemakeWrappersRepoVersion").get(),
            layout.buildDirectory.file("bundledWrappers/smk-wrapper-storage-bundled.cbor").get(),
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
        )
        maxHeapSize = "1024m" // Not much RAM is available on TC agents
    }


    prepareSandbox {
        // Pack wrappers bundle into plugin:
        dependsOn("buildWrappersBundle")

        from(layout.buildDirectory.file("bundledWrappers/smk-wrapper-storage-bundled.cbor")) {
            into(pluginName.map { "$it/extra" })
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