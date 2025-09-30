package io.github.qwzhang01.desensitize.config;

import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 脱敏自动配置类
 *
 * @author qwzhang01
 */
@Configuration
public class MaskAutoConfig {

    /**
     * 默认加密算法Bean
     */
    @Bean
    @ConditionalOnMissingBean(DefaultEncryptionAlgo.class)
    public DefaultEncryptionAlgo defaultEncryptionAlgo() {
        return new DefaultEncryptionAlgo();
    }

    /**
     * Spring 上下文工具类Bean
     */
    @Bean
    @ConditionalOnMissingBean(SpringContextUtil.class)
    public SpringContextUtil springContextUtil() {
        return new SpringContextUtil();
    }
}
