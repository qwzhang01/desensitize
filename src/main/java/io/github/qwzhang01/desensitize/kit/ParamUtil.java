package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.domain.ParameterEncryptInfo;
import io.github.qwzhang01.desensitize.domain.ParameterRestoreInfo;
import io.github.qwzhang01.desensitize.encrypt.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.encrypt.context.SqlRewriteContext;
import io.github.qwzhang01.desensitize.encrypt.shield.EncryptionAlgo;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.sql.tool.helper.ParserHelper;
import io.github.qwzhang01.sql.tool.model.SqlParam;
import io.github.qwzhang01.sql.tool.model.SqlTable;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static io.github.qwzhang01.desensitize.kit.ClazzUtil.setPropertyValue;

/**
 * Parameter analysis utility for SQL encryption.
 *
 * @author avinzhang
 */
public final class ParamUtil {

    private static final Logger log = LoggerFactory.getLogger(ParamUtil.class);

    private ParamUtil() {
        throw new UnsupportedOperationException("ParamUtil is a utility class" +
                " and cannot be instantiated");
    }

    /**
     * Check if parameter is QueryWrapper
     */
    private static boolean isQueryWrapperParameter(Object parameterObject) {
        if (parameterObject instanceof Map<?, ?> paramMap) {
            return paramMap.containsKey("ew") ||
                    paramMap.keySet().stream().anyMatch(key ->
                            key.toString().contains("paramNameValuePairs"));
        }
        return false;
    }


    /**
     * Analyze parameters to determine which need encryption
     */
    public static List<ParameterEncryptInfo> analyzeParameters(List<ParameterMapping> parameterMappings,
                                                               List<SqlParam> params,
                                                               List<SqlTable> tables,
                                                               Object parameterObject) {
        if (parameterObject == null) {
            return Collections.emptyList();
        }

        log.debug("Parameter type: {}, mappings: {}",
                parameterObject.getClass().getSimpleName(),
                parameterMappings.size());

        if (parameterObject instanceof Map paramMap) {
            if (isQueryWrapperParameter(parameterObject)) {
                log.debug("QueryWrapper parameter detected");
                return analyzeQueryWrapperParameters(paramMap, params, tables);
            }
            return analyzeMapParameters(paramMap, parameterMappings, params,
                    tables);
        }
        return analyzeObjectParameters(parameterObject, parameterMappings,
                params, tables);
    }

    /**
     * Analyze QueryWrapper parameters
     */
    private static List<ParameterEncryptInfo> analyzeQueryWrapperParameters(Map<String, Object> paramMap,
                                                                            List<SqlParam> params, List<SqlTable> tables) {
        log.debug("Analyzing QueryWrapper parameters: {}", paramMap.keySet());

        Object wrapper = paramMap.get("ew");
        if (wrapper == null) {
            log.debug("QueryWrapper object not found");
            return Collections.emptyList();
        }

        try {
            Map<String, Object> paramNameValuePairs =
                    getParamNameValuePairs(wrapper);
            if (paramNameValuePairs == null || paramNameValuePairs.isEmpty()) {
                log.debug("No parameters in QueryWrapper");
                return Collections.emptyList();
            }

            String sqlSegment = getSqlSegment(wrapper);
            if (sqlSegment == null || sqlSegment.isEmpty()) {
                log.debug("No SQL segment in QueryWrapper");
                return Collections.emptyList();
            }

            log.debug("QueryWrapper SQL: {}", sqlSegment);
            log.debug("QueryWrapper params: {}", paramNameValuePairs);

            Map<String, String> fieldParamMapping =
                    parseFieldParamMapping(sqlSegment);

            List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
            for (Map.Entry<String, String> entry :
                    fieldParamMapping.entrySet()) {
                String fieldName = entry.getValue();
                String paramName = entry.getKey();
                Object paramValue = paramNameValuePairs.get(paramName);

                if (paramValue instanceof String) {
                    log.debug("Checking QueryWrapper field: {} -> param: {} =" +
                            " {}", fieldName, paramName, paramValue);

                    ParameterEncryptInfo encryptInfo =
                            FieldMatchUtil.matchParameterToTableField(fieldName, (String) paramValue, params, tables);
                    if (encryptInfo != null) {
                        String parameterKey =
                                "ew.paramNameValuePairs." + paramName;
                        encryptInfo.setParameterKey(parameterKey);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(SystemMetaObject.forObject(paramMap));
                        encryptInfo.setQueryWrapperParam(true);
                        encryptInfo.setQueryWrapperParamName(paramName);
                        encryptInfos.add(encryptInfo);
                        log.debug("Added QueryWrapper encryption param: {} ->" +
                                " {}", fieldName, parameterKey);
                    }
                }
            }
            return encryptInfos;
        } catch (Exception e) {
            throw new DesensitizeException("Failed to parse QueryWrapper " +
                    "parameters: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze Map parameters
     */
    private static List<ParameterEncryptInfo> analyzeMapParameters(Map<String
            , Object> paramMap,
                                                                   List<ParameterMapping> parameterMappings,
                                                                   List<SqlParam> params, List<SqlTable> tables) {
        List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
        log.debug("Analyzing Map parameters: {}", paramMap.keySet());

        MetaObject metaObject = SystemMetaObject.forObject(paramMap);
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            Object value = null;

            try {
                if (metaObject.hasGetter(property)) {
                    value = metaObject.getValue(property);
                } else {
                    value = paramMap.get(property);
                }

                if (value instanceof String) {
                    log.debug("Checking parameter: {} = {}", property, value);

                    ParameterEncryptInfo encryptInfo = matchParameterToSqlField(
                            property, (String) value, params, tables,
                            parameterMappings);
                    if (encryptInfo != null) {
                        encryptInfo.setParameterKey(property);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(metaObject);
                        encryptInfos.add(encryptInfo);
                        log.debug("Added encryption parameter: {}", property);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to get parameter value: {}", property, e);
            }
        }

        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();

            if (paramValue instanceof String) {
                boolean alreadyProcessed = parameterMappings.stream()
                        .anyMatch(mapping -> paramName.equals(mapping.getProperty()));

                if (!alreadyProcessed) {
                    ParameterEncryptInfo encryptInfo =
                            FieldMatchUtil.matchParameterToTableField(
                                    paramName, (String) paramValue, params,
                                    tables);
                    if (encryptInfo != null) {
                        encryptInfo.setParameterKey(paramName);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(metaObject);
                        encryptInfos.add(encryptInfo);
                        log.debug("Added additional encryption parameter: {}"
                                , paramName);
                    }
                }
            }
        }

        return encryptInfos;
    }

    /**
     * Analyze object parameters
     */
    private static List<ParameterEncryptInfo> analyzeObjectParameters(Object parameterObject,
                                                                      List<ParameterMapping> parameterMappings,
                                                                      List<SqlParam> sqlAnalysis, List<SqlTable> tables) {
        log.debug("Analyzing object parameters: {}",
                parameterObject.getClass().getSimpleName());

        List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            try {
                Object value = ClazzUtil.getPropertyValue(parameterObject,
                        property);
                if (value instanceof String) {
                    log.debug("Checking object property: {} = {}", property,
                            value);
                    ParameterEncryptInfo encryptInfo =
                            matchParameterToSqlField(property, (String) value
                                    , sqlAnalysis, tables, parameterMappings);
                    if (encryptInfo != null) {
                        encryptInfo.setTargetObject(parameterObject);
                        encryptInfo.setPropertyName(property);
                        encryptInfos.add(encryptInfo);
                        log.debug("Added object encryption property: {}",
                                property);
                    }
                }
            } catch (Exception e) {
                throw new DesensitizeException("Failed to get object property" +
                        " value: " + parameterObject + "." + property, e);
            }
        }
        return encryptInfos;
    }

    /**
     * Map parameter to SQL field
     */
    private static ParameterEncryptInfo matchParameterToSqlField(String paramProperty, String paramValue,
                                                                 List<SqlParam> params, List<SqlTable> tables,
                                                                 List<ParameterMapping> parameterMappings) {
        int paramIndex = findParameterIndex(paramProperty, parameterMappings);

        if (paramIndex >= 0 && paramIndex < params.size()) {
            SqlParam param = params.get(paramIndex);

            for (SqlTable table : tables) {
                ParameterEncryptInfo encryptInfo = FieldMatchUtil
                        .createEncryptInfo(table.getName(), param.getColumn()
                                , paramValue);
                if (encryptInfo != null) {
                    log.debug("Position mapping found encryption field: " +
                                    "param[{}] -> SQL field[{}] -> table[{}] " +
                                    "(index:{})",
                            paramProperty, param.getColumn(), table.getName()
                            , paramIndex);
                    return encryptInfo;
                }
            }
        }
        String fieldName = StringUtil.extractFieldName(paramProperty);
        return FieldMatchUtil.matchParameterToTableField(fieldName,
                paramValue, params, tables);
    }

    /**
     * Find parameter index in mapping list
     */
    private static int findParameterIndex(String paramProperty,
                                          List<ParameterMapping> parameterMappings) {
        for (int i = 0; i < parameterMappings.size(); i++) {
            if (paramProperty.equals(parameterMappings.get(i).getProperty())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get QueryWrapper SQL segment
     */
    private static String getSqlSegment(Object wrapper) {
        try {
            Method getSqlSegmentMethod =
                    ClazzUtil.findMethod(wrapper.getClass(), "getSqlSegment");
            if (getSqlSegmentMethod != null) {
                getSqlSegmentMethod.setAccessible(true);
                Object result = getSqlSegmentMethod.invoke(wrapper);
                return result != null ? result.toString() : null;
            }

            Method getCustomSqlSegmentMethod =
                    ClazzUtil.findMethod(wrapper.getClass(),
                            "getCustomSqlSegment");
            if (getCustomSqlSegmentMethod != null) {
                getCustomSqlSegmentMethod.setAccessible(true);
                Object result = getCustomSqlSegmentMethod.invoke(wrapper);
                return result != null ? result.toString() : null;
            }

            log.debug("Unable to get QueryWrapper SQL segment");
            return null;
        } catch (Exception e) {
            throw new DesensitizeException("Failed to get QueryWrapper SQL " +
                    "segment", e);
        }
    }

    /**
     * Parse field-parameter mapping
     */
    private static Map<String, String> parseFieldParamMapping(String sqlSegment) {

        if (StringUtil.isEmpty(sqlSegment)) {
            return Collections.emptyMap();
        }

        try {
            Map<String, String> mapping = new HashMap<>();
            List<SqlParam> params = ParserHelper.getSpecParam("select * from " +
                    "dumpy_table where " + sqlSegment);
            for (SqlParam param : params) {
                mapping.put("MPGENVAL" + param.getIndex(), param.getColumn());
            }
            return mapping;
        } catch (Exception e) {
            throw new DesensitizeException("Failed to parse field-parameter " +
                    "mapping", e);
        }
    }

    /**
     * Execute parameter encryption
     */
    public static void encryptParameters(List<ParameterEncryptInfo> encryptInfos) {
        List<ParameterRestoreInfo> restoreInfos = new ArrayList<>();

        for (ParameterEncryptInfo encryptInfo : encryptInfos) {
            try {
                EncryptionAlgo algo =
                        SpringContextUtil.getBean(AbstractEncryptAlgoContainer.class).getAlgo(encryptInfo.getAlgoClass());
                String encryptedValue =
                        algo.encrypt(encryptInfo.getOriginalValue());

                ParameterRestoreInfo restoreInfo = new ParameterRestoreInfo();
                restoreInfo.setOriginalValue(encryptInfo.getOriginalValue());
                restoreInfo.setParameterMap(encryptInfo.getParameterMap());
                restoreInfo.setParameterKey(encryptInfo.getParameterKey());
                restoreInfo.setMetaObject(encryptInfo.getMetaObject());
                restoreInfo.setTargetObject(encryptInfo.getTargetObject());
                restoreInfo.setPropertyName(encryptInfo.getPropertyName());
                restoreInfo.setQueryWrapperParam(encryptInfo.isQueryWrapperParam());
                restoreInfo.setQueryWrapperParamName(encryptInfo.getQueryWrapperParamName());

                if (encryptInfo.isQueryWrapperParam()) {
                    updateQueryWrapperParameter(encryptInfo, encryptedValue);
                } else if (encryptInfo.getParameterMap() != null && encryptInfo.getParameterKey() != null) {
                    if (encryptInfo.getMetaObject() != null && encryptInfo.getMetaObject().hasSetter(encryptInfo.getParameterKey())) {
                        encryptInfo.getMetaObject().setValue(encryptInfo.getParameterKey(), encryptedValue);
                        log.debug("Updated nested parameter via MetaObject: " +
                                        "{} = {}",
                                encryptInfo.getParameterKey(),
                                encryptedValue);
                    } else {
                        encryptInfo.getParameterMap().put(encryptInfo.getParameterKey(), encryptedValue);
                        log.debug("Updated Map parameter: {} = {}",
                                encryptInfo.getParameterKey(), encryptedValue);
                    }
                } else if (encryptInfo.getTargetObject() != null && encryptInfo.getPropertyName() != null) {
                    ClazzUtil.setPropertyValue(encryptInfo.getTargetObject(),
                            encryptInfo.getPropertyName(), encryptedValue);
                    log.debug("Updated object property: {} = {}",
                            encryptInfo.getPropertyName(), encryptedValue);
                }

                restoreInfos.add(restoreInfo);

                log.debug("Field {}.{} encryption completed: {} -> {}",
                        encryptInfo.getTableName(), encryptInfo.getFieldName(),
                        encryptInfo.getOriginalValue(), encryptedValue);

            } catch (Exception e) {
                throw new DesensitizeException("Failed to encrypt parameter: "
                        + encryptInfo.getTableName() + "." + encryptInfo.getFieldName(), e);
            }
        }

        SqlRewriteContext.cache(restoreInfos);
    }

    /**
     * Update QueryWrapper parameters
     */
    private static void updateQueryWrapperParameter(ParameterEncryptInfo encryptInfo, String encryptedValue) {
        try {
            Object wrapper = encryptInfo.getParameterMap().get("ew");
            if (wrapper == null) {
                log.error("Unable to get QueryWrapper object");
                return;
            }

            Map<String, Object> paramNameValuePairs =
                    getParamNameValuePairs(wrapper);
            if (paramNameValuePairs == null) {
                log.error("Unable to get QueryWrapper paramNameValuePairs");
                return;
            }

            String paramName = encryptInfo.getQueryWrapperParamName();
            paramNameValuePairs.put(paramName, encryptedValue);

            log.debug("Updated QueryWrapper parameter: {} = {}", paramName,
                    encryptedValue);

            if (encryptInfo.getMetaObject() != null && encryptInfo.getMetaObject().hasSetter(encryptInfo.getParameterKey())) {
                encryptInfo.getMetaObject().setValue(encryptInfo.getParameterKey(), encryptedValue);
                log.debug("Synced QueryWrapper parameter via MetaObject: {}",
                        encryptInfo.getParameterKey());
            }

        } catch (Exception e) {
            throw new DesensitizeException("Failed to update QueryWrapper " +
                    "parameter: " + encryptInfo.getTableName() + "." + encryptInfo.getFieldName(), e);
        }
    }

    /**
     * Get QueryWrapper paramNameValuePairs
     */
    private static Map<String, Object> getParamNameValuePairs(Object wrapper) {
        try {
            Field paramField = ClazzUtil.findField(wrapper.getClass(),
                    "paramNameValuePairs");
            if (paramField != null) {
                paramField.setAccessible(true);
                return (Map<String, Object>) paramField.get(wrapper);
            }

            Method getterMethod = ClazzUtil.findMethod(wrapper.getClass(),
                    "getParamNameValuePairs");
            if (getterMethod != null) {
                getterMethod.setAccessible(true);
                return (Map<String, Object>) getterMethod.invoke(wrapper);
            }

            log.debug("Unable to get QueryWrapper paramNameValuePairs");
            return null;
        } catch (Exception e) {
            throw new DesensitizeException("Failed to get QueryWrapper " +
                    "paramNameValuePairs", e);
        }
    }

    /**
     * Restore original values
     */
    public static void restoreOriginalValues(List<ParameterRestoreInfo> restoreInfos) {
        if (restoreInfos == null || restoreInfos.isEmpty()) {
            return;
        }

        log.debug("Starting to restore parameters, total: {}",
                restoreInfos.size());

        for (ParameterRestoreInfo restoreInfo : restoreInfos) {
            try {
                if (restoreInfo.isQueryWrapperParam()) {
                    restoreQueryWrapperParameter(restoreInfo);
                } else if (restoreInfo.getParameterMap() != null && restoreInfo.getParameterKey() != null) {
                    if (restoreInfo.getMetaObject() != null && restoreInfo.getMetaObject().hasSetter(restoreInfo.getParameterKey())) {
                        restoreInfo.getMetaObject().setValue(restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                        log.debug("Restored nested parameter via MetaObject: " +
                                        "{} = {}",
                                restoreInfo.getParameterKey(),
                                restoreInfo.getOriginalValue());
                    } else {
                        restoreInfo.getParameterMap().put(restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                        log.debug("Restored Map parameter: {} = {}",
                                restoreInfo.getParameterKey(),
                                restoreInfo.getOriginalValue());
                    }
                } else if (restoreInfo.getTargetObject() != null && restoreInfo.getPropertyName() != null) {
                    setPropertyValue(restoreInfo.getTargetObject(),
                            restoreInfo.getPropertyName(),
                            restoreInfo.getOriginalValue());
                    log.debug("Restored object property: {} = {}",
                            restoreInfo.getPropertyName(),
                            restoreInfo.getOriginalValue());
                }
                log.debug("Parameter restoration completed: {}",
                        restoreInfo.getOriginalValue());
            } catch (Exception e) {
                throw new DesensitizeException("Failed to restore parameter",
                        e);
            }
        }
        log.debug("Parameter restoration completed");
    }

    /**
     * Restore QueryWrapper parameters
     */
    private static void restoreQueryWrapperParameter(ParameterRestoreInfo restoreInfo) {
        try {
            Object wrapper = restoreInfo.getParameterMap().get("ew");
            if (wrapper == null) {
                log.error("Unable to get QueryWrapper object for restoration");
                return;
            }

            Map<String, Object> paramNameValuePairs =
                    getParamNameValuePairs(wrapper);
            if (paramNameValuePairs == null) {
                log.error("Unable to get QueryWrapper paramNameValuePairs for" +
                        " restoration");
                return;
            }

            String paramName = restoreInfo.getQueryWrapperParamName();
            paramNameValuePairs.put(paramName, restoreInfo.getOriginalValue());

            log.debug("Restored QueryWrapper parameter: {} = {}", paramName,
                    restoreInfo.getOriginalValue());

            if (restoreInfo.getMetaObject() != null && restoreInfo.getMetaObject().hasSetter(restoreInfo.getParameterKey())) {
                restoreInfo.getMetaObject().setValue(restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                log.debug("Synced QueryWrapper parameter restoration via " +
                        "MetaObject: {}", restoreInfo.getParameterKey());
            }

        } catch (Exception e) {
            throw new DesensitizeException("Failed to restore QueryWrapper " +
                    "parameter", e);
        }
    }
}
