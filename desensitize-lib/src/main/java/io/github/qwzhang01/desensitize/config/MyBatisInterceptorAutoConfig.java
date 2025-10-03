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

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.github.qwzhang01.desensitize.core.DecryptInterceptor;
import io.github.qwzhang01.desensitize.core.EncryptTypeHandler;
import io.github.qwzhang01.desensitize.core.SqlRewriteInterceptor;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import jakarta.annotation.PostConstruct;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configuration for MyBatis interceptors.
 *
 * <p>This configuration class is responsible for registering MyBatis interceptors
 * after the MyBatis auto-configuration is complete. It's separated from the main
 * configuration to avoid circular dependency issues.</p>
 *
 * <p>The configuration runs after MybatisAutoConfiguration to ensure that
 * SqlSessionFactory is fully initialized before adding interceptors.</p>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass({SqlSessionFactory.class})
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
public class MyBatisInterceptorAutoConfig {

    @Autowired(required = false)
    private List<SqlSessionFactory> sqlSessionFactories;

    /**
     * Adds desensitization interceptors to all available SqlSessionFactory instances.
     * This method is called after the Spring context is fully initialized,
     * ensuring no circular dependency issues.
     */
    @PostConstruct
    public void addInterceptors() {
        if (sqlSessionFactories != null && !sqlSessionFactories.isEmpty()) {
            for (SqlSessionFactory sqlSessionFactory : sqlSessionFactories) {
                org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();

                // Add interceptors in the correct order
                // DecryptInterceptor should be first to decrypt data when reading
                configuration.addInterceptor(new DecryptInterceptor());

                // SqlRewriteInterceptor for SQL modification if needed
                configuration.addInterceptor(new SqlRewriteInterceptor());

                // Register Encrypt type handler
                configuration.getTypeHandlerRegistry().register(Encrypt.class, EncryptTypeHandler.class);
            }
        }
    }
}