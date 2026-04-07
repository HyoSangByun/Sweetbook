package com.sweetbook.server.credit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sweetbook.server.credit.dto.CreditBalanceResponse;
import com.sweetbook.server.sweetbook.client.SweetbookCreditsClient;
import com.sweetbook.server.sweetbook.dto.credits.CreditsBalanceResponseData;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private SweetbookCreditsClient sweetbookCreditsClient;

    @InjectMocks
    private CreditService creditService;

    @Test
    void 잔액_응답을_매핑한다() {
        OffsetDateTime expectedCreatedAt = OffsetDateTime.parse("2026-04-07T00:00:00Z");
        OffsetDateTime expectedUpdatedAt = OffsetDateTime.parse("2026-04-07T01:00:00Z");

        when(sweetbookCreditsClient.getCreditsBalance()).thenReturn(
                new CreditsBalanceResponseData(
                        "acc_123",
                        100000L,
                        "KRW",
                        "test",
                        expectedCreatedAt,
                        expectedUpdatedAt
                )
        );

        CreditBalanceResponse response = creditService.getCreditsBalance();

        assertThat(response.accountUid()).isEqualTo("acc_123");
        assertThat(response.balance()).isEqualTo(100000L);
        assertThat(response.currency()).isEqualTo("KRW");
        assertThat(response.env()).isEqualTo("test");
        assertThat(response.createdAt()).isEqualTo(expectedCreatedAt.toLocalDateTime());
        assertThat(response.updatedAt()).isEqualTo(expectedUpdatedAt.toLocalDateTime());
    }
}
