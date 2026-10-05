package com.mall.tester.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 效期计算规则：
 * 效期 = 开封日期 + PAO 月数；截止日当天仍可用，次日视为过期；
 * 剩余天数 <= 阈值（30 天）且未过期为临期。
 */
class ExpiryServiceTest {

    private ExpiryService service;

    @BeforeEach
    void setUp() {
        service = new ExpiryService();
        service.nearExpiryDays = 30;
    }

    @Test
    void expiryDateIsOpenedDatePlusPaoMonths() {
        assertEquals(LocalDate.of(2026, 7, 15),
                service.expiryDate(LocalDate.of(2026, 1, 15), 6));
        assertEquals(LocalDate.of(2027, 1, 15),
                service.expiryDate(LocalDate.of(2026, 1, 15), 12));
    }

    @Test
    void expiryDateHandlesMonthEndAndYearCrossing() {
        // 1月31日 + 1个月 -> 2月28日（平年），不溢出到3月
        assertEquals(LocalDate.of(2025, 2, 28),
                service.expiryDate(LocalDate.of(2025, 1, 31), 1));
        // 跨年：2026-11-20 + 3M -> 2027-02-20
        assertEquals(LocalDate.of(2027, 2, 20),
                service.expiryDate(LocalDate.of(2026, 11, 20), 3));
    }

    @Test
    void unopenedItemHasNoExpiry() {
        assertNull(service.expiryDate(null, 6));
        assertFalse(service.isExpired(null, 6, LocalDate.now()));
        assertFalse(service.isNearExpiry(null, 6, LocalDate.now()));
    }

    @Test
    void expiredOnlyAfterExpiryDate() {
        LocalDate opened = LocalDate.of(2026, 1, 15); // 效期至 2026-07-15
        assertFalse(service.isExpired(opened, 6, LocalDate.of(2026, 7, 15)), "截止日当天仍可用");
        assertTrue(service.isExpired(opened, 6, LocalDate.of(2026, 7, 16)), "次日即过期");
    }

    @Test
    void nearExpiryWithinThreshold() {
        LocalDate opened = LocalDate.of(2026, 1, 1); // 效期 2026-07-01
        assertTrue(service.isNearExpiry(opened, 6, LocalDate.of(2026, 6, 1)), "恰好剩30天为临期");
        assertTrue(service.isNearExpiry(opened, 6, LocalDate.of(2026, 7, 1)), "截止日当天仍属临期");
        assertFalse(service.isNearExpiry(opened, 6, LocalDate.of(2026, 5, 31)), "剩31天不算临期");
        assertFalse(service.isNearExpiry(opened, 6, LocalDate.of(2026, 7, 2)), "已过期不再算临期");
    }

    @Test
    void daysLeftIsExact() {
        LocalDate opened = LocalDate.of(2026, 3, 10); // 效期 2026-09-10
        assertEquals(10, service.daysLeft(opened, 6, LocalDate.of(2026, 8, 31)));
        assertEquals(0, service.daysLeft(opened, 6, LocalDate.of(2026, 9, 10)));
        assertEquals(-1, service.daysLeft(opened, 6, LocalDate.of(2026, 9, 11)));
    }
}
