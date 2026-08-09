package com.closiq.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    @Test
    void slugify_normalizesTitle() {
        assertThat(SlugUtils.slugify("Emerald Draped Saree!")).isEqualTo("emerald-draped-saree");
    }

    @Test
    void uniqueSlug_appendsSuffixWhenTaken() {
        String slug = SlugUtils.uniqueSlug("Test Product", s -> s.equals("test-product"));
        assertThat(slug).isEqualTo("test-product-2");
    }
}
