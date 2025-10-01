package io.github.qwzhang01.desensitize.table;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author avinzhang
 */
public class TableContainer {

    private static final Map<String, EncryptColumn> ENCRYPT_COLUMNS = new ConcurrentHashMap<>();
    private boolean init = false;

    public void init() {

        if (init) {
            return;
        }

        synchronized (this) {
            if (init) {
                return;
            }
            List<TableInfo> tableInfos = TableInfoHelper.getTableInfos();
            tableInfos.forEach(t -> {
                List<TableFieldInfo> fieldList = t.getFieldList();
                for (TableFieldInfo fieldInfo : fieldList) {
                    EncryptField encryptField = fieldInfo.getField().getAnnotation(EncryptField.class);
                    if (encryptField != null) {
                        EncryptColumn encryptColumn = getEncryptColumn(t, fieldInfo, encryptField);
                        ENCRYPT_COLUMNS.put(
                                encryptColumn.getTable() + ":" + encryptColumn.getName(),
                                encryptColumn);
                    }
                }
            });

            init = true;
        }
    }

    private EncryptColumn getEncryptColumn(TableInfo tableInfo, TableFieldInfo fieldInfo, EncryptField encryptField) {
        EncryptColumn encryptColumn = new EncryptColumn();
        encryptColumn.setTable(tableInfo.getTableName());
        encryptColumn.setAlgo(encryptField.value());
        TableField tableField = fieldInfo.getField().getAnnotation(TableField.class);
        if (tableField != null) {
            encryptColumn.setName(tableField.value());
        } else {
            encryptColumn.setName(fieldInfo.getField().getName());
        }
        return encryptColumn;
    }

    public boolean isEncrypt(String tableName, String columnName) {
        if (!init) {
            init();
        }
        return ENCRYPT_COLUMNS.containsKey(tableName + ":" + columnName);
    }

    public Class<? extends EncryptionAlgo> getAlgo(String tableName, String columnName) {
        if (!init) {
            init();
        }
        EncryptColumn column = ENCRYPT_COLUMNS.get(tableName + ":" + columnName);
        if (column == null) {
            return DefaultEncryptionAlgo.class;
        }
        Class<? extends EncryptionAlgo> algo = column.getAlgo();
        if (algo == null) {
            return DefaultEncryptionAlgo.class;
        }
        return algo;
    }

    /**
     * Encrypt column information
     */
    private static final class EncryptColumn {
        private String name;
        private String table;
        private Class<? extends EncryptionAlgo> algo;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setAlgo(Class<? extends EncryptionAlgo> algo) {
            this.algo = algo;
        }

        public Class<? extends EncryptionAlgo> getAlgo() {
            return algo;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }
    }
}
