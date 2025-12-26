package io.github.qwzhang01.desensitize.encrypt.context;

import io.github.qwzhang01.desensitize.domain.ParameterRestoreInfo;
import io.github.qwzhang01.desensitize.kit.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SQL rewrite context for parameter restoration.
 *
 * <p>This class provides thread-local storage for parameter restoration
 * information, ensuring that encrypted parameters are restored to their
 * original values after SQL execution.</p>
 *
 * <p><strong>Workflow:</strong></p>
 * <ol>
 *   <li>Parameters are encrypted before SQL execution</li>
 *   <li>Restoration info is cached in ThreadLocal</li>
 *   <li>SQL is executed with encrypted values</li>
 *   <li>Parameters are restored to original values</li>
 *   <li>ThreadLocal is cleared</li>
 * </ol>
 *
 * @author avinzhang
 */
public class SqlRewriteContext {
    private static final Logger log =
            LoggerFactory.getLogger(SqlRewriteContext.class);
    // ThreadLocal for storing original parameter values (ensures thread safety)
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
