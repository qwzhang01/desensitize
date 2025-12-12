package io.github.qwzhang01.desensitize.encrypt.container;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.qwzhang01.desensitize.encrypt.annotation.EncryptField;
import io.github.qwzhang01.desensitize.encrypt.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.encrypt.shield.EncryptionAlgo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.qwzhang01.desensitize.kit.StringUtil.clearSqlTip;

/**
 * @author avinzhang
 */
public class EncryptFieldTableContainer {

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
                                String.format("%s:%s",
                                        clearSqlTip(encryptColumn.getTable()), clearSqlTip(encryptColumn.getName())),
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
        return ENCRYPT_COLUMNS.containsKey(clearSqlTip(tableName) + ":" + clearSqlTip(columnName));
    }

    public boolean hasEncrypt() {
        if (!init) {
            init();
        }
        return !ENCRYPT_COLUMNS.isEmpty();
    }

    public Class<? extends EncryptionAlgo> getAlgo(String tableName, String columnName) {
        if (!init) {
            init();
        }
        EncryptColumn column = ENCRYPT_COLUMNS.get(clearSqlTip(tableName) + ":" + clearSqlTip(columnName));
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class<? extends EncryptionAlgo> getAlgo() {
            return algo;
        }

        public void setAlgo(Class<? extends EncryptionAlgo> algo) {
            this.algo = algo;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }
    }
}
