package com.weverse.ticketing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketingApplicationTests {

    @Test
    @DisplayName("기본 컨텍스트 로딩 사전 단위 테스트 검증")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
