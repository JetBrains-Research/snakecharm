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
import java.io.IOException
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
                    val tagNumberPattern = Pattern.compile(SmkWrapperUtil.TAG_NUMBER_REGEX_STRING)
                    val (res, tags) = collectTags()
                    if (!res) {
                        notifyAboutWrapperFetchingError(project)
                        return
                    }

                    val latestTag = tags.sortedWith(compareBy({ tag ->
                        val matcher = tagNumberPattern.matcher(tag)
                        matcher.find()
                        matcher.group(2).toInt()
                    }, { tag ->
                        val matcher = tagNumberPattern.matcher(tag)
                        matcher.find()
                        matcher.group(3).toInt()
                    })).lastOrNull()
                            ?: return

                    // fetch data if a new tag is available or if there's no saved data
                    val cachedWrappers = WrapperStorage.getInstance().getWrapperList()
                    if (cachedWrappers.isEmpty() || cachedWrappers.map { it.repositoryTag }.all { it < latestTag }) {
                        val latestCommitInTag = getTagLatestCommitHash(latestTag)
                        if (latestCommitInTag == null) {
                            notifyAboutWrapperFetchingError(project)
                            return
                        }
                        if (!crawlDirectory(
                                        latestTag,
                                        "$BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL/src/$latestCommitInTag/",
                                        "",
                                        project
                                )) {
                            notifyAboutWrapperFetchingError(project)
                        }
                    }
                }
            })
        }
    }

    private fun collectTags(): Pair<Boolean, List<String>> {
        val tags = mutableListOf<String>()
        var url = "$BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL/refs/tags"
        while (true) {
            val tagsResponse = try {
                khttp.get(url)
            } catch (e: IOException) {
                return false to tags
            }

            if (tagsResponse.statusCode != 200) {
                return false to tags
            }
            val tagsResponseJSONObject = tagsResponse.jsonObject
            (tagsResponseJSONObject["values"] as JSONArray).map { tags.add((it as JSONObject)["name"] as String) }
            if (tagsResponseJSONObject.isNull("next")) {
                break
            }
            url = tagsResponseJSONObject["next"] as String
        }
        return true to tags
    }

    private fun getTagLatestCommitHash(tag: String): String? {
        val commitsResponse = try {
            khttp.get("$BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL/commits/$tag")
        } catch (e: IOException) {
            return null
        }

        if (commitsResponse.statusCode != 200) {
            return null
        }

        val commitsResponseJSONObject = commitsResponse.jsonObject
        val latestCommitJSONObject = (commitsResponseJSONObject["values"] as JSONArray)[0] as JSONObject
        return if (latestCommitJSONObject.isNull("hash")) null else latestCommitJSONObject["hash"] as String
    }

    private fun crawlDirectory(
            tag: String,
            urlStart: String,
            directory: String,
            project: Project
    ): Boolean {
        var url = "$urlStart/$directory"
        while (true) {
            val directoryContentJSONObject =
                    try {
                        khttp.get(url).jsonObject
                    } catch (e: IOException) {
                        return false
                    } catch (e: JSONException) {
                        return false
                    }
            val files = (directoryContentJSONObject["values"] as JSONArray)
                    .filter { (it as JSONObject)["type"] == "commit_file" }
                    .map { it as JSONObject }
            val directories = (directoryContentJSONObject["values"] as JSONArray)
                    .filter { (it as JSONObject)["type"] == "commit_directory" }
                    .map { it as JSONObject }
            if (files.any { (it["path"] as String).endsWith(SmkWrapperUtil.SMK_WRAPPER_FILE_NAME) }) {
                // it is assumed that environment/meta/test should always be present
                // (true for latest tags, false for earlier, there's a probability it could change in the future)
                // if we couldn't get those, must be a network error
                val environment = findFileAndGetPath(SmkWrapperUtil.SMK_ENVIRONMENT_FILE_NAME, files)
                val envinronmentContent = environment?.let { path -> getFileContentFromPath("$urlStart/$path") }
                        ?: return false
                val meta = findFileAndGetPath(SmkWrapperUtil.SMK_META_FILE_NAME, files)
                val metaContent = meta?.let { path -> getFileContentFromPath("$urlStart/$path") }
                        ?: return false
                val test = findFileAndGetPath(SmkWrapperUtil.SMK_TEST_DIRECTORY_NAME, directories)
                val exampleSnakefile = test?.let {testDir ->
                    try {
                        val directoryContent = khttp.get("$urlStart/$testDir").jsonObject
                        val path = findFileAndGetPath(
                                SmkWrapperUtil.SMK_TEST_SNAKEFILE_NAME,
                                (directoryContent["values"] as JSONArray).map { it as JSONObject }
                        )
                        path?.let { it -> getFileContentFromPath("$urlStart/$it") }
                    } catch(e: IOException) {
                        return false
                    } catch (e: JSONException) {
                        return false
                    }
                } ?: return false
                WrapperStorage.getInstance().addWrapper(WrapperStorage.Wrapper(
                        tag,
                        directory,
                        envinronmentContent,
                        metaContent,
                        exampleSnakefile

                ))
                return true
            }
            directories
                    .filter { (it["path"] as String).startsWith("bio") }
                    .forEach {
                        val res = crawlDirectory(
                                tag,
                                urlStart,
                                it["path"] as String,
                                project
                        )
                        if (!res) {
                            return false
                        }
                    }
            if (directoryContentJSONObject.isNull("next")) {
                break
            }
            url = directoryContentJSONObject["next"] as String
        }

        return true
    }

    private fun findFileAndGetPath(name: String, pathObjects: List<JSONObject>) =
            pathObjects.find { (it["path"] as String).endsWith(name) }?.let { it["path"] as String }

    private fun getFileContentFromPath(path: String) =
            try {
                khttp.get(path).content.toString(Charset.forName("UTF-8"))
            } catch (e: IOException) {
                null
            }

    private fun notifyAboutWrapperFetchingError(project: Project) =
            Notification(
                    WRAPPER_PROCESS_ID,
                    WRAPPER_FETCHING_ERROR_HEADER,
                    WRAPPER_FETCHING_ERROR_CONTENT,
                    NotificationType.WARNING
            ).notify(project)

    companion object {
        private const val DEFAULT_WRAPPER_PATH = "snakemake/snakemake-wrappers"
        private const val BITBUCKET_API_URL = "https://api.bitbucket.org/2.0"
        const val BITBUCKET_WRAPPER_REPOSITORY_QUERY_URL = "$BITBUCKET_API_URL/repositories/$DEFAULT_WRAPPER_PATH"

        const val WRAPPER_PROCESS_ID = "Wrapper repository fetching"
        private const val WRAPPER_FETCHING_ERROR_HEADER = "Unable to retrieve wrapper repository data"
        private const val WRAPPER_FETCHING_ERROR_CONTENT =
                "Unable to retrieve all data from wrapper repository (some data might be available). " +
                        "Please check your Internet connection. " +
                        "If your connection is working, rate limit for repository data requests might have been exceeded."
    }
}