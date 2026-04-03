package com.sweetbook.server.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sweetbook.server.activity.dto.ActivityImportResponse;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ActivityCsvImportServiceTest {

    @Autowired
    private ActivityCsvImportService activityCsvImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    void personal_csv_중복헤더를_정규화해_적재한다() throws IOException {
        User user = userRepository.save(newUser("import-test-personal@sweetbook.com"));

        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/data/strava_personal.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "strava_personal.csv", "text/csv", bytes);

        ActivityImportResponse response = activityCsvImportService.importCsv(user.getId(), file);

        assertThat(response.importedCount()).isGreaterThan(0);
        assertThat(activityRepository.findDistinctMonthsByUserId(user.getId())).isNotEmpty();
    }

    @Test
    void 같은_csv를_다시_적재하면_중복은_건너뛴다() throws IOException {
        User user = userRepository.save(newUser("import-test-duplicate@sweetbook.com"));

        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/data/strava_dummy.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "strava_dummy.csv", "text/csv", bytes);

        ActivityImportResponse first = activityCsvImportService.importCsv(user.getId(), file);
        ActivityImportResponse second = activityCsvImportService.importCsv(user.getId(), file);

        assertThat(first.importedCount()).isGreaterThan(0);
        assertThat(second.importedCount()).isZero();
        assertThat(second.skippedCount()).isEqualTo(first.importedCount());
        assertThat(second.skippedRows()).isNotEmpty();
    }

    @Test
    void csv_확장자가_아니면_검증_실패한다() {
        User user = userRepository.save(newUser("import-test-invalid-ext@sweetbook.com"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "not-csv.txt",
                "text/plain",
                "Activity ID,Activity Date\nA1,2026-04-01".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> activityCsvImportService.importCsv(user.getId(), file))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                });
    }

    @Test
    void 다양한_날짜_숫자_포맷을_파싱한다() {
        User user = userRepository.save(newUser("import-test-format@sweetbook.com"));

        String csv = String.join("\n",
                "Activity ID,Activity Date,Activity Name,Activity Type,Distance,Moving Time",
                "A-1,2026-04-01,ISO Date Only,Run,\"1,23\",1200",
                "A-2,\"Apr 1, 2026, 9:16 AM\",US Date,Walk,\"1,234.56\",600"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "format.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ActivityImportResponse response = activityCsvImportService.importCsv(user.getId(), file);

        assertThat(response.importedCount()).isEqualTo(2);
        assertThat(response.skippedCount()).isZero();
    }

    private User newUser(String email) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .role(UserRole.USER)
                .build();
    }
}

