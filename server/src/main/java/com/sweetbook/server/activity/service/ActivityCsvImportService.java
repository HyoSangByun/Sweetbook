package com.sweetbook.server.activity.service;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.activity.dto.ActivityImportResponse;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
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

    private static final long MAX_CSV_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final List<DateTimeFormatter> DATE_ONLY_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d", Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART)
    );

    private final ActivityRepository activityRepository;
    private final ActivityCsvHeaderNormalizer headerNormalizer;

    @Transactional
    public ActivityImportResponse importCsv(MultipartFile file) {
        validateFile(file);

        int importedCount = 0;
        int skippedCount = 0;
        List<ActivityImportResponse.SkippedRow> skippedRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 헤더가 비어 있습니다.");
            }

            List<String> rawHeaders = parseCsvLine(headerLine);
            List<String> normalizedHeaders = headerNormalizer.normalize(rawHeaders);
            validateHeaders(normalizedHeaders);

            try (CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
                for (CSVRecord record : parser) {
                    long rowNumber = record.getRecordNumber() + 1; // 헤더 라인 보정
                    Map<String, String> row = toRow(normalizedHeaders, record);
                    String externalActivityId = trimToNull(getFirst(row, "Activity ID"));
                    LocalDateTime activityDateTime = parseActivityDateTime(row);

                    if (externalActivityId == null) {
                        skippedCount++;
                        skippedRows.add(new ActivityImportResponse.SkippedRow(rowNumber, "Activity ID 값이 없습니다."));
                        continue;
                    }
                    if (activityDateTime == null) {
                        skippedCount++;
                        skippedRows.add(new ActivityImportResponse.SkippedRow(
                                rowNumber,
                                "Activity Date/Start Time 날짜 파싱에 실패했습니다."
                        ));
                        continue;
                    }

                    // 중복 방지 키: external_activity_id
                    if (activityRepository.existsByExternalActivityId(externalActivityId)) {
                        skippedCount++;
                        skippedRows.add(new ActivityImportResponse.SkippedRow(rowNumber, "이미 적재된 Activity ID 입니다."));
                        continue;
                    }

                    String activityType = trimToNull(getFirst(row, "Activity Type", "Type"));
                    String activityName = trimToNull(getFirst(row, "Activity Name"));

                    Activity activity = Activity.builder()
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

        return new ActivityImportResponse(importedCount, skippedCount, skippedRows);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 파일이 비어 있습니다.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 파일(.csv)만 업로드할 수 있습니다.");
        }
        if (file.getSize() > MAX_CSV_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 파일 크기는 10MB 이하여야 합니다.");
        }
    }

    private void validateHeaders(List<String> headers) {
        if (headers.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "CSV 헤더가 비어 있습니다.");
        }
        if (!headers.contains("Activity ID")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "필수 헤더(Activity ID)가 없습니다.");
        }
        if (!headers.contains("Activity Date") && !headers.contains("Start Time")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "필수 헤더(Activity Date 또는 Start Time)가 없습니다.");
        }
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
        LocalDateTime parsedFromActivityDate = parseDateTimeFlexible(activityDate);
        if (parsedFromActivityDate != null) {
            return parsedFromActivityDate;
        }

        String startTime = trimToNull(getFirst(row, "Start Time"));
        return parseDateTimeFlexible(startTime);
    }

    private LocalDateTime parseDateTimeFlexible(String value) {
        String input = trimToNull(value);
        if (input == null) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : DATE_ONLY_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(input, formatter);
                return date.atStartOfDay();
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

        String normalized = trimmed.replaceAll("[^0-9,\\.\\-+]", "");
        if (normalized.isBlank() || normalized.equals("-") || normalized.equals("+")) {
            return null;
        }

        if (normalized.contains(",") && normalized.contains(".")) {
            // 1,234.56 형태는 콤마를 천 단위 구분자로 처리.
            normalized = normalized.replace(",", "");
        } else if (normalized.contains(",")) {
            // 12,34 형태는 콤마를 소수점으로 처리.
            normalized = normalized.replace(",", ".");
        }

        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
