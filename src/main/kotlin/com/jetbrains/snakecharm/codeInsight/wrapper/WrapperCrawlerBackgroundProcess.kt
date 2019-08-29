package com.jetbrains.snakecharm.codeInsight.wrapper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.regex.Pattern

class WrapperCrawlerBackgroundProcess : StartupActivity {
    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                    project,
                    "Fetching wrapper repository data",
                    false
            ) {
                override fun run(indicator: ProgressIndicator) {
                    // only fetching the latest tag to try to avoid exceeding rate limit
                    val tagNumberPattern = Pattern.compile("v?\\d*\\.(\\d*)\\.(\\d*)")
                    val latestTag = collectTags().sortedWith(compareBy( { tag ->
                        val matcher = tagNumberPattern.matcher(tag)
                        matcher.find()
                        matcher.group(1).toInt()
                    }, {tag ->
                        val matcher = tagNumberPattern.matcher(tag)
                        matcher.find()
                        matcher.group(2).toInt()
                    })).last()

                    // fetch data if a new tag is available or if there's no saved data
                    val cachedWrappers = WrapperStorage.getInstance().getWrapperList()
                    if (cachedWrappers.isEmpty() || cachedWrappers.map { it.repositoryTag }.all { it < latestTag }) {
                        val latestCommitInTag = getTagLatestCommitHash(latestTag)
                        crawlDirectory(
                                latestTag,
                                "$BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL/src/$latestCommitInTag/",
                                "",
                                project
                        )
                    }
                }
            })
        }
    }

    private fun collectTags(): List<String> {
        val tags = mutableListOf<String>()
        var url = "$BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL/refs/tags"
        while (true) {
            val tagsResponseJSONObject = khttp.get(url).jsonObject
            (tagsResponseJSONObject["values"] as JSONArray).map { tags.add((it as JSONObject)["name"] as String) }
            if (tagsResponseJSONObject.isNull("next")) {
                break
            }
            url = tagsResponseJSONObject["next"] as String
        }
        return tags
    }

    private fun getTagLatestCommitHash(tag: String): String? {
        val commitsResponseJSONObject = khttp.get("$BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL/commits/$tag").jsonObject
        val latestCommitJSONObject = (commitsResponseJSONObject["values"] as JSONArray)[0] as JSONObject
        return if (latestCommitJSONObject.isNull("hash")) null else latestCommitJSONObject["hash"] as String
    }

    private fun crawlDirectory(
            tag: String,
            urlStart: String,
            directory: String,
            project: Project
    ) {
        var url = "$urlStart/$directory"
        while (true) {
            val directoryContentJSONObject =
                    try {
                        khttp.get(url).jsonObject
                    } catch (e: JSONException) {
                        val notification = Notification(
                                WRAPPER_PROCESS_ID,
                                WRAPPER_FETCHING_ERROR_HEADER,
                                WRAPPER_FETCHING_ERROR_CONTENT,
                                NotificationType.WARNING
                        )
                        notification.notify(project)
                        null
                    } ?: return
            val files = (directoryContentJSONObject["values"] as JSONArray)
                    .filter { (it as JSONObject)["type"] == "commit_file" }
                    .map { it as JSONObject }
            val directories = (directoryContentJSONObject["values"] as JSONArray)
                    .filter { (it as JSONObject)["type"] == "commit_directory" }
                    .map { it as JSONObject }
            if (files.any { (it["path"] as String).endsWith(SmkWrapperUtil.SMK_WRAPPER_FILE_NAME) }) {
                val environment = findFileAndGetPath(SmkWrapperUtil.SMK_ENVIRONMENT_FILE_NAME, files)
                val envinronmentContent = environment?.let { path -> getFileContentFromPath("$urlStart/$path") }
                        ?: FILE_UNAVAILABLE
                val meta = findFileAndGetPath(SmkWrapperUtil.SMK_META_FILE_NAME, files)
                val metaContent = meta?.let { path -> getFileContentFromPath("$urlStart/$path") }
                        ?: FILE_UNAVAILABLE
                val test = findFileAndGetPath(SmkWrapperUtil.SMK_TEST_DIRECTORY_NAME, directories)
                val exampleSnakefile = test?.let {testDir ->
                    try {
                        val directoryContent = khttp.get("$urlStart/$testDir").jsonObject
                        val path = findFileAndGetPath(
                                SmkWrapperUtil.SMK_TEST_SNAKEFILE_NAME,
                                (directoryContent["values"] as JSONArray).map { it as JSONObject }
                        )
                        path?.let { it -> getFileContentFromPath("$urlStart/$it") }
                    } catch (e: JSONException) {
                        val notification = Notification(
                                WRAPPER_PROCESS_ID,
                                WRAPPER_FETCHING_ERROR_HEADER,
                                WRAPPER_FETCHING_ERROR_CONTENT,
                                NotificationType.WARNING
                        )
                        notification.notify(project)
                        null
                    }
                } ?: FILE_UNAVAILABLE
                WrapperStorage.getInstance().addWrapper(WrapperStorage.Wrapper(
                        tag,
                        directory,
                        envinronmentContent,
                        metaContent,
                        exampleSnakefile

                ))
                return
            }
            directories
                    .filter { (it["path"] as String).startsWith("bio") }
                    .forEach {
                        crawlDirectory(
                                tag,
                                urlStart,
                                it["path"] as String,
                                project
                        )
                    }
            if (directoryContentJSONObject.isNull("next")) {
                break
            }
            url = directoryContentJSONObject["next"] as String
        }
    }

    private fun findFileAndGetPath(name: String, pathObjects: List<JSONObject>) =
            pathObjects.find { (it["path"] as String).endsWith(name) }?.let { it["path"] as String }

    private fun getFileContentFromPath(path: String) =
            khttp.get(path).content.toString(Charset.forName("UTF-8"))

    companion object {
        private const val DEFAULT_WRAPPER_PATH = "snakemake/snakemake-wrappers"
        private const val BITBUCKET_API_URL = "https://api.bitbucket.org/2.0"
        const val BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL = "$BITBUCKET_API_URL/repositories/$DEFAULT_WRAPPER_PATH"

        const val WRAPPER_PROCESS_ID = "Wrapper repository fetching"
        private const val WRAPPER_FETCHING_ERROR_HEADER = "Unable to retrieve wrapper repository data"
        private const val WRAPPER_FETCHING_ERROR_CONTENT = "Rate limit for repository data requests has been exceeded."

        private const val FILE_UNAVAILABLE = "File unavailable."
    }
}