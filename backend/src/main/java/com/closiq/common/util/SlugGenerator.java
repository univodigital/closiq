package com.closiq.common.util;

import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
public class SlugGenerator {

    public String slugify(String input) {
        return SlugUtils.slugify(input);
    }

    public String uniqueSlug(String base, Predicate<String> exists) {
        return SlugUtils.uniqueSlug(base, exists);
    }
}
