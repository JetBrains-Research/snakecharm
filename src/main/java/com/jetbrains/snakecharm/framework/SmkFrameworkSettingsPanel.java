package com.jetbrains.snakecharm.framework;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import com.jetbrains.snakecharm.SnakemakeBundle;
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Snakemake framework settings for:
 *  - supported frameworks page
 *  - new project wizards
 *
 * Let's rewrite it in Kotlin in some distant future, especially if IDEA will improve *.form integration with Kotlin
 */
public class SmkFrameworkSettingsPanel extends JPanel {
    //// instantiated using reflection + xml based *.form settings:
    private JPanel myContentPane;
    private JRadioButton wrappersBundledRB;
    private JRadioButton wrappersFromSrcRB;
    private TextFieldWithBrowseButton wrappersSrcPathTF;
    private JPanel wrappersSrcPathSettingsPanel;
    private JBLabel wrappersSrcPathSettingsHint;
    //////

    // TODO [romeo]: add SdkCombobox, see IdeaGradleProjectSettingsControlBuilder impl

    public SmkFrameworkSettingsPanel(@Nullable final Project project) {
        setLayout(new BorderLayout());
        add(myContentPane, BorderLayout.CENTER);

        wrappersBundledRB.setText(
                SnakemakeBundle.message(
                        "smk.framework.configurable.panel.wrappers.bundled",
                        SnakemakeAPI.SMK_WRAPPERS_BUNDLED_REPO
                )
        );

        addFolderChooser(
                SnakemakeBundle.message("smk.framework.configurable.panel.wrappers.sources.path.chooser"),
                wrappersSrcPathTF, project
        );

        wrappersFromSrcRB.addActionListener(e -> updateWrappersSrcPanelEnabledPropertyRecursively());
        wrappersBundledRB.addActionListener(e -> updateWrappersSrcPanelEnabledPropertyRecursively());

        wrappersSrcPathSettingsHint.setText(
                XmlStringUtil.wrapInHtml(SnakemakeBundle.message("smk.framework.configurable.panel.wrappers.sources.help"))
        );
        wrappersSrcPathSettingsHint.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        wrappersSrcPathSettingsHint.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse("https://github.com/snakemake/snakemake-wrappers");
                super.mouseClicked(e);
            }
        });

        updateWrappersSrcPanelEnabledPropertyRecursively();
    }

    private void createUIComponents() {
        // method that is called after form initialization from xml  (reflection)
        // required for custom components initialization

        // Do nothing, no custom components, just use this methods as a remainder that
        // it is API for custom comps initialization
    }

    public static FileChooserDescriptor addFolderChooser(
            @NotNull final String title,
            @NotNull final TextFieldWithBrowseButton textField,
            final Project project
    ) {
        final FileChooserDescriptor folderChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        folderChooserDescriptor.setTitle(title);
        textField.addBrowseFolderListener(title, null, project, folderChooserDescriptor);

        return folderChooserDescriptor;
    }

    public void apply(@NotNull SmkSupportProjectSettings.State state) {
        state.setUseBundledWrappersInfo(wrappersBundledRB.isSelected());
        state.setWrappersCustomSourcesFolder(FileUtil.toSystemIndependentName(wrappersSrcPathTF.getText().trim()));
    }

    public void reset(@NotNull SmkSupportProjectSettings.State state) {
        setUIWrappersSrcFolderPath(state.getWrappersCustomSourcesFolder());

        final boolean useBundledWrappersInfo = state.getUseBundledWrappersInfo();
        wrappersBundledRB.setSelected(useBundledWrappersInfo);
        wrappersFromSrcRB.setSelected(!useBundledWrappersInfo);

        updateWrappersSrcPanelEnabledPropertyRecursively();
    }

    public void disposeUIResources() {
        // Do nothing
    }

    private void setUIWrappersSrcFolderPath(final String path) {
        wrappersSrcPathTF.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(path)));
    }

    public void updateWrappersSrcPanelEnabledPropertyRecursively() {
        UIUtil.setEnabled(wrappersSrcPathSettingsPanel, wrappersFromSrcRB.isSelected(), true);
    }
}
