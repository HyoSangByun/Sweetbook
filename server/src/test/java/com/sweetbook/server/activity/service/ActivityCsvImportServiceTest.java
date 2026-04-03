package com.sweetbook.server.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sweetbook.server.activity.dto.ActivityImportResponse;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.io.IOException;
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
    void personal_csv_중복헤더를_정규화해서_적재한다() throws IOException {
        User user = userRepository.save(User.builder()
                .email("import-test-personal@sweetbook.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .build());

        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/data/strava_personal.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "strava_personal.csv", "text/csv", bytes);

        ActivityImportResponse response = activityCsvImportService.importCsv(user.getId(), file);

        assertThat(response.importedCount()).isGreaterThan(0);
        assertThat(activityRepository.findDistinctMonthsByUserId(user.getId())).isNotEmpty();
    }

    @Test
    void 같은_csv를_다시_적재하면_중복은_건너뛴다() throws IOException {
        User user = userRepository.save(User.builder()
                .email("import-test-duplicate@sweetbook.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .build());

        byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/data/strava_dummy.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "strava_dummy.csv", "text/csv", bytes);

        ActivityImportResponse first = activityCsvImportService.importCsv(user.getId(), file);
        ActivityImportResponse second = activityCsvImportService.importCsv(user.getId(), file);

        assertThat(first.importedCount()).isGreaterThan(0);
        assertThat(second.importedCount()).isZero();
        assertThat(second.skippedCount()).isEqualTo(first.importedCount());
    }
}

