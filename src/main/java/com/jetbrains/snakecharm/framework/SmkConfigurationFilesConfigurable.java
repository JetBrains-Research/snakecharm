package com.jetbrains.snakecharm.framework;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.jetbrains.snakecharm.SnakemakeBundle;
import com.jetbrains.snakecharm.model.KeyValuePairTableModel;
import com.jetbrains.snakecharm.model.YAMLPathTableModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SmkConfigurationFilesConfigurable implements SearchableConfigurable {
    @NotNull
    private final Project project;

    private JPanel mainPanel;
    private JPanel pairsPanel;
    private JPanel configurationFilesTable;
    private JBTable yamlFilesTable;
    private JBTable keyValuePairsTable;

    private final List<SmkSupportProjectSettings.FilePathState> yamlFilesPaths = new ArrayList<>();
    private final List<SmkSupportProjectSettings.KeyValuePairState> keyValuePairs = new ArrayList<>();

    private FileChooserDescriptor filesChooserDescriptor;

    public SmkConfigurationFilesConfigurable(@NotNull final Project project) {
        this.project = project;
        initJPanel();
    }

    private void initJPanel() {
        TableModel yamlFileTableModel = new YAMLPathTableModel(yamlFilesPaths);
        TableModel keyValuePairsTableModel = new KeyValuePairTableModel(keyValuePairs);

        yamlFilesTable = new JBTable(yamlFileTableModel);
        yamlFilesTable.setShowGrid(true);
        int enabledColumnWidth = 15 + yamlFilesTable.getTableHeader().getFontMetrics(yamlFilesTable.getTableHeader().getFont()).stringWidth(YAMLPathTableModel.columnNames[1]);
        TableColumn enabledColumn = yamlFilesTable.getColumn(YAMLPathTableModel.columnNames[1]);
        enabledColumn.setMaxWidth(enabledColumnWidth);

        keyValuePairsTable = new JBTable(keyValuePairsTableModel);
        keyValuePairsTable.setShowGrid(true);

        filesChooserDescriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor();
        filesChooserDescriptor.setTitle(SnakemakeBundle.message("smk.framework.configurable.panel.configuration.files.file.chooser"));
        filesChooserDescriptor.withFileFilter(file -> {
            String path = file.getCanonicalPath();
            return path != null && (path.endsWith(".yaml") || path.endsWith(".yml"));
        });

        var yamlFilesPanel = ToolbarDecorator.createDecorator(yamlFilesTable)
                .setAddAction(anActionButton -> addFilePath())
                .setRemoveAction(anActionButton -> removeSelectedElement(yamlFilesTable, yamlFilesPaths)).createPanel();
        var keyValuePAirsPanel = ToolbarDecorator.createDecorator(keyValuePairsTable)
                .setAddAction(anActionButton -> addEmptyPair())
                .setRemoveAction(anActionButton -> removeSelectedElement(keyValuePairsTable, keyValuePairs)).createPanel();

        configurationFilesTable.add(yamlFilesPanel, BorderLayout.CENTER);
        pairsPanel.add(keyValuePAirsPanel, BorderLayout.CENTER);
    }

    private void removeSelectedElement(JBTable table, List<?> contentList) {
        int rowIndex = table.getSelectedRow();
        if (rowIndex == -1 || rowIndex >= contentList.size()) {
            return;
        }
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
        AbstractTableModel model = (AbstractTableModel) table.getModel();
        contentList.remove(rowIndex);
        model.fireTableRowsDeleted(rowIndex, rowIndex);

        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(table, true));
    }

    private void addEmptyPair() {
        keyValuePairs.add(new SmkSupportProjectSettings.KeyValuePairState("", ""));
        ((AbstractTableModel) keyValuePairsTable.getModel()).fireTableRowsInserted(keyValuePairs.size() - 1, keyValuePairs.size() - 1);
    }

    private void addFilePath() {
        VirtualFile[] files = FileChooser.chooseFiles(filesChooserDescriptor, project, null);
        for (final VirtualFile chosenFile : files) {
            String path = chosenFile.getCanonicalPath();
            if (path != null && yamlFilesPaths.stream().noneMatch(it -> path.equals(it.getPath()))) {
                yamlFilesPaths.add(new SmkSupportProjectSettings.FilePathState(path, true));
            }
        }
        ((AbstractTableModel) yamlFilesTable.getModel()).fireTableRowsInserted(yamlFilesPaths.size() - 1, yamlFilesPaths.size() - 1);
    }

    @Override
    public @Nullable
    JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public @NotNull
    @NonNls
    String getId() {
        return "Settings.Snakemake.Configuration_file";
    }

    @Override
    public @Nls
    String getDisplayName() {
        return SnakemakeBundle.message("smk.framework.configurable.configuration.files.display.name");
    }

    @Override
    public boolean isModified() {
        final SmkSupportProjectSettings settings = SmkSupportProjectSettings.Companion.getInstance(project);
        return !getUIState().equals(settings.stateSnapshot());
    }

    @NotNull
    private SmkSupportProjectSettings.State getUIState() {
        SmkSupportProjectSettings.State state = SmkSupportProjectSettings.Companion.getInstance(project).stateSnapshot();
        state.setConfigurationFiles(yamlFilesPaths);
        state.setExplicitlyDefinedKeyValuePairs(keyValuePairs.stream().distinct().collect(Collectors.toList()));
        return state;
    }

    @Override
    public void apply() throws ConfigurationException {
        SmkFrameworkConfigurable.applyUIStateToProject(getUIState(), project);
    }

    @Override
    public void reset() {
        final SmkSupportProjectSettings conf = SmkSupportProjectSettings.Companion.getInstance(project);
        // Here we create a new instances for FilePathState and KeyValuePairState
        // Because we want to know, when settings state was changed
        // Otherwise, if we use provided objects
        // Old settings state and the new one will be the same
        yamlFilesPaths.clear();
        yamlFilesPaths.addAll(conf.getConfigurationFiles().stream()
                .map(it -> new SmkSupportProjectSettings.FilePathState(Objects.requireNonNull(it.getPath()), it.getEnabled())).collect(Collectors.toList()));
        ((AbstractTableModel) yamlFilesTable.getModel()).fireTableRowsInserted(0, yamlFilesPaths.size() - 1);
        keyValuePairs.clear();
        keyValuePairs.addAll(conf.getExplicitlyDefinedKeyValuePairs().stream()
                .map(it -> new SmkSupportProjectSettings.KeyValuePairState(Objects.requireNonNull(it.getKey()), Objects.requireNonNull(it.getValue()))).collect(Collectors.toList()));
        ((AbstractTableModel) keyValuePairsTable.getModel()).fireTableRowsInserted(0, keyValuePairs.size() - 1);
    }
}