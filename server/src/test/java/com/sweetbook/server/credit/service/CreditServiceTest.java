package com.sweetbook.server.credit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sweetbook.server.credit.dto.ChargeCreditRequest;
import com.sweetbook.server.credit.dto.ChargeCreditResponse;
import com.sweetbook.server.credit.dto.CreditBalanceResponse;
import com.sweetbook.server.sweetbook.client.SweetbookCreditsClient;
import com.sweetbook.server.sweetbook.dto.credits.CreditsBalanceResponseData;
import com.sweetbook.server.sweetbook.dto.credits.SandboxChargeResponseData;
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
    void 잔액_조회_응답을_매핑한다() {
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

    @Test
    void 충전시_idempotencyKey를_자동_생성한다() {
        when(sweetbookCreditsClient.chargeSandboxCredits(anyLong(), anyString(), anyString())).thenReturn(
                new SandboxChargeResponseData("tx_001", 50000L, 150000L, "KRW")
        );

        ChargeCreditResponse response = creditService.chargeSandboxCredits(
                new ChargeCreditRequest(50000L, "테스트 충전", null)
        );

        assertThat(response.transactionUid()).isEqualTo("tx_001");
        assertThat(response.amount()).isEqualTo(50000L);
        assertThat(response.balanceAfter()).isEqualTo(150000L);
        verify(sweetbookCreditsClient).chargeSandboxCredits(anyLong(), anyString(), anyString());
    }
}
