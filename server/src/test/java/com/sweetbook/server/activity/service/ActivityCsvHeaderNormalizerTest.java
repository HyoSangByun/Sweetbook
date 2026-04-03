package com.sweetbook.server.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityCsvHeaderNormalizerTest {

    private final ActivityCsvHeaderNormalizer normalizer = new ActivityCsvHeaderNormalizer();

    @Test
    void 중복_헤더는_뒤에_순번_접미사를_붙인다() {
        List<String> normalized = normalizer.normalize(List.of(
                "Distance",
                "Elapsed Time",
                "Distance",
                "Elapsed Time",
                "Distance"
        ));

        assertThat(normalized).containsExactly(
                "Distance",
                "Elapsed Time",
                "Distance.1",
                "Elapsed Time.1",
                "Distance.2"
        );
    }
}

