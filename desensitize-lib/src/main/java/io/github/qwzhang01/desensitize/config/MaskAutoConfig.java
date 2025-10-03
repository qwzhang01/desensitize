/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.github.qwzhang01.desensitize.config;

import io.github.qwzhang01.desensitize.advice.MaskAdvice;
import io.github.qwzhang01.desensitize.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.container.EncryptFieldTableContainer;
import io.github.qwzhang01.desensitize.container.EncryptionAlgoContainer;
import io.github.qwzhang01.desensitize.container.MaskAlgoContainer;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.shield.CoverAlgo;
import io.github.qwzhang01.desensitize.shield.DefaultCoverAlgo;
import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration class for data masking and encryption functionality.
 *
 * <p>This configuration class automatically registers default implementations
 * of masking and encryption algorithms when no custom implementations are provided.
 * It follows Spring Boot's auto-configuration pattern to provide sensible defaults
 * while allowing for easy customization.</p>
 *
 * <p>The configuration includes:</p>
 * <ul>
 *   <li>Default data masking algorithm for sensitive data protection</li>
 *   <li>Default encryption algorithm for data encryption/decryption</li>
 *   <li>Spring context utility for accessing beans in non-Spring managed classes</li>
 * </ul>
 *
 * <p>MyBatis interceptors are configured separately in {@link MyBatisInterceptorAutoConfig}
 * to avoid circular dependency issues.</p>
 *
 * @author avinzhang
 * @see CoverAlgo
 * @see EncryptionAlgo
 * @see SpringContextUtil
 * @see MyBatisInterceptorAutoConfig
 * @since 1.0.0
 */
@Configuration
public class MaskAutoConfig {

    @Bean
    @ConditionalOnMissingBean(MaskAdvice.class)
    public MaskAdvice maskAdvice() {
        return new MaskAdvice(new MaskAlgoContainer());
    }

    /**
     * Provides a default data masking algorithm bean.
     * This bean is only created if no other CoverAlgo implementation is found in the context.
     *
     * @return a new instance of DefaultCoverAlgo with comprehensive masking capabilities
     */
    @Bean
    @ConditionalOnMissingBean(CoverAlgo.class)
    public CoverAlgo coverAlgo() {
        return new DefaultCoverAlgo();
    }

    /**
     * Provides a default encryption algorithm bean.
     * This bean is only created if no other EncryptionAlgo implementation is found in the context.
     *
     * @return a new instance of DefaultEncryptionAlgo using DES encryption
     */
    @Bean
    @ConditionalOnMissingBean(DefaultEncryptionAlgo.class)
    public DefaultEncryptionAlgo defaultEncryptionAlgo() {
        return new DefaultEncryptionAlgo();
    }

    @Bean
    @ConditionalOnMissingBean(AbstractEncryptAlgoContainer.class)
    public AbstractEncryptAlgoContainer encryptAlgoContainer(EncryptionAlgo encryptionAlgo) {
        return new EncryptionAlgoContainer(encryptionAlgo);
    }

    @Bean
    @ConditionalOnMissingBean(EncryptFieldTableContainer.class)
    public EncryptFieldTableContainer encryptFieldTableContainer() {
        return new EncryptFieldTableContainer();
    }

    /**
     * Provides a Spring context utility bean for accessing Spring-managed beans
     * from non-Spring managed classes.
     * This bean is only created if no other SpringContextUtil implementation is found in the context.
     *
     * @return a new instance of SpringContextUtil for bean access operations
     */
    @Bean
    @ConditionalOnMissingBean(SpringContextUtil.class)
    public SpringContextUtil springContextUtil() {
        return new SpringContextUtil();
    }
}
