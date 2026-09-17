package com.kbase.backend.rag.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Deterministic local response generator for development/tests only; it is not an LLM. */
@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "dev", matchIfMissing = true)
public class DeterministicDevLlmService implements LlmService {

    private final LlmProperties properties;

    public DeterministicDevLlmService(LlmProperties properties) {
        this.properties = properties;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (request.sources().isEmpty()) {
            throw new IllegalArgumentException("DEV provider requires document context");
        }
        var source = request.sources().getFirst();
        String excerpt = source.content().length() <= 240
                ? source.content()
                : source.content().substring(0, 240);
        String answer = "Based on " + source.documentTitle() + ": " + excerpt;
        return new LlmResponse(answer, properties.model(), approximateTokens(request),
                Math.max(1, (answer.length() + 3) / 4));
    }

    private int approximateTokens(LlmRequest request) {
        int characters = request.systemPrompt().length()
                + request.messages().stream().mapToInt(message -> message.content().length()).sum();
        return Math.max(1, (characters + 3) / 4);
    }
}
