package com.kbase.backend.rag.llm;

import com.kbase.backend.rag.retrieval.SemanticSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicDevLlmServiceTests {

    @Test
    void deterministicallyAnswersFromFirstContextWithoutExternalCalls() {
        LlmProperties properties = new LlmProperties(
                "dev", "", "", "dev-test", 0, 100, 5);
        DeterministicDevLlmService service = new DeterministicDevLlmService(properties);
        var source = new SemanticSearchResult(
                UUID.randomUUID(), UUID.randomUUID(), "Guide", 0,
                "JWT authentication validates a signed bearer token.", 0.9);
        LlmRequest request = new LlmRequest("system",
                List.of(new LlmMessage("user", "question")), List.of(source));

        LlmResponse first = service.generate(request);
        LlmResponse second = service.generate(request);

        assertEquals(first, second);
        assertTrue(first.answer().contains("JWT authentication"));
        assertEquals("dev-test", first.model());
    }
}
