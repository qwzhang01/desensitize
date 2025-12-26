package io.github.qwzhang01.dsecurity.encrypt.processor;

import io.github.qwzhang01.dsecurity.domain.ParameterEncryptInfo;
import io.github.qwzhang01.dsecurity.encrypt.container.EncryptFieldTableContainer;
import io.github.qwzhang01.dsecurity.kit.ParamUtil;
import io.github.qwzhang01.dsecurity.kit.SpringContextUtil;
import io.github.qwzhang01.sql.tool.helper.ParserHelper;
import io.github.qwzhang01.sql.tool.model.SqlParam;
import io.github.qwzhang01.sql.tool.model.SqlTable;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Encryption processor for automatic parameter encryption.
 *
 * <p>This processor handles automatic encryption of query parameters before
 * SQL execution. It parses SQL statements, identifies encrypted fields, and
 * applies appropriate encryption algorithms.</p>
 *
 * <p><strong>Process Flow:</strong></p>
 * <ol>
 *   <li>Parse SQL to identify tables and parameters</li>
 *   <li>Match parameters to encrypted fields</li>
 *   <li>Encrypt matched parameters</li>
 *   <li>Save restoration info for later parameter recovery</li>
 * </ol>
 *
 * @author avinzhang
 */
public class EncryptProcessor {
    private static final Logger log =
            LoggerFactory.getLogger(EncryptProcessor.class);

    private EncryptProcessor() {
    }

    public static EncryptProcessor getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Encrypts query parameters for encrypted fields.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Parses the SQL to identify tables and parameters</li>
     *   <li>Analyzes parameter objects to find encrypted fields</li>
     *   <li>Encrypts the parameters using configured algorithms</li>
     *   <li>Saves restoration info to ThreadLocal for later recovery</li>
     * </ol>
     *
     * @param invocation the method invocation containing SQL and parameters
     */
    public void encryptParameters(Invocation invocation) {
        StatementHandler statementHandler =
                (StatementHandler) invocation.getTarget();
        // 获取 ParameterHandler 中的参数对象
        Object parameterObject =
                statementHandler.getParameterHandler().getParameterObject();
        BoundSql boundSql = statementHandler.getBoundSql();

        encryptParameters(boundSql, parameterObject);
    }

    private void encryptParameters(BoundSql boundSql, Object parameterObject) {
        try {
            EncryptFieldTableContainer container =
                    SpringContextUtil.getBean(EncryptFieldTableContainer.class);
            if (!container.hasEncrypt()) {
                // No encrypted fields, skip this interceptor
                // return;
            }

            String originalSql = boundSql.getSql();
            log.debug("Starting query encryption processing, SQL: {}", originalSql);

            if (boundSql.getParameterObject() == null) {
                log.debug("Parameter object is null, skipping encryption");
                return;
            }

            // 1. Parse SQL to get all involved table information
            List<SqlTable> tables = null;
            List<SqlParam> param = null;
            try {
                tables = ParserHelper.getTables(originalSql);
                param = ParserHelper.getParam(originalSql);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (param == null || param.isEmpty() || tables.isEmpty()) {
                log.debug("No table information found, skipping encryption");
                return;
            }

            // 2. Parse parameter object to get parameters that need encryption
            List<ParameterEncryptInfo> encryptInfos =
                    ParamUtil.analyzeParameters(
                            boundSql.getParameterMappings(), param, tables,
                            parameterObject);

            // 3. Execute parameter encryption
            if (!encryptInfos.isEmpty()) {
                ParamUtil.encryptParameters(encryptInfos);
                log.debug("Completed parameter encryption, processed {} parameters", encryptInfos.size());
            }
        } catch (Exception e) {
            log.error("Query parameter encryption processing failed", e);
        }
    }

    private static final class Holder {
        private static final EncryptProcessor INSTANCE = new EncryptProcessor();
    }
}
