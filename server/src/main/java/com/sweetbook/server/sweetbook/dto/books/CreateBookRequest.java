package com.sweetbook.server.sweetbook.dto.books;

public record CreateBookRequest(
        String title,
        String bookSpecUid,
        String externalRef
) {
}

