package com.jetbrains.snakecharm.model;

import com.jetbrains.snakecharm.SnakemakeBundle;
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class KeyValuePairTableModel extends AbstractTableModel {
    private final List<SmkSupportProjectSettings.KeyValuePairState> keyValuePairs;
    private final String[] columnNames = {
            SnakemakeBundle.message("smk.framework.configurable.configuration.files.table.column.key"),
            SnakemakeBundle.message("smk.framework.configurable.configuration.files.table.column.value")
    };

    public KeyValuePairTableModel(List<SmkSupportProjectSettings.KeyValuePairState> keyValuePairs) {
        this.keyValuePairs = keyValuePairs;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return keyValuePairs.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SmkSupportProjectSettings.KeyValuePairState pair = keyValuePairs.get(rowIndex);
        if (columnIndex == 0) {
            return pair.getKey();
        } else if (columnIndex == 1) {
            return pair.getValue();
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

        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < columnNames.length;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!(aValue instanceof String)) {
            return;
        }
        SmkSupportProjectSettings.KeyValuePairState path = keyValuePairs.get(rowIndex);
        if (columnIndex == 0) {
            path.setKey((String) aValue);
        } else if (columnIndex == 1) {
            path.setValue((String) aValue);
        }
    }
}