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
                        SnakemakeBundle.message("wrapper.bundled.storage.version")
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

    public boolean isModified(@NotNull SmkFacetState state) {
        return !getUIState().equals(state);
    }

    public void apply(@NotNull SmkFacetConfiguration configuration) throws ConfigurationException {
        final SmkFacetState newState = getUIState();

        final ValidationResult validationResult = SmkFacetEditorTab.Companion.validateWrappersPath(newState);
        if (!validationResult.isOk()) {
            throw new ConfigurationException(validationResult.getErrorMessage());
        }
        configuration.setStateInternal$snakecharm(newState);
    }

    @NotNull
    public SmkFacetState getUIState() {
        return new SmkFacetState(
                wrappersBundledRB.isSelected(),
                FileUtil.toSystemIndependentName(wrappersSrcPathTF.getText().trim())
        );
    }

    public void reset(@NotNull SmkFacetState state) {
        setUIWrappersSrcFolderPath(state.getWrappersCustomSourcesFolder());
        final boolean useBundledWrappersInfo = state.getUseBundledWrappersInfo();
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
