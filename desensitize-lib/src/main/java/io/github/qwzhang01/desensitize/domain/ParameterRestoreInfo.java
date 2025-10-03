package io.github.qwzhang01.desensitize.domain;

import org.apache.ibatis.reflection.MetaObject;

import java.util.Map;

/**
 * 参数还原信息
 *
 * @author avinzhang
 */
public class ParameterRestoreInfo {
    private String originalValue;

    // Map 参数相关
    private Map<String, Object> parameterMap;
    private String parameterKey;
    private MetaObject metaObject;

    // 对象参数相关
    private Object targetObject;
    private String propertyName;

    // QueryWrapper 参数相关
    private boolean isQueryWrapperParam = false;
    private String queryWrapperParamName;

    // Getters and Setters
    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public Map<String, Object> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public void setParameterKey(String parameterKey) {
        this.parameterKey = parameterKey;
    }

    public MetaObject getMetaObject() {
        return metaObject;
    }

    public void setMetaObject(MetaObject metaObject) {
        this.metaObject = metaObject;
    }

    public Object getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public boolean isQueryWrapperParam() {
        return isQueryWrapperParam;
    }

    public void setQueryWrapperParam(boolean queryWrapperParam) {
        isQueryWrapperParam = queryWrapperParam;
    }

    public String getQueryWrapperParamName() {
        return queryWrapperParamName;
    }

    public void setQueryWrapperParamName(String queryWrapperParamName) {
        this.queryWrapperParamName = queryWrapperParamName;
    }
}
