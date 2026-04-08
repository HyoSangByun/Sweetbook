package com.sweetbook.server.sweetbook.service;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.client.SweetbookBooksClient;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SweetbookBookPhotoSelector {

    private final SweetbookBooksClient sweetbookBooksClient;

    public Set<String> listSelectableFileNames(String bookUid) {
        return sweetbookBooksClient.getBookPhotos(bookUid).stream()
                .map(photo -> photo.get("fileName"))
                .filter(value -> value != null)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void validateSelectedFileName(Set<String> selectableFileNames, String fileName, String fieldName) {
        String normalized = fileName == null ? "" : fileName.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " 값이 비어 있습니다.");
        }
        if (!selectableFileNames.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "는 GET /books/{bookUid}/photos 에서 선택 가능한 fileName 이어야 합니다. value=" + normalized
            );
        }
    }
}
