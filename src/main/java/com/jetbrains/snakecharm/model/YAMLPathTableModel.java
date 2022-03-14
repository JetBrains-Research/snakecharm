package com.jetbrains.snakecharm.model;

import com.jetbrains.snakecharm.SnakemakeBundle;
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class YAMLPathTableModel extends AbstractTableModel {
    private final List<SmkSupportProjectSettings.FilePathState> paths;

    public static final String[] columnNames = {
            SnakemakeBundle.message("smk.framework.configurable.configuration.files.table.column.path"),
            SnakemakeBundle.message("smk.framework.configurable.configuration.files.table.column.enabled")
    };

    public YAMLPathTableModel(List<SmkSupportProjectSettings.FilePathState> paths) {
        this.paths = paths;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return paths.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SmkSupportProjectSettings.FilePathState path = paths.get(rowIndex);
        if (columnIndex == 0) {
            return path.getPath();
        } else if (columnIndex == 1) {
            return path.getEnabled();
        }
        return null;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex > columnNames.length) {
            return null;
        }
        return columnNames[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex > columnNames.length) {
            return null;
        }
        if (columnIndex == 0) {
            return String.class;
        }

        return Boolean.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex >= columnNames.length) {
            return false;
        }
        return columnIndex == 1;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        SmkSupportProjectSettings.FilePathState path = paths.get(rowIndex);
        if (columnIndex == 0 && aValue instanceof String) {
            path.setPath((String) aValue);
        } else if (columnIndex == 1 && aValue instanceof Boolean) {
            path.setEnabled((Boolean) aValue);
        }
    }
}