package io.github.qwzhang01.desensitize.context;

import io.github.qwzhang01.desensitize.domain.ParameterRestoreInfo;
import io.github.qwzhang01.desensitize.kit.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SQL重写上下文
 *
 * @author avinzhang
 */
public class SqlRewriteContext {
    private static final Logger log = LoggerFactory.getLogger(SqlRewriteContext.class);
    // 用于保存原始值的 ThreadLocal，确保线程安全
    private static final ThreadLocal<List<ParameterRestoreInfo>> RESTORE_INFO_HOLDER = new ThreadLocal<>();

    public static void cache(List<ParameterRestoreInfo> restoreInfos) {
        if (restoreInfos != null && !restoreInfos.isEmpty()) {
            RESTORE_INFO_HOLDER.set(restoreInfos);
        }
    }

    public static void restore() {
        ParamUtil.restoreOriginalValues(RESTORE_INFO_HOLDER.get());
        clear();
    }

    public static void clear() {
        RESTORE_INFO_HOLDER.remove();
    }
}
