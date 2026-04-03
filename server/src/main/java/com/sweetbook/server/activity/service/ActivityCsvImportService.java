package com.sweetbook.server.activity.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.activity.dto.ActivityImportResponse;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.repository.UserRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ActivityCsvImportService {

    private static final DateTimeFormatter ACTIVITY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter START_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityCsvHeaderNormalizer headerNormalizer;

    @Transactional
    public ActivityImportResponse importCsv(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 파일이 비어 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int importedCount = 0;
        int skippedCount = 0;

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
                )
        ) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 헤더가 없습니다.");
            }

            List<String> rawHeaders = parseCsvLine(headerLine);
            List<String> normalizedHeaders = headerNormalizer.normalize(rawHeaders);

            try (CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                for (CSVRecord record : parser) {
                    Map<String, String> row = toRow(normalizedHeaders, record);
                    String externalActivityId = trimToNull(getFirst(row, "Activity ID"));
                    LocalDateTime activityDateTime = parseActivityDateTime(row);

                    if (externalActivityId == null || activityDateTime == null) {
                        skippedCount++;
                        continue;
                    }
                    if (activityRepository.existsByUserIdAndExternalActivityId(userId, externalActivityId)) {
                        skippedCount++;
                        continue;
                    }

                    String activityType = trimToNull(getFirst(row, "Activity Type", "Type"));
                    String activityName = trimToNull(getFirst(row, "Activity Name"));

                    Activity activity = Activity.builder()
                            .user(user)
                            .externalActivityId(externalActivityId)
                            .activityType(activityType == null ? "UNKNOWN" : activityType)
                            .activityName(activityName == null ? "이름 없음" : activityName)
                            .description(trimToNull(getFirst(row, "Activity Description")))
                            .activityDateTime(activityDateTime)
                            .activityMonth(YearMonth.from(activityDateTime).toString())
                            .distanceKm(parseDouble(getFirst(row, "Distance")))
                            .movingTimeSeconds(parseInteger(getFirst(row, "Moving Time")))
                            .elapsedTimeSeconds(parseInteger(getFirst(row, "Elapsed Time.1", "Elapsed Time")))
                            .averageSpeed(parseDouble(getFirst(row, "Average Speed")))
                            .elevationGain(parseDouble(getFirst(row, "Elevation Gain")))
                            .calories(parseInteger(getFirst(row, "Calories")))
                            .build();

                    activityRepository.save(activity);
                    importedCount++;
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.CSV_IMPORT_FAILED, "파일을 읽는 중 오류가 발생했습니다.");
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.CSV_IMPORT_FAILED, ex.getMessage());
        }

        return new ActivityImportResponse(importedCount, skippedCount);
    }

    private Map<String, String> toRow(List<String> headers, CSVRecord record) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String value = i < record.size() ? record.get(i) : null;
            row.put(headers.get(i), value);
        }
        return row;
    }

    private List<String> parseCsvLine(String csvLine) throws IOException {
        try (CSVParser parser = CSVParser.parse(csvLine, CSVFormat.DEFAULT)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                return List.of();
            }
            CSVRecord record = records.get(0);
            return record.stream().toList();
        }
    }

    private String getFirst(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private LocalDateTime parseActivityDateTime(Map<String, String> row) {
        String activityDate = trimToNull(getFirst(row, "Activity Date"));
        if (activityDate != null) {
            try {
                return LocalDateTime.parse(activityDate, ACTIVITY_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        String startTime = trimToNull(getFirst(row, "Start Time"));
        if (startTime != null) {
            try {
                return LocalDateTime.parse(startTime, START_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    private Integer parseInteger(String value) {
        String normalized = normalizeNumber(value);
        if (normalized == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(normalized));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        String normalized = normalizeNumber(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeNumber(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.replace(",", ".");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

