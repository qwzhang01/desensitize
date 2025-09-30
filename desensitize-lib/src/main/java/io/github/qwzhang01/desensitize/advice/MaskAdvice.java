package io.github.qwzhang01.desensitize.advice;

import io.github.qwzhang01.desensitize.kit.MaskAlgoContainer;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

/**
 * 接口返回字段脱敏拦截器
 * 使用说明
 * <p>
 * 使用方法，返回结果的类继承 com.qw.desensitize.dto.UnSensitiveDto
 * com.qw.desensitize.dto.UnSensitiveDto#sensitiveFlag，脱敏的标识，比如本人登录状态，则赋值为false，不脱敏，其他人登录查看则赋值为true脱敏
 * <p>
 * 需要脱敏的字段添加注解 com.qw.desensitize.common.sensitive.UnSensitive
 * com.qw.desensitize.common.sensitive.UnSensitive#type() 为脱敏算法，目前实现了手机，身份证，邮箱三种脱敏算法，对应枚举定位位置 com.qw.desensitize.dto.UnSensitiveDto
 *
 * @author avinzhang
 */
@ControllerAdvice
public class MaskAdvice implements ResponseBodyAdvice<Object> {
    private final MaskAlgoContainer maskAlgoContainer;

    public MaskAdvice() {
        this.maskAlgoContainer = new MaskAlgoContainer();
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 不是文件下载
        boolean notFileDownload = returnType.getParameterType().equals(InputStreamResource.class) ||
                returnType.getParameterType().equals(Resource.class) ||
                returnType.getParameterType().equals(File.class);
        if (notFileDownload) {
            return false;
        }

        // flux 响应式结果不包装
        boolean isFlux = Flux.class.isAssignableFrom(returnType.getParameterType())
                || SseEmitter.class.isAssignableFrom(returnType.getParameterType())
                || Mono.class.isAssignableFrom(returnType.getParameterType());

        return !isFlux;
    }

    @Nullable
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return body;
        }
        return maskAlgoContainer.mask(body);
    }

}