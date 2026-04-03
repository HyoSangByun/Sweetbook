package com.sweetbook.server.sweetbook.client;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.books.AddPageRequest;
import com.sweetbook.server.sweetbook.dto.books.CreateBookRequest;
import com.sweetbook.server.sweetbook.dto.books.CreateBookResponseData;
import com.sweetbook.server.sweetbook.dto.books.UploadBookPhotoResponseData;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SweetbookBooksClient {

    private final RestClient sweetbookRestClient;

    public String createBook(String title, String bookSpecUid, String externalRef) {
        SweetbookApiResponse<CreateBookResponseData> response = sweetbookRestClient.post()
                .uri("/v1/books")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateBookRequest(title, bookSpecUid, externalRef))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.success() || response.data() == null || response.data().bookUid() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to create book.");
        }
        return response.data().bookUid();
    }

    public void addCover(String bookUid, String templateUid, Map<String, Object> parameters) {
        AddPageRequest request = new AddPageRequest(templateUid, parameters);
        SweetbookApiResponse<Object> response = sweetbookRestClient.post()
                .uri("/v1/books/{bookUid}/cover", bookUid)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to add book cover.");
        }
    }

    public void addContent(String bookUid, String templateUid, Map<String, Object> parameters, String breakBefore) {
        AddPageRequest request = new AddPageRequest(templateUid, parameters);
        SweetbookApiResponse<Object> response = sweetbookRestClient.post()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/v1/books/{bookUid}/contents");
                    if (breakBefore != null && !breakBefore.isBlank()) {
                        builder.queryParam("breakBefore", breakBefore);
                    }
                    return builder.build(bookUid);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to add book content.");
        }
    }

    public String uploadPhoto(String bookUid, Path filePath) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new FileSystemResource(filePath));

        SweetbookApiResponse<UploadBookPhotoResponseData> response = sweetbookRestClient.post()
                .uri("/v1/books/{bookUid}/photos", bookUid)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.success() || response.data() == null || response.data().fileName() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to upload photo.");
        }
        return response.data().fileName();
    }

    public void finalizeBook(String bookUid) {
        SweetbookApiResponse<Object> response = sweetbookRestClient.post()
                .uri("/v1/books/{bookUid}/finalization", bookUid)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to finalize book.");
        }
    }
}
