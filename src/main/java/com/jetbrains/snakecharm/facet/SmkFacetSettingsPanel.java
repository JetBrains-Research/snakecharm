package com.jetbrains.snakecharm.facet;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import com.jetbrains.snakecharm.SnakemakeBundle;
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Let's rewrite it in Kotlin in some distant future, especially if IDEA will improve *.form integration with Kotlin
 */
public class SmkFacetSettingsPanel extends JPanel {
    //// instantiated using reflection + xml based *.form settings:
    private JPanel myContentPane;
    private JRadioButton wrappersBundledRB;
    private JRadioButton wrappersFromSrcRB;
    private TextFieldWithBrowseButton wrappersSrcPathTF;
    private JPanel wrappersSrcPathSettingsPanel;
    private JBLabel wrappersSrcPathSettingsHint;
    //////

    // TODO [romeo]: add SdkCombobox, see IdeaGradleProjectSettingsControlBuilder impl

    public SmkFacetSettingsPanel(
            final Project project
    ) {
        setLayout(new BorderLayout());
        add(myContentPane, BorderLayout.CENTER);

        wrappersBundledRB.setText(
                SnakemakeBundle.message(
                        "facet.settings.wrappers.bundled",
                        SnakemakeAPI.SMK_WRAPPERS_BUNDLED_REPO
                )
        );

        addFolderChooser(
                SnakemakeBundle.message("facet.settings.wrappers.sources.path.chooser"),
                wrappersSrcPathTF, project
        );

        wrappersFromSrcRB.addActionListener(e -> updateWrappersSrcPanelEnabled());
        wrappersBundledRB.addActionListener(e -> updateWrappersSrcPanelEnabled());

        wrappersSrcPathSettingsHint.setText(
                XmlStringUtil.wrapInHtml(SnakemakeBundle.message("facet.settings.wrappers.sources.help"))
        );
        wrappersSrcPathSettingsHint.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        wrappersSrcPathSettingsHint.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse("https://github.com/snakemake/snakemake-wrappers");
                super.mouseClicked(e);
            }
        });

        updateWrappersSrcPanelEnabled();
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

    public boolean isModified(@NotNull SmkFacetConfiguration configuration) {
        return !getUIState().equals(configuration.getState());
    }

    public void apply(@NotNull SmkFacetConfiguration configuration) throws ConfigurationException {
        final SmkFacetConfiguration.State newState = getUIState();

        final ValidationResult validationResult = SmkFacetEditorTab.Companion.validateWrappersPath(newState);
        if (!validationResult.isOk()) {
            throw new ConfigurationException(validationResult.getErrorMessage());
        }
        configuration.loadState(newState);
    }

    @NotNull
    public SmkFacetConfiguration.State getUIState() {
        final SmkFacetConfiguration.State st = new SmkFacetConfiguration.State();
        st.setUseBundledWrappersInfo(wrappersBundledRB.isSelected());
        st.setWrappersCustomSourcesFolder(FileUtil.toSystemIndependentName(wrappersSrcPathTF.getText().trim()));
        return st;
    }

    public void reset(@NotNull SmkFacetConfiguration configuration) {
        setUIWrappersSrcFolderPath(configuration.getWrappersCustomSourcesFolder());

        final boolean useBundledWrappersInfo = configuration.getUseBundledWrappersInfo();
        wrappersBundledRB.setSelected(useBundledWrappersInfo);
        wrappersFromSrcRB.setSelected(!useBundledWrappersInfo);

        updateWrappersSrcPanelEnabled();
    }

    public void disposeUIResources() {
        // Do nothing
    }

    public static LabeledComponent<TextFieldWithBrowseButton> createScriptPathComponent(
            final Ref<? super TextFieldWithBrowseButton> testScriptTextFieldWrapper,
            final String text
    ) {
        final TextFieldWithBrowseButton testScriptTextField = new TextFieldWithBrowseButton();
        testScriptTextFieldWrapper.set(testScriptTextField);

        final LabeledComponent<TextFieldWithBrowseButton> myComponent = new LabeledComponent<>();
        myComponent.setComponent(testScriptTextField);
        myComponent.setText(text);

        return myComponent;
    }

    public JComponent[] getComponentsToValidate() {
        return new JComponent[]{wrappersSrcPathTF, wrappersBundledRB, wrappersFromSrcRB};
    }

    private void setUIWrappersSrcFolderPath(final String path) {
        wrappersSrcPathTF.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(path)));
    }

    public void updateWrappersSrcPanelEnabled() {
        UIUtil.setEnabled(wrappersSrcPathSettingsPanel, wrappersFromSrcRB.isSelected(), true);
    }
}
