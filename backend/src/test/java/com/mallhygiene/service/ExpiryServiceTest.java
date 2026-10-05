package com.mallhygiene.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 效期计算单元测试 */
class ExpiryServiceTest {

    private ExpiryService service;

    @BeforeEach
    void setUp() {
        service = new ExpiryService();
        service.nearExpiryDays = 7;
    }

    @Test
    void expiryIsOpenedDatePlusPaoMonths() {
        // 开封 2026-04-09 + 6 个月 = 2026-10-09
        assertEquals(LocalDate.of(2026, 10, 9),
                service.computeExpiry(LocalDate.of(2026, 4, 9), 6));
        // 12 个月跨年
        assertEquals(LocalDate.of(2027, 4, 9),
                service.computeExpiry(LocalDate.of(2026, 4, 9), 12));
        // 月末对齐：1/31 + 1 个月 = 2/28（2026 非闰年）
        assertEquals(LocalDate.of(2026, 2, 28),
                service.computeExpiry(LocalDate.of(2026, 1, 31), 1));
        assertNull(service.computeExpiry(null, 6));
    }

    @Test
    void expiredOnlyAfterExpiryDate() {
        LocalDate expiry = LocalDate.of(2026, 10, 9);
        // 到期日当天仍可用
        assertFalse(service.isExpired(expiry, LocalDate.of(2026, 10, 9)));
        // 次日才算过期
        assertTrue(service.isExpired(expiry, LocalDate.of(2026, 10, 10)));
        assertFalse(service.isExpired(null, LocalDate.of(2026, 10, 10)));
    }

    @Test
    void nearExpiryWithinSevenDays() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        // 6 天后到期：临期
        assertTrue(service.isNearExpiry(LocalDate.of(2026, 10, 10), today));
        // 恰好 7 天：临期
        assertTrue(service.isNearExpiry(LocalDate.of(2026, 10, 11), today));
        // 8 天：未临期
        assertFalse(service.isNearExpiry(LocalDate.of(2026, 10, 12), today));
        // 已过期不算临期
        assertFalse(service.isNearExpiry(LocalDate.of(2026, 10, 1), today));
        // 到期日当天：临期
        assertTrue(service.isNearExpiry(LocalDate.of(2026, 10, 4), today));
    }
}
