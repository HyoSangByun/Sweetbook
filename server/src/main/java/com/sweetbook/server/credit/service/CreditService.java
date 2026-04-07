package com.sweetbook.server.credit.service;

import com.sweetbook.server.credit.dto.CreditBalanceResponse;
import com.sweetbook.server.sweetbook.client.SweetbookCreditsClient;
import com.sweetbook.server.sweetbook.dto.credits.CreditsBalanceResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final SweetbookCreditsClient sweetbookCreditsClient;

    public CreditBalanceResponse getCreditsBalance() {
        CreditsBalanceResponseData data = sweetbookCreditsClient.getCreditsBalance();
        return new CreditBalanceResponse(
                data.accountUid(),
                data.balance(),
                data.currency(),
                data.env(),
                data.createdAt() == null ? null : data.createdAt().toLocalDateTime(),
                data.updatedAt() == null ? null : data.updatedAt().toLocalDateTime()
        );
    }
}
