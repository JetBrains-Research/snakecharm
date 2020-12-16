package com.jetbrains.snakecharm.facet;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.snakecharm.SnakemakeBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Facet settings component used by Supported Frameworks project configurable
 *
 * Let's rewrite it in Kotlin in some distant future, especially if IDEA will improve *.form integration with Kotlin
 */
public class SmkSupportedFrameworksModuleConfigurable implements SearchableConfigurable {
    private final Module module;
    private JPanel mainPanel;
    private JCheckBox enableSmkSupportCB;
    private JPanel settingsPanelPlaceHolder; // fake panel in GUI builder where settings panel is inserted
    private final SmkFacetSettingsPanel settingsPanel;

    public SmkSupportedFrameworksModuleConfigurable(@NotNull Module module) {
        this.module = module;
        settingsPanel = new SmkFacetSettingsPanel(module.getProject());
        settingsPanelPlaceHolder.add(settingsPanel, BorderLayout.CENTER);
        enableSmkSupportCB.addActionListener(e -> updatePanelEnabled());
        updatePanelEnabled();
    }

    @Override
    public @NotNull String getId() {
        return "Settings.Snakemake";
    }

    @Override
    public @Nls String getDisplayName() {
        return SnakemakeBundle.Companion.message("facet.configurable.display.name");
    }

    @Override
    public @Nullable JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        final boolean selected = enableSmkSupportCB.isSelected();

        if (selected != SnakemakeFacet.Companion.isPresent(module)) {
            return true;
        }
        if (selected) {
            final SnakemakeFacet facet = SnakemakeFacet.Companion.getInstance(module);
            return facet == null || settingsPanel.isModified(facet.getConfiguration().getState());
        }

        return false;
    }

    /**
     * Is called only for modified configurables
     * @throws ConfigurationException if validation error
     */
    @Override
    public void apply() throws ConfigurationException {
        try {
            if (enableSmkSupportCB.isSelected()) {
                // add facet or change settings
                final SnakemakeFacet facet = SnakemakeFacet.getInstance(module);
                if (facet != null) {
                    // update settings
                    final SmkFacetConfiguration configuration = facet.getConfiguration();
                    settingsPanel.apply(configuration);
                    ApplicationManager.getApplication().runWriteAction(()
                            -> FacetManager.getInstance(module).facetConfigurationChanged(facet));
                } else {
                    // add facet
                    final SmkFacetConfiguration configuration = SmkFacetType.createDefaultConfiguration(module.getProject());
                    settingsPanel.apply(configuration);
                    SmkFacetType.createAndAddFacet(module, configuration);
                }
            } else {
                // enable support not selected => remove facet if unselected
                final ModifiableFacetModel model = FacetManager.getInstance(module).createModifiableModel();
                model.removeFacet(SnakemakeFacet.getInstance(module));
                ApplicationManager.getApplication().runWriteAction(model::commit);
            }
        } catch (ConfigurationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ConfigurationException(e.toString());
        }
    }


    @Override
    public void reset() {
        enableSmkSupportCB.setEnabled(true);
        final SnakemakeFacet instance = SnakemakeFacet.getInstance(module);
        if (instance != null) {
            enableSmkSupportCB.setSelected(true);
            settingsPanel.setEnabled(true);
            settingsPanel.reset(instance.getConfiguration().getState());
        } else {
            enableSmkSupportCB.setSelected(false);
            settingsPanel.setEnabled(false);
        }

        updatePanelEnabled();
    }

    @Override
    public void disposeUIResources() {
        settingsPanel.disposeUIResources();
    }

    private void updatePanelEnabled() {
        final boolean smkSupportEnabled = enableSmkSupportCB.isSelected();
        UIUtil.setEnabled(settingsPanel, smkSupportEnabled, true);
        if (smkSupportEnabled) {
            settingsPanel.updateWrappersSrcPanelEnabled();
        }
    }
}
