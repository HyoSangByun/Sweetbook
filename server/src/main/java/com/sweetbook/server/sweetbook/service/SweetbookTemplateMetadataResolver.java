package com.sweetbook.server.sweetbook.service;

import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SweetbookTemplateMetadataResolver {

    public Map<String, Object> extractDefinitions(Map<String, Object> templateDetail) {
        Object templateObject = templateDetail.get("template");
        Map<String, Object> templateRoot = templateDetail;
        if (templateObject instanceof Map<?, ?> templateMap) {
            templateRoot = toStringKeyMap(templateMap);
        }

        Object parametersObject = templateRoot.get("parameters");
        if (!(parametersObject instanceof Map<?, ?> parametersMap)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Template parameters metadata is missing.");
        }

        Object definitionsObject = parametersMap.get("definitions");
        if (!(definitionsObject instanceof Map<?, ?> definitionsMap)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Template parameter definitions are missing.");
        }

        return toStringKeyMap(definitionsMap);
    }

    public Set<String> resolveRequiredKeys(Map<String, Object> definitions) {
        Set<String> requiredKeys = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("required"))) {
                requiredKeys.add(entry.getKey());
            }
        }
        return requiredKeys;
    }

    public void putIfTemplateParameterExists(
            Map<String, Object> params,
            Map<String, Object> definitions,
            String key,
            Object value
    ) {
        if (definitions.containsKey(key) && value != null) {
            params.put(key, value);
        }
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }
}
