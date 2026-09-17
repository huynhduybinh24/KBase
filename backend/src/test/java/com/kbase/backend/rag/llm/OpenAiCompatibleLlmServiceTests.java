package com.kbase.backend.rag.llm;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleLlmServiceTests {

    @Test
    void createsCompatibleRequestAndMapsUsageWithoutExposingKey() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://provider.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LlmProperties properties = properties();
        OpenAiCompatibleLlmService service = new OpenAiCompatibleLlmService(
                builder.defaultHeader("Authorization", "Bearer test-secret").build(), properties);
        server.expect(once(), requestTo("http://provider.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-secret"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("chat-model"),
                        org.hamcrest.Matchers.containsString("max_tokens"),
                        org.hamcrest.Matchers.containsString("system"))))
                .andRespond(withSuccess("""
                        {"model":"provider-model","choices":[{"message":{"role":"assistant","content":"Grounded answer"}}],
                         "usage":{"prompt_tokens":12,"completion_tokens":3}}
                        """, MediaType.APPLICATION_JSON));

        LlmResponse response = service.generate(request());

        assertEquals("Grounded answer", response.answer());
        assertEquals(12, response.inputTokenCount());
        assertEquals(3, response.outputTokenCount());
        server.verify();
    }

    @Test
    void safelyWrapsNonSuccessAndMalformedResponses() {
        RestClient.Builder errorBuilder = RestClient.builder().baseUrl("http://provider.test");
        MockRestServiceServer errorServer = MockRestServiceServer.bindTo(errorBuilder).build();
        var errorService = new OpenAiCompatibleLlmService(errorBuilder.build(), properties());
        errorServer.expect(requestTo("http://provider.test/chat/completions"))
                .andRespond(withServerError().body("provider internal secret"));
        assertThrows(LlmUnavailableException.class, () -> errorService.generate(request()));

        RestClient.Builder malformedBuilder = RestClient.builder().baseUrl("http://provider.test");
        MockRestServiceServer malformedServer = MockRestServiceServer.bindTo(malformedBuilder).build();
        var malformedService = new OpenAiCompatibleLlmService(
                malformedBuilder.build(), properties());
        malformedServer.expect(requestTo("http://provider.test/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(LlmUnavailableException.class, () -> malformedService.generate(request()));
    }

    private LlmProperties properties() {
        return new LlmProperties(
                "openai-compatible", "http://provider.test", "test-secret",
                "chat-model", 0.1, 123, 2);
    }

    private LlmRequest request() {
        return new LlmRequest("system", List.of(new LlmMessage("user", "question")), List.of());
    }
}
