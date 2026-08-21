package com.example.common.web.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shared jar carries its own tests, so a consumer module is not the first place a regression
 * in the envelope shows up.
 */
class PageResponseTest {

    @Test
    void copiesPageMetadataAndMapsContent() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 6);

        PageResponse<String> response = PageResponse.from(page, String::toUpperCase);

        assertThat(response.content()).containsExactly("A", "B");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void keepsPageMetadataWhenContentIsSuppliedSeparately() {
        var page = new PageImpl<>(List.of("a"), PageRequest.of(0, 1), 1);

        PageResponse<Integer> response = PageResponse.of(page, List.of(1));

        assertThat(response.content()).containsExactly(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }
}
