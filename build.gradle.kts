
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")

    // Kotlin support
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"

    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    // This plugin allows you to build plugins for IntelliJ platform using specific
    // IntelliJ SDK and bundled plugins.
    id("org.jetbrains.intellij") version "1.2.1"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
    id("io.gitlab.arturbosch.detekt") version "1.18.1"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

group = properties("pluginGroup")
version = "${properties("pluginVersion")}.${properties("pluginBuildCounter")}${properties("pluginPreReleaseSuffix")}"

// Configure project's dependencies
repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.18.1")

    testImplementation("io.cucumber:cucumber-java:7.0.0")
    testImplementation("io.cucumber:cucumber-junit:7.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.3.0")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    val platformType = properties("platformType")

    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(platformType)
    downloadSources.set(properties("platformDownloadSources").toBoolean())
    updateSinceUntilBuild.set(true)

    val isPyCharm = platformType == "PC" || platformType == "PY" || platformType == "PD"
    sandboxDir.set("${project.rootDir}/.sandbox${if (isPyCharm) "_pycharm" else "" }")
    ideaDependencyCachePath.set("${project.rootDir}/.idea_distrib_cache")  // Useful for Windows due to short cmdline path

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    val platformPlugins = ArrayList<String>()
    when (platformType) {
        "PC" -> platformPlugins.add("python-ce")
        "PY", "PD" -> platformPlugins.add("python")
        else -> platformPlugins.add(properties("pythonPlugin"))
    }
    platformPlugins.addAll(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    plugins.set(platformPlugins)
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
// Configuration: https://github.com/JetBrains/gradle-changelog-plugin#configuration
changelog {
    version.set(project.version.toString())
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

tasks {
    // TODO [1.8] ?
    // Set the compatibility versions to 11
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    withType<Detekt> {
        jvmTarget = "11"
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            File(projectDir, "README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider { changelog.getLatest().toHTML() })
    }

    runPluginVerifier {
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',')
            .map(String::trim).filter(String::isNotEmpty))
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
        // Cucumber tests are executed using `AllCucumberFeaturesTest` JUnit runner
        // this task is an alternative if `AllCucumberFeaturesTest` stop working for
        // some reason, normally we don't need this
        dependsOn("assemble", "compileTestKotlin")
        doLast {
            javaexec {
                main = "io.cucumber.core.cli.Main"
                classpath =  project.sourceSets.test.get().runtimeClasspath
                systemProperties(
                    "idea.config.path" to "${intellij.sandboxDir.get()}/config-test",
                    "idea.system.path" to "${intellij.sandboxDir.get()}/system-test",
                    "idea.plugins.path" to "${intellij.sandboxDir.get()}/plugins-test"
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
                main = "com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler"
                classpath =  project.sourceSets.main.get().runtimeClasspath + files(intellij.ideaDependency.get().jarFiles)
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
                main = "com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler"
                classpath =  project.sourceSets.main.get().runtimeClasspath + files(intellij.ideaDependency.get().jarFiles)
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
        dependsOn("buildTestWrappersBundle")
        reports {
            // turn off html reports... windows can't handle certain cucumber test name characters.
            junitXml.required.set(true)
            html.required.set(false)
        }

        include("**/*Test.class")
//        include("**/AllCucumberFeaturesTest.class")  // Uncomment to disable gradle tests
//        include("**/AllCucumberFeaturesTest.class")  // Uncomment to disable gradle tests
    }
}