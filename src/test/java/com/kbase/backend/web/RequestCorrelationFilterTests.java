package com.kbase.backend.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RequestCorrelationFilterTests {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void propagatesSafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "frontend-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        assertEquals("frontend-request-123", response.getHeader(RequestCorrelationFilter.HEADER));
    }

    @Test
    void generatesRequestIdWhenMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, mock(FilterChain.class));
        assertNotNull(response.getHeader(RequestCorrelationFilter.HEADER));
    }
}
