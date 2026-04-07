package com.sweetbook.server.sweetbook.client;

import org.slf4j.Logger;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public final class SweetbookClientErrorLogger {

    private static final int MAX_RESPONSE_BODY_LENGTH = 1000;

    private SweetbookClientErrorLogger() {
    }

    public static void logRestClientException(Logger log, String action, String endpoint, RestClientException exception) {
        Throwable rootCause = findRootCause(exception);
        String rootCauseType = rootCause == null ? "n/a" : rootCause.getClass().getSimpleName();
        String rootCauseMessage = rootCause == null ? "n/a" : rootCause.getMessage();

        if (exception instanceof RestClientResponseException responseException) {
            String responseBody = abbreviate(responseException.getResponseBodyAsString(), MAX_RESPONSE_BODY_LENGTH);
            log.error(
                    "Sweetbook API {} failed. endpoint={}, status={}, responseBody={}, exceptionType={}, message={}, rootCauseType={}, rootCauseMessage={}",
                    action,
                    endpoint,
                    responseException.getStatusCode(),
                    responseBody,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    rootCauseType,
                    rootCauseMessage,
                    exception
            );
            return;
        }

        log.error(
                "Sweetbook API {} failed. endpoint={}, exceptionType={}, message={}, rootCauseType={}, rootCauseMessage={}",
                action,
                endpoint,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                rootCauseType,
                rootCauseMessage,
                exception
        );
    }

    private static Throwable findRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }
}
