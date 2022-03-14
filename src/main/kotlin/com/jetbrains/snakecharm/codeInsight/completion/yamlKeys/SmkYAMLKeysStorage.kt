package com.jetbrains.snakecharm.codeInsight.completion.yamlKeys

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.jetbrains.extensions.python.toPsi
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyNumericLiteralExpression
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionExpression
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.*
import java.io.File

// TODO: Support JSON
// TODO: disable unresolved reference inspection for predefined key-value pairs
class SmkYAMLKeysStorage(val project: Project) : Disposable {

    private var reversedYAMLFiles = mutableListOf<SmartPsiElementPointer<YAMLFile>>()
    private var definedPairs = mutableMapOf<String, String>()

    fun initOnStartup() {
        subscribeOnEvents()

        collectYamlKeys(project, updateFolder = true, updateKeyValuePairs = true)
    }

    fun collectYamlKeys(project: Project, updateFolder: Boolean, updateKeyValuePairs: Boolean) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            SnakemakeBundle.message("smk.framework.configuration.files.collecting.data"),
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                val config = SmkSupportProjectSettings.getInstance(project)
                if (updateFolder) {
                    reversedYAMLFiles.clear()
                    processFilesDefinedInConfig(project, config)
                }
                if (updateKeyValuePairs) {
                    definedPairs.clear()
                    processExplicitlyDefinedKeyValuePairs(config)
                }
            }
        })
    }

    /**
     * Returns top-level [YAMLKeyValue] by its name or null
     */
    fun getTopLevelYAMLKeyValueByName(file: SmkFile, name: String) = getYamlFilesForSmkFile(file).firstNotNullOfOrNull {
        YAMLUtil.getTopLevelKeys(it).firstOrNull { key -> (key.keyText == name) }
    }

    /**
     * Returns list of top-level '.yaml' file keys as completion variants
     */
    fun getTopLevelKeysVariants(file: SmkFile): List<String> {
        val result = mutableListOf<String>()
        val allYamlFiles = reversedYAMLFiles.mapNotNull { it.element } + file.advancedCollectConfigFiles()
            .mapNotNull { (it.reference?.resolve() as? YAMLFile) }
        result.addAll(definedPairs.keys)
        result.addAll(allYamlFiles.map { YAMLUtil.getTopLevelKeys(it).map { yamlKeyValue -> yamlKeyValue.keyText } }
            .flatten())
        return result
    }

    /**
     * Returns list of completion variants for 'yaml' key
     */
    fun getCompletionVariantsForOperand(operand: PySubscriptionExpression): Array<String> {
        val file = getFileByOperand(operand) ?: return emptyArray()
        val path = getPathByOperand(operand) ?: return emptyArray()
        val targetPsi = resolveToOperandOrItsChild(file, path) ?: return emptyArray()
        // We don't provide completion for YAMLSequence
        // Because it receives indexes, not keys
        return when (val variantsProvider = (targetPsi as? YAMLKeyValue)?.value ?: targetPsi) {
            is YAMLMapping -> variantsProvider.keyValues.mapNotNull { it.keyText }.toTypedArray()
            else -> return emptyArray()
        }
    }

    /**
     * Resolve to [YAMLPsiElement] by '.yaml' key. There are few possible cases:
     * * [name] is null. So it is SL case, [operand] refers to resolve result
     * * [name] is not null. So it is not SL case, and [operand] is parent of resolve result psi element,
     * and [name] is value of returned [YAMLPsiElement]
     */
    fun resolveToOperandChildByName(operand: PySubscriptionExpression, name: String? = null): YAMLPsiElement? {
        val file = getFileByOperand(operand) ?: return null
        val path = getPathByOperand(operand, name) ?: return null
        return resolveToOperandOrItsChild(file, path)
    }

    /**
     * Returns [SmkFile] of [operand]
     */
    private fun getFileByOperand(operand: PySubscriptionExpression) = when (operand.containingFile) {
        is SmkFile -> operand.containingFile
        else -> operand.containingFile?.context?.containingFile
    } as? SmkFile

    /**
     * Returns full path for target [YAMLPsiElement] by [operand] and [name]
     * For additional details see [resolveToOperandChildByName], [resolveToOperandOrItsChild]
     */
    private fun getPathByOperand(operand: PySubscriptionExpression, name: String? = null) = when (operand) {
        is SmkSLSubscriptionExpression -> operand.getSubscriptionKeys()
        else -> getPathToYAMLKey(operand, name)
    }

    /**
     * Collects all keys from config[][][]...
     * For an example, refer to [YAMLUtil.getConfigFullName()]
     */
    private fun getPathToYAMLKey(
        operand: PySubscriptionExpression,
        initialElement: String? = null
    ): MutableList<String>? {
        val pathToYAMLKey = mutableListOf<String>()
        initialElement?.let { pathToYAMLKey.add(it) }
        var parent: PyExpression = operand
        while (parent is PySubscriptionExpression) {
            when (parent.indexExpression) {
                is PyNumericLiteralExpression -> pathToYAMLKey.add("[${(parent.indexExpression as PyNumericLiteralExpression).text}]")
                else -> pathToYAMLKey.add(parent.indexExpression?.reference?.canonicalText ?: return null)
            }
            parent = parent.operand
        }
        pathToYAMLKey.reverse()
        return pathToYAMLKey
    }

    /**
     * Resolve to [YAMLPsiElement] by path to it.
     *
     * [pathToYAMLKey] is collected from all keys from config[][][]...
     * and will be used as path to target YAML element.
     * For an example, refer to [YAMLUtil.getConfigFullName()]
     */
    private fun resolveToOperandOrItsChild(
        file: SmkFile,
        pathToYAMLKey: MutableList<String>
    ): YAMLPsiElement? {
        val topLevelKey = pathToYAMLKey.removeFirst()

        // Firstly, it finds root YAMLKeyValue, that probably contains target element
        val topLevelYAMLKeyValue = getTopLevelYAMLKeyValueByName(file, topLevelKey)
        if (pathToYAMLKey.isEmpty() || topLevelYAMLKeyValue == null) {
            return topLevelYAMLKeyValue
        }

        // Secondly, it searches for the target element
        return PyUtil.getNullableParameterizedCachedValue(topLevelYAMLKeyValue, pathToYAMLKey) { path ->
            SmkYAMLUtil.getKeyValuePsiElementInFile(topLevelYAMLKeyValue, path)
        }
    }

    private fun processFilesDefinedInConfig(
        project: Project,
        config: SmkSupportProjectSettings
    ) {
        val fileSystem = LocalFileSystem.getInstance()
        val pointerManager = SmartPointerManager.getInstance(project)
        ApplicationManager.getApplication().runReadAction {
            config.configurationFiles.forEach { file ->
                val path = file.path
                if (file.enabled && path != null) {
                    val yamlFile = fileSystem.findFileByIoFile(File(path))?.toPsi(project) as? YAMLFile
                    if (yamlFile != null) {
                        reversedYAMLFiles.add(pointerManager.createSmartPsiElementPointer(yamlFile))
                    }
                }
            }
            // The last file overrides all previous
            // So in order to use '.firstNotNullOfOrNull()'
            // We reverse it
            reversedYAMLFiles.reverse()
        }
    }

    private fun processExplicitlyDefinedKeyValuePairs(
        config: SmkSupportProjectSettings
    ) {
        config.explicitlyDefinedKeyValuePairs.forEach { pair ->
            val key = pair.key
            val value = pair.value
            if (key != null && value != null) {
                definedPairs[key] = value
            }
        }
    }

    /**
     * Collects '.yaml' files that produce keys for specific [file]
     */
    private fun getYamlFilesForSmkFile(file: SmkFile) =
        reversedYAMLFiles.mapNotNull { it.element } + file.advancedCollectConfigFiles()
            .mapNotNull { it.reference?.resolve() as? YAMLFile }

    private fun subscribeOnEvents() {
        val connection = project.messageBus.connect()
        connection.subscribe(SmkSupportProjectSettings.TOPIC, object : SmkSupportProjectSettingsListener {
            override fun stateChanged(
                newSettings: SmkSupportProjectSettings,
                oldState: SmkSupportProjectSettings.State,
                sdkRenamed: Boolean,
                sdkRemoved: Boolean
            ) = collectYamlKeys(
                project,
                newSettings.configurationFiles != oldState.configurationFiles,
                newSettings.explicitlyDefinedKeyValuePairs != oldState.explicitlyDefinedKeyValuePairs
            )

            override fun disabled(newSettings: SmkSupportProjectSettings) = collectYamlKeys(
                project,
                updateFolder = true,
                updateKeyValuePairs = true
            )

            override fun enabled(newSettings: SmkSupportProjectSettings) = collectYamlKeys(
                project,
                updateFolder = true,
                updateKeyValuePairs = true
            )
        })
    }

    override fun dispose() {
        // Same as in SmkWrapperStorage
    }
}