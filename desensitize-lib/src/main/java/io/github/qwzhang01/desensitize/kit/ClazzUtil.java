package io.github.qwzhang01.desensitize.kit;

public class ClazzUtil {
    public static boolean isWrapper(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        String packageName = clazz.getPackageName();
        return !packageName.startsWith("java.lang")
                && !packageName.startsWith("java.math")
                && !packageName.startsWith("java.time");
    }
}
