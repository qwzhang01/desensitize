package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.context.SqlRewriteContext;
import io.github.qwzhang01.desensitize.domain.ParameterEncryptInfo;
import io.github.qwzhang01.desensitize.domain.ParameterRestoreInfo;
import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;

import static io.github.qwzhang01.desensitize.kit.ClazzUtil.setPropertyValue;
import static io.github.qwzhang01.desensitize.kit.RegexPatterns.*;

/**
 * 参数解析工具
 *
 * @author avinzhang
 */
public final class ParamUtil {

    private static final Logger log = LoggerFactory.getLogger(ParamUtil.class);

    private ParamUtil() {
        // 工具类不允许实例化
    }

    /**
     * 检测是否为 QueryWrapper 参数
     */
    private static boolean isQueryWrapperParameter(Object parameterObject) {
        if (parameterObject instanceof Map<?, ?>) {
            Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
            // 检查是否包含 QueryWrapper 的特征参数
            return paramMap.containsKey("ew") ||
                    paramMap.keySet().stream().anyMatch(key ->
                            key.toString().contains("paramNameValuePairs"));
        }
        return false;
    }


    /**
     * 分析参数对象，确定需要加密的参数
     *
     * @param boundSql        SQL绑定对象
     * @param sqlAnalysis     SQL分析信息
     * @param parameterObject 参数对象
     * @return 需要加密的参数列表
     */
    public static List<ParameterEncryptInfo> analyzeParameters(BoundSql boundSql,
                                                               SqlAnalysisInfo sqlAnalysis,
                                                               Object parameterObject) {
        if (parameterObject == null) {
            return Collections.emptyList();
        }

        log.debug("参数对象类型: {}, 参数映射数量: {}",
                parameterObject.getClass().getSimpleName(), boundSql.getParameterMappings().size());

        if (parameterObject instanceof Map) {
            Map<String, Object> paramMap = (Map<String, Object>) parameterObject;
            // 检查是否为 QueryWrapper 参数
            if (isQueryWrapperParameter(parameterObject)) {
                log.debug("检测到 QueryWrapper 参数，使用专门的解析逻辑");
                return analyzeQueryWrapperParameters(paramMap, sqlAnalysis);
            }
            // 处理普通 Map 参数（MyBatis 常见场景）
            return analyzeMapParameters(paramMap, boundSql.getParameterMappings(), sqlAnalysis);
        }
        // 处理实体对象参数
        return analyzeObjectParameters(parameterObject, boundSql.getParameterMappings(), sqlAnalysis);
    }

    /**
     * 分析 QueryWrapper 类型的参数
     */
    private static List<ParameterEncryptInfo> analyzeQueryWrapperParameters(Map<String, Object> paramMap,
                                                                            SqlAnalysisInfo sqlAnalysis) {
        log.debug("分析 QueryWrapper 参数: {}", paramMap.keySet());

        // 获取 QueryWrapper 对象
        Object wrapper = paramMap.get("ew");
        if (wrapper == null) {
            log.debug("未找到 QueryWrapper 对象 (ew)");
            return Collections.emptyList();
        }

        try {
            // 获取参数值映射
            Map<String, Object> paramNameValuePairs = getParamNameValuePairs(wrapper);
            if (paramNameValuePairs == null || paramNameValuePairs.isEmpty()) {
                log.debug("QueryWrapper 中没有参数");
                return Collections.emptyList();
            }

            // 获取 SQL 片段
            String sqlSegment = getSqlSegment(wrapper);
            if (sqlSegment == null || sqlSegment.isEmpty()) {
                log.debug("QueryWrapper 中没有 SQL 片段");
                return Collections.emptyList();
            }

            log.debug("QueryWrapper SQL 片段: {}", sqlSegment);
            log.debug("QueryWrapper 参数: {}", paramNameValuePairs);

            // 解析字段与参数的映射关系
            Map<String, String> fieldParamMapping = parseFieldParamMapping(sqlSegment);

            // 处理每个参数
            List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
            for (Map.Entry<String, String> entry : fieldParamMapping.entrySet()) {
                String fieldName = entry.getKey();
                String paramName = entry.getValue();
                Object paramValue = paramNameValuePairs.get(paramName);

                if (paramValue instanceof String) {
                    log.debug("检查 QueryWrapper 字段: {} -> 参数: {} = {}", fieldName, paramName, paramValue);

                    ParameterEncryptInfo encryptInfo = FieldMatchUtil.matchParameterToTableField(fieldName, (String) paramValue, sqlAnalysis);
                    if (encryptInfo != null) {
                        // 设置 QueryWrapper 特有的参数路径
                        String parameterKey = "ew.paramNameValuePairs." + paramName;
                        encryptInfo.setParameterKey(parameterKey);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(SystemMetaObject.forObject(paramMap));
                        encryptInfo.setQueryWrapperParam(true);
                        encryptInfo.setQueryWrapperParamName(paramName);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加 QueryWrapper 加密参数: {} -> {}", fieldName, parameterKey);
                    }
                }
            }
            return encryptInfos;
        } catch (Exception e) {
            throw new DesensitizeException("解析 QueryWrapper 参数失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分析 Map 类型的参数
     */
    private static List<ParameterEncryptInfo> analyzeMapParameters(Map<String, Object> paramMap,
                                                                   List<ParameterMapping> parameterMappings,
                                                                   SqlAnalysisInfo sqlAnalysis) {
        List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
        log.debug("分析 Map 参数: {}", paramMap.keySet());

        // 创建 MetaObject 来处理嵌套属性
        MetaObject metaObject = SystemMetaObject.forObject(paramMap);

        // 方法1：通过 ParameterMapping 分析
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            Object value = null;

            try {
                // 使用 MetaObject 获取嵌套属性值，支持 userParam.phoneNo 这样的路径
                if (metaObject.hasGetter(property)) {
                    value = metaObject.getValue(property);
                } else {
                    // 回退到直接从 Map 获取
                    value = paramMap.get(property);
                }

                if (value instanceof String) {
                    log.debug("检查参数: {} = {}", property, value);

                    // 尝试将参数映射到SQL字段
                    ParameterEncryptInfo encryptInfo = matchParameterToSqlField(
                            property, (String) value, sqlAnalysis, parameterMappings);
                    if (encryptInfo != null) {
                        encryptInfo.setParameterKey(property);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(metaObject);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加加密参数: {}", property);
                    }
                }
            } catch (Exception e) {
                log.debug("获取参数值失败: {}", property, e);
            }
        }

        // 方法2：直接遍历 Map 中的参数
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();

            if (paramValue instanceof String) {
                // 跳过已经通过 ParameterMapping 处理的参数
                boolean alreadyProcessed = parameterMappings.stream()
                        .anyMatch(mapping -> paramName.equals(mapping.getProperty()));

                if (!alreadyProcessed) {
                    ParameterEncryptInfo encryptInfo = FieldMatchUtil.matchParameterToTableField(
                            paramName, (String) paramValue, sqlAnalysis);
                    if (encryptInfo != null) {
                        encryptInfo.setParameterKey(paramName);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(metaObject);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加额外加密参数: {}", paramName);
                    }
                }
            }
        }

        return encryptInfos;
    }

    /**
     * 分析对象参数
     */
    private static List<ParameterEncryptInfo> analyzeObjectParameters(Object parameterObject,
                                                                      List<ParameterMapping> parameterMappings,
                                                                      SqlAnalysisInfo sqlAnalysis) {
        log.debug("分析对象参数: {}", parameterObject.getClass().getSimpleName());

        List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
        for (ParameterMapping mapping : parameterMappings) {
            // 通过 ParameterMapping 获取需要处理的属性
            String property = mapping.getProperty();
            try {
                Object value = ClazzUtil.getPropertyValue(parameterObject, property);
                if (value instanceof String) {
                    log.debug("检查对象属性: {} = {}", property, value);
                    ParameterEncryptInfo encryptInfo = matchParameterToSqlField(property, (String) value, sqlAnalysis, parameterMappings);
                    if (encryptInfo != null) {
                        encryptInfo.setTargetObject(parameterObject);
                        encryptInfo.setPropertyName(property);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加对象加密属性: {}", property);
                    }
                }
            } catch (Exception e) {
                throw new DesensitizeException("获取对象属性值失败: " + parameterObject + "." + property, e);
            }
        }
        return encryptInfos;
    }

    /**
     * 将参数映射到SQL字段（考虑参数在SQL中的实际位置）
     */
    private static ParameterEncryptInfo matchParameterToSqlField(String paramProperty, String paramValue,
                                                                 SqlAnalysisInfo sqlAnalysis,
                                                                 List<ParameterMapping> parameterMappings) {
        // 1. 首先尝试通过参数在SQL中的位置来确定对应的字段
        int paramIndex = findParameterIndex(paramProperty, parameterMappings);
        List<SqlAnalysisInfo.FieldCondition> allFields = sqlAnalysis.getAllFields();

        if (paramIndex >= 0 && paramIndex < allFields.size()) {
            // 根据参数在SQL中的位置，找到对应的字段条件
            SqlAnalysisInfo.FieldCondition condition = allFields.get(paramIndex);
            String sqlFieldName = condition.columnName();

            // 检查这个字段是否需要加密
            for (SqlAnalysisInfo.TableInfo tableInfo : sqlAnalysis.getTables()) {
                String tableName = tableInfo.tableName();
                ParameterEncryptInfo encryptInfo = FieldMatchUtil.createEncryptInfo(tableName, sqlFieldName, paramValue);
                if (encryptInfo != null) {
                    log.debug("通过位置映射找到加密字段: 参数[{}] -> SQL字段[{}] -> 表[{}] (索引:{})",
                            paramProperty, sqlFieldName, tableName, paramIndex);
                    return encryptInfo;
                }
            }
        }

        // 2. 如果位置映射失败，回退到原有的名称匹配逻辑
        String fieldName = StringUtil.extractFieldName(paramProperty);
        return FieldMatchUtil.matchParameterToTableField(fieldName, paramValue, sqlAnalysis);
    }

    /**
     * 查找参数在参数映射列表中的索引位置
     */
    private static int findParameterIndex(String paramProperty, List<ParameterMapping> parameterMappings) {
        for (int i = 0; i < parameterMappings.size(); i++) {
            if (paramProperty.equals(parameterMappings.get(i).getProperty())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取 QueryWrapper 的 SQL 片段
     */
    private static String getSqlSegment(Object wrapper) {
        try {
            // 尝试通过 getSqlSegment 方法
            Method getSqlSegmentMethod = ClazzUtil.findMethod(wrapper.getClass(), "getSqlSegment");
            if (getSqlSegmentMethod != null) {
                getSqlSegmentMethod.setAccessible(true);
                Object result = getSqlSegmentMethod.invoke(wrapper);
                return result != null ? result.toString() : null;
            }

            // 尝试通过 getCustomSqlSegment 方法
            Method getCustomSqlSegmentMethod = ClazzUtil.findMethod(wrapper.getClass(), "getCustomSqlSegment");
            if (getCustomSqlSegmentMethod != null) {
                getCustomSqlSegmentMethod.setAccessible(true);
                Object result = getCustomSqlSegmentMethod.invoke(wrapper);
                return result != null ? result.toString() : null;
            }

            log.debug("无法获取 QueryWrapper 的 SQL 片段");
            return null;
        } catch (Exception e) {
            throw new DesensitizeException("获取 QueryWrapper SQL 片段失败", e);
        }
    }

    /**
     * 解析字段与参数的映射关系
     */
    private static Map<String, String> parseFieldParamMapping(String sqlSegment) {
        Map<String, String> mapping = new HashMap<>();

        if (StringUtil.isEmpty(sqlSegment)) {
            return mapping;
        }

        try {
            // 使用统一的正则表达式模式
            parseFieldMapping(sqlSegment, QW_EQUAL_PATTERN, mapping, "等值");
            parseFieldMapping(sqlSegment, QW_LIKE_PATTERN, mapping, "LIKE");
            parseFieldMapping(sqlSegment, QW_IN_PATTERN, mapping, "IN");

        } catch (Exception e) {
            throw new DesensitizeException("解析字段参数映射失败", e);
        }
        return mapping;
    }

    /**
     * 解析特定模式的字段映射
     */
    private static void parseFieldMapping(String sqlSegment, java.util.regex.Pattern pattern,
                                          Map<String, String> mapping, String patternType) {
        Matcher matcher = pattern.matcher(sqlSegment);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String paramName = matcher.group(2);
            mapping.put(fieldName, paramName);
            log.debug("解析到 {} 字段映射: {} -> {}", patternType, fieldName, paramName);
        }
    }

    /**
     * 执行参数加密
     */
    public static void encryptParameters(List<ParameterEncryptInfo> encryptInfos) {
        List<ParameterRestoreInfo> restoreInfos = new ArrayList<>();

        for (ParameterEncryptInfo encryptInfo : encryptInfos) {
            try {
                EncryptionAlgo algo = SpringContextUtil.getBean(AbstractEncryptAlgoContainer.class).getAlgo(encryptInfo.getAlgoClass());
                String encryptedValue = algo.encrypt(encryptInfo.getOriginalValue());

                // 创建还原信息
                ParameterRestoreInfo restoreInfo = new ParameterRestoreInfo();
                restoreInfo.setOriginalValue(encryptInfo.getOriginalValue());
                restoreInfo.setParameterMap(encryptInfo.getParameterMap());
                restoreInfo.setParameterKey(encryptInfo.getParameterKey());
                restoreInfo.setMetaObject(encryptInfo.getMetaObject());
                restoreInfo.setTargetObject(encryptInfo.getTargetObject());
                restoreInfo.setPropertyName(encryptInfo.getPropertyName());
                restoreInfo.setQueryWrapperParam(encryptInfo.isQueryWrapperParam());
                restoreInfo.setQueryWrapperParamName(encryptInfo.getQueryWrapperParamName());

                // 根据参数类型更新加密后的值
                if (encryptInfo.isQueryWrapperParam()) {
                    // QueryWrapper 参数特殊处理
                    updateQueryWrapperParameter(encryptInfo, encryptedValue);
                } else if (encryptInfo.getParameterMap() != null && encryptInfo.getParameterKey() != null) {
                    // 普通 Map 参数处理
                    // 优先使用 MetaObject 来设置嵌套属性
                    if (encryptInfo.getMetaObject() != null && encryptInfo.getMetaObject().hasSetter(encryptInfo.getParameterKey())) {
                        encryptInfo.getMetaObject().setValue(encryptInfo.getParameterKey(), encryptedValue);
                        log.debug("通过 MetaObject 更新嵌套参数: {} = {}", encryptInfo.getParameterKey(), encryptedValue);
                    } else {
                        // 回退到直接 Map 操作
                        encryptInfo.getParameterMap().put(encryptInfo.getParameterKey(), encryptedValue);
                        log.debug("更新 Map 参数: {} = {}", encryptInfo.getParameterKey(), encryptedValue);
                    }
                } else if (encryptInfo.getTargetObject() != null && encryptInfo.getPropertyName() != null) {
                    // 对象属性
                    ClazzUtil.setPropertyValue(encryptInfo.getTargetObject(), encryptInfo.getPropertyName(), encryptedValue);
                    log.debug("更新对象属性: {} = {}", encryptInfo.getPropertyName(), encryptedValue);
                }

                // 保存还原信息
                restoreInfos.add(restoreInfo);

                log.debug("字段 {}.{} 加密完成: {} -> {}",
                        encryptInfo.getTableName(), encryptInfo.getFieldName(),
                        encryptInfo.getOriginalValue(), encryptedValue);

            } catch (Exception e) {
                throw new DesensitizeException("加密参数失败: " + encryptInfo.getTableName() + "." + encryptInfo.getFieldName(), e);
            }
        }

        // 保存还原信息到 ThreadLocal
        SqlRewriteContext.cache(restoreInfos);
    }

    /**
     * 更新 QueryWrapper 参数
     */
    private static void updateQueryWrapperParameter(ParameterEncryptInfo encryptInfo, String encryptedValue) {
        try {
            // 获取 QueryWrapper 对象
            Object wrapper = encryptInfo.getParameterMap().get("ew");
            if (wrapper == null) {
                log.error("无法获取 QueryWrapper 对象");
                return;
            }

            // 获取 paramNameValuePairs
            Map<String, Object> paramNameValuePairs = getParamNameValuePairs(wrapper);
            if (paramNameValuePairs == null) {
                log.error("无法获取 QueryWrapper 的 paramNameValuePairs");
                return;
            }

            // 更新参数值
            String paramName = encryptInfo.getQueryWrapperParamName();
            paramNameValuePairs.put(paramName, encryptedValue);

            log.debug("更新 QueryWrapper 参数: {} = {}", paramName, encryptedValue);

            // 同时尝试通过 MetaObject 更新（如果可能的话）
            if (encryptInfo.getMetaObject() != null && encryptInfo.getMetaObject().hasSetter(encryptInfo.getParameterKey())) {
                encryptInfo.getMetaObject().setValue(encryptInfo.getParameterKey(), encryptedValue);
                log.debug("通过 MetaObject 同步更新 QueryWrapper 参数: {}", encryptInfo.getParameterKey());
            }

        } catch (Exception e) {
            throw new DesensitizeException("更新 QueryWrapper 参数失败: " + encryptInfo.getTableName() + "." + encryptInfo.getFieldName(), e);
        }
    }

    /**
     * 获取 QueryWrapper 的 paramNameValuePairs
     */
    private static Map<String, Object> getParamNameValuePairs(Object wrapper) {
        try {
            // 尝试通过字段直接访问
            Field paramField = ClazzUtil.findField(wrapper.getClass(), "paramNameValuePairs");
            if (paramField != null) {
                paramField.setAccessible(true);
                return (Map<String, Object>) paramField.get(wrapper);
            }

            // 尝试通过 getter 方法
            Method getterMethod = ClazzUtil.findMethod(wrapper.getClass(), "getParamNameValuePairs");
            if (getterMethod != null) {
                getterMethod.setAccessible(true);
                return (Map<String, Object>) getterMethod.invoke(wrapper);
            }

            log.debug("无法获取 QueryWrapper 的 paramNameValuePairs");
            return null;
        } catch (Exception e) {
            throw new DesensitizeException("获取 QueryWrapper paramNameValuePairs 失败 ", e);
        }
    }

    /**
     * 还原原始值
     */
    public static void restoreOriginalValues(List<ParameterRestoreInfo> restoreInfos) {
        if (restoreInfos == null || restoreInfos.isEmpty()) {
            return;
        }

        log.debug("开始还原参数明文，共 {} 个参数", restoreInfos.size());

        for (ParameterRestoreInfo restoreInfo : restoreInfos) {
            try {
                // 根据参数类型还原原始值
                if (restoreInfo.isQueryWrapperParam()) {
                    // QueryWrapper 参数特殊处理
                    restoreQueryWrapperParameter(restoreInfo);
                } else if (restoreInfo.getParameterMap() != null && restoreInfo.getParameterKey() != null) {
                    // 普通 Map 参数处理
                    // 优先使用 MetaObject 来设置嵌套属性
                    if (restoreInfo.getMetaObject() != null && restoreInfo.getMetaObject().hasSetter(restoreInfo.getParameterKey())) {
                        restoreInfo.getMetaObject().setValue(restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                        log.debug("通过 MetaObject 还原嵌套参数: {} = {}", restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                    } else {
                        // 回退到直接 Map 操作
                        restoreInfo.getParameterMap().put(restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                        log.debug("还原 Map 参数: {} = {}", restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                    }
                } else if (restoreInfo.getTargetObject() != null && restoreInfo.getPropertyName() != null) {
                    // 对象属性
                    setPropertyValue(restoreInfo.getTargetObject(), restoreInfo.getPropertyName(), restoreInfo.getOriginalValue());
                    log.debug("还原对象属性: {} = {}", restoreInfo.getPropertyName(), restoreInfo.getOriginalValue());
                }
                log.debug("参数还原完成: {}", restoreInfo.getOriginalValue());
            } catch (Exception e) {
                throw new DesensitizeException("还原参数失败", e);
            }
        }
        log.debug("参数明文还原完成");
    }

    /**
     * 还原 QueryWrapper 参数
     */
    private static void restoreQueryWrapperParameter(ParameterRestoreInfo restoreInfo) {
        try {
            // 获取 QueryWrapper 对象
            Object wrapper = restoreInfo.getParameterMap().get("ew");
            if (wrapper == null) {
                log.error("无法获取 QueryWrapper 对象进行还原");
                return;
            }

            // 获取 paramNameValuePairs
            Map<String, Object> paramNameValuePairs = getParamNameValuePairs(wrapper);
            if (paramNameValuePairs == null) {
                log.error("无法获取 QueryWrapper 的 paramNameValuePairs 进行还原");
                return;
            }

            // 还原参数值
            String paramName = restoreInfo.getQueryWrapperParamName();
            paramNameValuePairs.put(paramName, restoreInfo.getOriginalValue());

            log.debug("还原 QueryWrapper 参数: {} = {}", paramName, restoreInfo.getOriginalValue());

            // 同时尝试通过 MetaObject 还原（如果可能的话）
            if (restoreInfo.getMetaObject() != null && restoreInfo.getMetaObject().hasSetter(restoreInfo.getParameterKey())) {
                restoreInfo.getMetaObject().setValue(restoreInfo.getParameterKey(), restoreInfo.getOriginalValue());
                log.debug("通过 MetaObject 同步还原 QueryWrapper 参数: {}", restoreInfo.getParameterKey());
            }

        } catch (Exception e) {
            throw new DesensitizeException("还原 QueryWrapper 参数失败", e);
        }
    }
}
