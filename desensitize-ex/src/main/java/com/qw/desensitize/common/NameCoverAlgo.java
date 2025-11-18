package com.qw.desensitize.common;

import io.github.qwzhang01.desensitize.mask.shield.CoverAlgo;
import org.springframework.stereotype.Component;

/**
 * example
 */
@Component
public class NameCoverAlgo implements CoverAlgo {
    @Override
    public String mask(String content) {
        return "--seven--";
    }
}
