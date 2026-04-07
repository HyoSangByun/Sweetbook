package com.sweetbook.server.sweetbook.service;

import com.sweetbook.server.sweetbook.client.SweetbookBooksClient;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SweetbookCatalogService {

    private final SweetbookBooksClient sweetbookBooksClient;

    public List<Map<String, Object>> getBookSpecs() {
        return sweetbookBooksClient.getBookSpecs();
    }

    public List<Map<String, Object>> getTemplates(String bookSpecUid, String templateKind) {
        return sweetbookBooksClient.getTemplates(bookSpecUid, templateKind);
    }

    public Map<String, Object> getTemplateDetail(String templateUid) {
        return sweetbookBooksClient.getTemplateDetail(templateUid);
    }
}
