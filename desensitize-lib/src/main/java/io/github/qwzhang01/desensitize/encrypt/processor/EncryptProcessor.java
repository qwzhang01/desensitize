package io.github.qwzhang01.desensitize.encrypt.processor;

import io.github.qwzhang01.desensitize.domain.ParameterEncryptInfo;
import io.github.qwzhang01.desensitize.encrypt.container.EncryptFieldTableContainer;
import io.github.qwzhang01.desensitize.kit.ParamUtil;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
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
 * 解密处理器
 *
 * @author avinzhang
 */
public class EncryptProcessor {
    private static final Logger log = LoggerFactory.getLogger(EncryptProcessor.class);

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
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // 获取 ParameterHandler 中的参数对象
        Object parameterObject = statementHandler.getParameterHandler().getParameterObject();
        BoundSql boundSql = statementHandler.getBoundSql();

        encryptParameters(boundSql, parameterObject);
    }

    private void encryptParameters(BoundSql boundSql, Object parameterObject) {
        try {
            EncryptFieldTableContainer container = SpringContextUtil.getBean(EncryptFieldTableContainer.class);
            if (!container.hasEncrypt()) {
                // 没有注解加密字段无需走这个拦截器
                // return;
            }

            String originalSql = boundSql.getSql();
            log.debug("开始处理查询加密，SQL: {}", originalSql);

            if (boundSql.getParameterObject() == null) {
                log.debug("参数对象为空，跳过加密处理");
                return;
            }

            // 1. 解析 SQL 获取所有涉及的表信息
            List<SqlTable> tables = null;
            List<SqlParam> param = null;
            try {
                tables = ParserHelper.getTables(originalSql);
                param = ParserHelper.getParam(originalSql);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (param == null || param.isEmpty() || tables.isEmpty()) {
                log.debug("未找到表信息，跳过加密处理");
                return;
            }

            // 2. 解析参数对象，获取需要加密的参数
            List<ParameterEncryptInfo> encryptInfos = ParamUtil.analyzeParameters(
                    boundSql.getParameterMappings(), param, tables, parameterObject);

            // 3. 执行参数加密
            if (!encryptInfos.isEmpty()) {
                ParamUtil.encryptParameters(encryptInfos);
                log.debug("完成参数加密，共处理 {} 个参数", encryptInfos.size());
            }
        } catch (Exception e) {
            log.error("查询参数加密处理失败", e);
        }
    }

    private static final class Holder {
        private static final EncryptProcessor INSTANCE = new EncryptProcessor();
    }
}
