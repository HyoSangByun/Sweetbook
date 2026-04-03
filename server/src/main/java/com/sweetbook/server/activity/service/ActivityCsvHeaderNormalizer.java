package com.sweetbook.server.activity.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ActivityCsvHeaderNormalizer {

    public List<String> normalize(List<String> rawHeaders) {
        Map<String, Integer> counts = new HashMap<>();
        return rawHeaders.stream()
                .map(header -> {
                    int next = counts.getOrDefault(header, 0) + 1;
                    counts.put(header, next);
                    if (next == 1) {
                        return header;
                    }
                    return header + "." + (next - 1);
                })
                .toList();
    }
}
