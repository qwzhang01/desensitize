package io.github.qwzhang01.desensitize.config;

import io.github.qwzhang01.desensitize.core.DecryptInterceptor;
import io.github.qwzhang01.desensitize.core.EncryptInterceptor;
import io.github.qwzhang01.desensitize.core.ExecutorInterceptor;
import io.github.qwzhang01.desensitize.core.SqlRewriteInterceptor;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.shield.CoverAlgo;
import io.github.qwzhang01.desensitize.shield.DefaultCoverAlgo;
import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import jakarta.annotation.PostConstruct;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author qwzhang01
 * @see CoverAlgo
 * @see EncryptionAlgo
 * @see SpringContextUtil
 * @since 1.0.0
 */
@Configuration
public class MaskAutoConfig {
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Bean
    @ConditionalOnMissingBean(DecryptInterceptor.class)
    public Interceptor decryptInterceptor() {
        return new DecryptInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(EncryptInterceptor.class)
    public Interceptor encryptInterceptor() {
        return new EncryptInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(ExecutorInterceptor.class)
    public Interceptor executorInterceptor() {
        return new ExecutorInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(SqlRewriteInterceptor.class)
    public Interceptor sqlRewriteInterceptor() {
        return new SqlRewriteInterceptor();
    }

    @PostConstruct
    public void addInterceptor() {
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();

        configuration.addInterceptor(decryptInterceptor());
        configuration.addInterceptor(encryptInterceptor());
        configuration.addInterceptor(executorInterceptor());
        configuration.addInterceptor(sqlRewriteInterceptor());
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
    @ConditionalOnMissingBean(EncryptionAlgo.class)
    public EncryptionAlgo encryptionAlgo() {
        return new DefaultEncryptionAlgo();
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
