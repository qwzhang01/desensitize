package com.qw.desensitize.config;

import io.github.qwzhang01.desensitize.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Example
 *
 * @author avinzhang
 */
@Configuration
public class ShieldConfig {
    @Bean
    public AbstractEncryptAlgoContainer getEncryptAlgoContainer() {
        return new AbstractEncryptAlgoContainer() {
            private EncryptionAlgo defaultEncryptionAlgo;
            private String hx = "avin";

            @Override
            public EncryptionAlgo defaultEncryptAlgo() {
                if (defaultEncryptionAlgo != null) {
                    return defaultEncryptionAlgo;
                }
                synchronized (this) {
                    if (this.defaultEncryptionAlgo != null) {
                        return defaultEncryptionAlgo;
                    }
                    // 实现自己的加密算法
                    this.defaultEncryptionAlgo = new EncryptionAlgo() {
                        @Override
                        public String encrypt(String value) {
                            if (StringUtils.hasText(value)) {
                                return value + hx;
                            }
                            return value;
                        }

                        @Override
                        public String decrypt(String value) {
                            if (StringUtils.hasText(value)) {
                                if (value.endsWith(hx)) {
                                    return value.substring(0, value.length() - hx.length());
                                }
                            }
                            return value;
                        }
                    };

                    return this.defaultEncryptionAlgo;
                }
            }
        };
    }
}
