package io.github.qwzhang01.desensitize.mask.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 继承了 MaskVo 的类，如果 maskFlag 为 true，则进行脱敏处理
 * 适用列表数据，且不同行脱敏规则不一致
 * 适用同一个对象，不同场景脱敏字段不一致的情况
 *
 * @author avinzhang
 */
public class MaskVo implements Serializable {
    /**
     * 标记整列是否做脱敏
     */
    private Boolean maskFlag;
    /**
     * 标记脱敏字段，如果为空则按注解脱敏
     */
    private List<String> maskFields;

    public Boolean getMaskFlag() {
        return maskFlag;
    }

    public void setMaskFlag(Boolean maskFlag) {
        this.maskFlag = maskFlag;
    }

    public List<String> getMaskFields() {
        return maskFields;
    }

    public void setMaskFields(List<String> maskFields) {
        this.maskFields = maskFields;
    }
}
