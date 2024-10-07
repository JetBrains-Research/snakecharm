package com.jetbrains.snakecharm.framework;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import com.jetbrains.python.PyBundle;
import com.jetbrains.python.configuration.PyConfigurableInterpreterList;
import com.jetbrains.python.newProject.steps.PythonSdkChooserCombo;
import com.jetbrains.python.sdk.PreferredSdkComparator;
import com.jetbrains.python.sdk.PySdkListCellRenderer;
import com.jetbrains.snakecharm.SnakemakeBundle;
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snakemake framework settings for:
 * - supported frameworks page
 * - new project wizards
 *
 * Let's rewrite it in Kotlin in some distant future, especially if IDEA will improve *.form integration with Kotlin
 */
public class SmkFrameworkSettingsPanel extends JPanel implements Disposable {
    @NotNull
    private final Project project;
    //// instantiated using reflection + xml based *.form settings:
    private JPanel myContentPane;
    private JRadioButton wrappersBundledRB;
    private JRadioButton wrappersFromSrcRB;
    private TextFieldWithBrowseButton wrappersSrcPathTF;
    private JPanel wrappersSrcPathSettingsPanel;
    private JBLabel wrappersSrcPathSettingsHint;
    private JBLabel pythonInterpreterHintLabel;
    // See sdk chooser (Combobox+PySdkListCellRenderer) in 'PyPluginCommonOptionsForm' or use `PythonSdkChooserCombo`
    private PythonSdkChooserCombo pythonSdkCB;

    @SuppressWarnings("unchecked")
    public SmkFrameworkSettingsPanel(@Nullable final Project project) {
        this.project = project != null ? project : ProjectManager.getInstance().getDefaultProject();

        setLayout(new BorderLayout());
        add(myContentPane, BorderLayout.CENTER);

        final SmkWrapperStorage wrapperStorage = SmkWrapperStorage.Companion.getInstance(this.project);
        final String wrappersRepoVersion = wrapperStorage.getVersion();
        wrappersBundledRB.setText(
                SnakemakeBundle.message(
                        "smk.framework.configurable.panel.wrappers.bundled",
                        wrappersRepoVersion.isEmpty() ? "n/a" : wrappersRepoVersion
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

        pythonInterpreterHintLabel.setText(
                XmlStringUtil.wrapInHtml(SnakemakeBundle.message("smk.framework.configurable.panel.sdk.hint"))
        );
        pythonInterpreterHintLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        
        // final String nullSdkLabel = PyBundle.message("python.sdk.there.is.no.interpreter");
        final String nullSdkLabel = PyBundle.message("python.sdk.rendering.project.default");
        final JComboBox sdksCombobox = pythonSdkCB.getComboBox();
        sdksCombobox.setRenderer(new PySdkListCellRenderer("<" + nullSdkLabel + ">"));
        sdksCombobox.setPreferredSize(new Dimension(100, sdksCombobox.getPreferredSize().height));
        pythonSdkCB.addChangedListener(e -> refreshSdkList(getSelectedSdk()));

        final MessageBusConnection connection = this.project.getMessageBus().connect();
        connection.subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, new ProjectJdkTable.Listener() {
            @Override
            public void jdkAdded(@NotNull Sdk jdk) {
                refreshSdkList(getSelectedSdk());
            }

            @Override
            public void jdkRemoved(@NotNull Sdk sdk) {
                final Sdk currentSelectedSdk = getSelectedSdk();
                if (currentSelectedSdk != null && currentSelectedSdk.getName().equals(sdk.getName())) {
                    refreshSdkList(null);
                } else {
                    refreshSdkList(currentSelectedSdk);
                }
            }

            @Override
            public void jdkNameChanged(@NotNull Sdk jdk, @NotNull String previousName) {
                final Sdk currentSelectedSdk = getSelectedSdk();
                if (currentSelectedSdk != null && previousName.equals(currentSelectedSdk.getName())) {
                    refreshSdkList(jdk);
                } else {
                    refreshSdkList(currentSelectedSdk);
                }
            }
        });
        Disposer.register(this, connection);

        refreshSdkList(null);
    }

    private void refreshSdkList(Sdk sdkToSelect) {
        final List<Sdk> sdks = new ArrayList<>();
        sdks.add(null);

        final List<Sdk> committedSdks = new ArrayList<>(getPythonSdks());
        committedSdks.sort(new PreferredSdkComparator());
        sdks.addAll(committedSdks);

        //noinspection unchecked
        pythonSdkCB.getComboBox().setModel(new CollectionComboBoxModel(sdks, sdkToSelect));
    }

    @NotNull
    private List<Sdk> getPythonSdks() {
        final PyConfigurableInterpreterList interpreterList = PyConfigurableInterpreterList.getInstance(project);
        return interpreterList.getAllPythonSdks();
    }

    @Nullable
    private Sdk getSelectedSdk() {
        return (Sdk) pythonSdkCB.getComboBox().getSelectedItem();
    }

    private void createUIComponents() {
        // method that is called after form initialization from xml  (reflection)
        // required for custom components initialization

        // Do nothing, no custom components, just use this methods as a remainder that
        // it is API for custom comps initialization

        pythonSdkCB = new PythonSdkChooserCombo(
                project, null, Collections.emptyList(), sdk -> true
        );
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
        final Sdk sdk = getSelectedSdk();
        state.setPythonSdkName(sdk == null ? "" : sdk.getName());
    }

    public void reset(@NotNull SmkSupportProjectSettings.State state) {
        setUIWrappersSrcFolderPath(state.getWrappersCustomSourcesFolder());

        final boolean useBundledWrappersInfo = state.getUseBundledWrappersInfo();
        wrappersBundledRB.setSelected(useBundledWrappersInfo);
        wrappersFromSrcRB.setSelected(!useBundledWrappersInfo);

        final String sdkName = state.getPythonSdkName();
        Sdk sdk = null;
        if (sdkName != null) {
            final List<Sdk> sdks = getPythonSdks();
            for (Sdk pySdk : sdks) {
                if (sdkName.equals(pySdk.getName())) {
                    sdk = pySdk;
                    break;
                }
            }
        }
        pythonSdkCB.getComboBox().setSelectedItem(sdk);

        updateWrappersSrcPanelEnabledPropertyRecursively();
    }

    public void disposeUIResources() {
        dispose();
    }

    private void setUIWrappersSrcFolderPath(final String path) {
        wrappersSrcPathTF.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(path)));
    }

    public void updateWrappersSrcPanelEnabledPropertyRecursively() {
        UIUtil.setEnabled(wrappersSrcPathSettingsPanel, wrappersFromSrcRB.isSelected(), true);
    }

    @Override
    public void dispose() {
        // do nothing
    }
}
