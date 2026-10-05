package com.mall.tester.service;

import com.mall.tester.model.TesterItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 卫生效期计算。
 * 规则：效期 = 开封日期 + PAO 月数；开封当天起算，效期当日 24:00 前可用。
 * 调柜等位置变更不参与计算 —— 只认开封日期。
 */
@ApplicationScoped
public class ExpiryService {

    @ConfigProperty(name = "tester.near-expiry-days", defaultValue = "30")
    int nearExpiryDays;

    /** 效期截止日 = 开封日期 + PAO 月数 */
    public LocalDate expiryDate(LocalDate openedDate, int paoMonths) {
        if (openedDate == null) {
            return null;
        }
        return openedDate.plusMonths(paoMonths);
    }

    /** 距效期剩余天数；today 晚于截止日为负数（已过期） */
    public long daysLeft(LocalDate openedDate, int paoMonths, LocalDate today) {
        LocalDate expiry = expiryDate(openedDate, paoMonths);
        if (expiry == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(today, expiry);
    }

    /** 是否已过期：当前日期晚于效期截止日 */
    public boolean isExpired(LocalDate openedDate, int paoMonths, LocalDate today) {
        return daysLeft(openedDate, paoMonths, today) < 0;
    }

    /** 是否临期：未过期，但剩余天数不超过阈值 */
    public boolean isNearExpiry(LocalDate openedDate, int paoMonths, LocalDate today) {
        long left = daysLeft(openedDate, paoMonths, today);
        return left >= 0 && left <= nearExpiryDays;
    }

    public int nearExpiryDays() {
        return nearExpiryDays;
    }

    /** 便捷方法：直接对试用品计算 */
    public LocalDate expiryDateOf(TesterItem item) {
        return expiryDate(item.openedDate, item.batch.paoMonths);
    }
}
