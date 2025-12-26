package io.github.qwzhang01.dsecurity.domain;

import org.apache.ibatis.reflection.MetaObject;

import java.util.Map;

/**
 * Parameter restoration information holder.
 *
 * <p>This class stores information needed to restore parameter values to their
 * original state after SQL execution. It works in conjunction with
 * {@link ParameterEncryptInfo} to ensure parameters are encrypted only during
 * SQL execution and restored afterward.</p>
 *
 * <p>The restoration process supports the same three parameter types:</p>
 * <ul>
 *   <li>Map parameters</li>
 *   <li>Object parameters</li>
 *   <li>QueryWrapper parameters</li>
 * </ul>
 *
 * @author avinzhang
 * @see ParameterEncryptInfo
 */
public class ParameterRestoreInfo {
    private String originalValue;

    // Map parameter fields
    private Map<String, Object> parameterMap;
    private String parameterKey;
    private MetaObject metaObject;

    // Object parameter fields
    private Object targetObject;
    private String propertyName;

    // QueryWrapper parameter fields
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
