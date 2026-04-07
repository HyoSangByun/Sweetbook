package com.sweetbook.server.sweetbook.client;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.sweetbook.dto.SweetbookApiResponse;
import com.sweetbook.server.sweetbook.dto.books.CreateBookRequest;
import com.sweetbook.server.sweetbook.dto.books.CreateBookResponseData;
import com.sweetbook.server.sweetbook.dto.books.UploadBookPhotoResponseData;
import java.nio.file.Path;
import java.util.List;
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
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class SweetbookBooksClient {

    private final RestClient sweetbookRestClient;
    private static final ParameterizedTypeReference<SweetbookApiResponse<Map<String, Object>>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    public String createBook(String title, String bookSpecUid, String externalRef) {
        SweetbookApiResponse<CreateBookResponseData> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/books")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateBookRequest(title, bookSpecUid, externalRef))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to create book via Sweetbook API."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null || response.data().bookUid() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to create book.");
        }
        return response.data().bookUid();
    }

    public void addCover(String bookUid, String templateUid, Map<String, Object> parameters) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("templateUid", templateUid);
        body.add("parameters", toJson(parameters));
        SweetbookApiResponse<Object> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/books/{bookUid}/cover", bookUid)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to add book cover via Sweetbook API."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to add book cover.");
        }
    }

    public void addContent(String bookUid, String templateUid, Map<String, Object> parameters, String breakBefore) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("templateUid", templateUid);
        body.add("parameters", toJson(parameters));
        SweetbookApiResponse<Object> response;
        try {
            response = sweetbookRestClient.post()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v1/books/{bookUid}/contents");
                        if (breakBefore != null && !breakBefore.isBlank()) {
                            builder.queryParam("breakBefore", breakBefore);
                        }
                        return builder.build(bookUid);
                    })
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to add book content via Sweetbook API."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to add book content.");
        }
    }

    public String uploadPhoto(String bookUid, Path filePath) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(filePath));

        SweetbookApiResponse<UploadBookPhotoResponseData> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/books/{bookUid}/photos", bookUid)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to upload photo via Sweetbook API."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null || response.data().fileName() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to upload photo.");
        }
        return response.data().fileName();
    }

    public void finalizeBook(String bookUid) {
        SweetbookApiResponse<Object> response;
        try {
            response = sweetbookRestClient.post()
                    .uri("/v1/books/{bookUid}/finalization", bookUid)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to finalize book via Sweetbook API."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to finalize book.");
        }
    }

    public void deleteBook(String bookUid) {
        SweetbookApiResponse<Object> response;
        try {
            response = sweetbookRestClient.delete()
                    .uri("/v1/books/{bookUid}", bookUid)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(
                    ErrorCode.SWEETBOOK_CALL_FAILED,
                    "Failed to delete temporary book via Sweetbook API."
            );
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success()) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to delete temporary book.");
        }
    }

    public List<Map<String, Object>> getBookSpecs() {
        SweetbookApiResponse<Map<String, Object>> response;
        try {
            response = sweetbookRestClient.get()
                    .uri("/v1/book-specs")
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch book specs.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch book specs.");
        }

        Object specs = response.data().get("bookSpecs");
        if (specs instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    public List<Map<String, Object>> getTemplates(String bookSpecUid, String templateKind) {
        SweetbookApiResponse<Map<String, Object>> response;
        try {
            response = sweetbookRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v1/templates")
                            .queryParam("bookSpecUid", bookSpecUid)
                            .queryParam("templateKind", templateKind)
                            .build())
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch templates.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch templates.");
        }

        Object templates = response.data().get("templates");
        if (templates instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    public Map<String, Object> getTemplateDetail(String templateUid) {
        SweetbookApiResponse<Map<String, Object>> response;
        try {
            response = sweetbookRestClient.get()
                    .uri("/v1/templates/{templateUid}", templateUid)
                    .retrieve()
                    .body(MAP_RESPONSE_TYPE);
        } catch (RestClientException e) {
            BusinessException be = new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch template detail.");
            be.initCause(e);
            throw be;
        }

        if (response == null || !response.success() || response.data() == null) {
            throw new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED, "Failed to fetch template detail.");
        }
        return response.data();
    }

    private String toJson(Map<String, Object> source) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(source == null ? Map.of() : source);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid template parameter payload.");
        }
    }
}
