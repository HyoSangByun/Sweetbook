package com.sweetbook.server.sweetbook.dto.books;

import java.util.Map;

public record AddPageRequest(
        String templateUid,
        Map<String, Object> parameters
) {
}

