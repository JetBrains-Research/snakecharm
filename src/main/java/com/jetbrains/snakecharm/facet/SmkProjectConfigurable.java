package com.jetbrains.snakecharm.facet;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.snakecharm.SnakemakeBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Facet settings component used by Supported Frameworks project configurable
 * <p>
 * Let's rewrite it in Kotlin in some distant future, especially if IDEA will improve *.form integration with Kotlin
 */
public class SmkProjectConfigurable implements SearchableConfigurable {
    @NotNull
    public Project project;

    //// instantiated using reflection + xml based *.form settings:
    private JPanel mainPanel;
    private JCheckBox enableSmkSupportCB;
    private JPanel settingsPanelPlaceHolder;
    //////
    private final SmkFacetSettingsPanel settingsPanel;

    public SmkProjectConfigurable(@NotNull Project project) {
        this.project = project;

        enableSmkSupportCB.addActionListener(e -> updateCompEnabledPropertyRecursively());

        settingsPanel = new SmkFacetSettingsPanel(project);
        settingsPanelPlaceHolder.add(settingsPanel, BorderLayout.CENTER);

        updateCompEnabledPropertyRecursively();
    }

    @Override
    public @NotNull String getId() {
        return "Settings.Snakemake";
    }

    @Override
    public @Nls String getDisplayName() {
        return SnakemakeBundle.Companion.message("smk.framework.configurable.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        final SmkSupportProjectSettings settings = SmkSupportProjectSettings.Companion.getInstance(project);
        return !getUIState().equals(settings.getState());
    }

    /**
     * Is called only for modified configurables
     *
     * @throws ConfigurationException if validation error
     */
    @Override
    public void apply() throws ConfigurationException {
        applyUIStateToProject(getUIState(), project);
    }

    public static void applyUIStateToProject(
            final SmkSupportProjectSettings.State uiState,
            @NotNull final Project project
    ) throws ConfigurationException {
        try {

            final ValidationResult validationResult = SmkSupportedFrameworksConfigurableProvider.Companion.validateWrappersPath(uiState);
            if (!validationResult.isOk()) {
                throw new ConfigurationException(validationResult.getErrorMessage());
            }
            SmkSupportProjectSettings.Companion.updateStateAndFireEvent(project, uiState);
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(e.toString());
        }
    }


    @Override
    public void reset() {
        final SmkSupportProjectSettings conf = SmkSupportProjectSettings.Companion.getInstance(project);
        final SmkSupportProjectSettings.State state = conf.getState();

        // support enabled
        enableSmkSupportCB.setSelected(state.getSnakemakeSupportEnabled());

        // wrappers options:
        settingsPanel.reset(state);

        // fix components enabled properties
        updateCompEnabledPropertyRecursively();
    }

    @Override
    public void disposeUIResources() {
        settingsPanel.disposeUIResources();
    }

    @NotNull
    private SmkSupportProjectSettings.State getUIState() {
        final SmkSupportProjectSettings.State st = new SmkSupportProjectSettings.State();
        st.setSnakemakeSupportEnabled(enableSmkSupportCB.isSelected());
        settingsPanel.apply(st);
        return st;
    }

    private void updateCompEnabledPropertyRecursively() {
        final boolean smkSupportEnabled = enableSmkSupportCB.isSelected();

        // enable all
        UIUtil.setEnabled(settingsPanel, smkSupportEnabled, true);
        if (smkSupportEnabled) {
            // enable/disable wrappers src panel depending on wrappers combobox state
            settingsPanel.updateWrappersSrcPanelEnabledPropertyRecursively();
        }
    }
}
