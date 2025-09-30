package io.github.qwzhang01.desensitize.domain;

public class MaskContext {
    private static final ThreadLocal<Boolean> MASK_CONTEXT = new ThreadLocal<>();

    public static void start() {
        MASK_CONTEXT.set(true);
    }

    public static void stop() {
        MASK_CONTEXT.remove();
    }

    public static boolean isMask() {
        return MASK_CONTEXT.get() != null;
    }
}
