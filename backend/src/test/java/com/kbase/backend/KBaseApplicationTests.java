package com.kbase.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KBaseApplicationTests {

    @Test
    void applicationEntryPointExists() {
        assertNotNull(KBaseApplication.class);
    }
}
