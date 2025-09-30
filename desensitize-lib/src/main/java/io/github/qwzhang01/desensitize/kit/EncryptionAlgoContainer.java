package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

public class EncryptionAlgoContainer {
    public static EncryptionAlgo getAlgo() {
        return SpringContextUtil.getBeanSafely(DefaultEncryptionAlgo.class);
    }

    public static EncryptionAlgo getAlgo(Class<? extends EncryptionAlgo> clazz) {
        EncryptionAlgo algo = SpringContextUtil.getBeanSafely(clazz);
        if (algo != null) {
            return algo;
        }
        return SpringContextUtil.getBeanSafely(DefaultEncryptionAlgo.class);
    }
}