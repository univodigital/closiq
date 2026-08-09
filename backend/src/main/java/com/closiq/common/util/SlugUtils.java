package com.closiq.common.util;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

@UtilityClass
public class SlugUtils {

    public String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "item";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
        if (normalized.isBlank()) {
            return "item";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    public String uniqueSlug(String base, Predicate<String> exists) {
        String slug = slugify(base);
        if (!exists.test(slug)) {
            return slug;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = slug + "-" + i;
            if (!exists.test(candidate)) {
                return candidate;
            }
        }
        return slug + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
