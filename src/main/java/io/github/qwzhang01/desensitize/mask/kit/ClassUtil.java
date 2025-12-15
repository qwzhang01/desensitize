package io.github.qwzhang01.desensitize.mask.kit;

import io.github.qwzhang01.desensitize.mask.MaskAlgoContainer;
import io.github.qwzhang01.desensitize.mask.annotation.Mask;
import io.github.qwzhang01.desensitize.mask.domain.MaskField;
import io.github.qwzhang01.desensitize.mask.domain.MaskVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassUtil {
    private static final Logger log = LoggerFactory.getLogger(MaskAlgoContainer.class);

    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            String.class, Integer.class, Long.class, Double.class, Float.class,
            Boolean.class, Byte.class, Short.class, Character.class,
            int.class, long.class, double.class, float.class,
            boolean.class, byte.class, short.class, char.class
    );

    public static List<MaskField> getMaskFields(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        Boolean maskFlag = null;
        if (obj instanceof MaskVo maskVo) {
            maskFlag = Boolean.TRUE.equals(maskVo.getMaskFlag());
        }

        return collectMaskFields(obj, maskFlag, false);
    }

    private static List<MaskField> collectMaskFields(Object obj, Boolean maskFlag, boolean parentFieldHasMask) {
        if (obj == null) {
            return Collections.emptyList();
        }
        try {

            if (obj instanceof Collection<?> collection) {
                return processCollection(collection, maskFlag, parentFieldHasMask);
            } else if (obj.getClass().isArray()) {
                return processArray((Object[]) obj, maskFlag, parentFieldHasMask);
            } else if (obj instanceof Map<?, ?> map) {
                return processMap(map, maskFlag, parentFieldHasMask);
            } else {

                Class<?> clazz = obj.getClass();
                Field[] fields = clazz.getDeclaredFields();

                List<MaskField> list = new ArrayList<>();

                for (Field field : fields) {
                    if (shouldSkipField(field)) {
                        continue;
                    }

                    field.setAccessible(true);

                    Mask maskAnnotation = findMaskAnnotation(field);
                    if (maskAnnotation != null) {
                        Object fieldValue = field.get(obj);
                        if (fieldValue != null) {
                            if (maskFlag == null) {
                                if (obj instanceof MaskVo maskVo) {
                                    maskFlag = Boolean.TRUE.equals(maskVo.getMaskFlag());
                                }
                            }

                            if (isComplexObject(fieldValue.getClass())) {
                                List<MaskField> maskFields = collectMaskFields(fieldValue, maskFlag, true);
                                if (!maskFields.isEmpty()) {
                                    list.addAll(maskFields);
                                }
                            } else {

                                boolean behest = maskAnnotation.behest();
                                if (behest) {
                                    behest = parentFieldHasMask;
                                } else {
                                    behest = true;
                                }

                                MaskField maskField = new MaskField(field, obj, behest,
                                        maskFlag == null || maskFlag, maskAnnotation);
                                list.add(maskField);
                            }
                        }
                    } else {
                        // 字段没有@Mask注解，但仍需递归处理其子字段
                        Object fieldValue = field.get(obj);
                        if (fieldValue != null) {
                            if (maskFlag == null) {
                                if (obj instanceof MaskVo maskVo) {
                                    maskFlag = Boolean.TRUE.equals(maskVo.getMaskFlag());
                                }
                            }
                            if (isComplexObject(fieldValue.getClass())) {
                                List<MaskField> maskFields = collectMaskFields(fieldValue, maskFlag, false);
                                if (!maskFields.isEmpty()) {
                                    list.addAll(maskFields);
                                }
                            }
                        }
                    }
                }

                return list;
            }
        } catch (Exception e) {
            log.error("脱敏异常", e);
            return Collections.emptyList();
        }
    }


    private static Mask findMaskAnnotation(Field field) {
        // 首先查找直接注解
        Mask mask = field.getAnnotation(Mask.class);
        if (mask != null) {
            return mask;
        }

        // 查找元注解
        for (Annotation annotation : field.getAnnotations()) {
            Mask metaMask = annotation.annotationType().getAnnotation(Mask.class);
            if (metaMask != null) {
                return metaMask;
            }
        }

        return null;
    }


    /**
     * 判断是否应该跳过该字段
     */
    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers);
    }

    /**
     * 判断是否为基本类型或常见类型
     */
    private static boolean isPrimitiveOrCommonType(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        return PRIMITIVE_TYPES.contains(clazz) ||
                clazz.isPrimitive() ||
                clazz.isEnum() ||
                clazz.getPackageName().startsWith("java.") ||
                clazz.getPackageName().startsWith("javax.");
    }

    /**
     * 判断是否为复杂对象（需要递归处理）
     */
    private static boolean isComplexObject(Class<?> clazz) {
        if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz) || clazz.isArray()) {
            return true;
        }

        return !isPrimitiveOrCommonType(clazz);
    }


    private static List<MaskField> processCollection(Collection<?> collection,
                                                     Boolean maskFlag, boolean parentFieldHasMask) {

        List<MaskField> list = new ArrayList<>();

        for (Object item : collection) {
            if (item != null && isComplexObject(item.getClass())) {
                List<MaskField> maskFields = collectMaskFields(item, maskFlag, parentFieldHasMask);
                if (!maskFields.isEmpty()) {
                    list.addAll(maskFields);
                }
            }
        }

        return list;
    }

    /**
     * Process array elements
     */
    private static List<MaskField> processArray(Object[] array,
                                                Boolean maskFlag, boolean parentFieldHasMask) {

        List<MaskField> list = new ArrayList<>();

        for (int i = 0; i < array.length; i++) {
            Object item = array[i];
            if (item != null && isComplexObject(item.getClass())) {
                List<MaskField> maskFields = collectMaskFields(item, maskFlag, parentFieldHasMask);
                if (!maskFields.isEmpty()) {
                    list.addAll(maskFields);
                }
            }
        }
        return list;
    }

    /**
     * Process map entries
     */
    private static List<MaskField> processMap(Map<?, ?> map,
                                              Boolean maskFlag, boolean parentFieldHasMask) {

        List<MaskField> list = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null && isComplexObject(value.getClass())) {
                List<MaskField> maskFields = collectMaskFields(value, maskFlag, parentFieldHasMask);
                if (!maskFields.isEmpty()) {
                    list.addAll(maskFields);
                }
            }
        }
        return list;
    }
}
