package com.mallhygiene.service;

import com.mallhygiene.domain.ItemStatus;
import com.mallhygiene.domain.Product;
import com.mallhygiene.domain.TesterItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;

/**
 * 效期计算：一切以“实际开封日期 + 开封后使用月数(PAO)”为准。
 * 调柜只改柜位，不回写开封日期，因此效期天然保持不变。
 */
@ApplicationScoped
public class ExpiryService {

    @ConfigProperty(name = "app.near-expiry-days", defaultValue = "7")
    int nearExpiryDays;

    /** 到期日 = 开封日期 + PAO 月数（月末自动对齐，如 1/31 + 1月 = 2/28） */
    public LocalDate computeExpiry(LocalDate openedDate, int paoMonths) {
        if (openedDate == null) {
            return null;
        }
        return openedDate.plusMonths(paoMonths);
    }

    /** 到期日当天仍可使用，次日才算过期 */
    public boolean isExpired(LocalDate expiryDate, LocalDate today) {
        return expiryDate != null && today.isAfter(expiryDate);
    }

    /** 距到期日不足阈值（含当天）且尚未过期即为临期 */
    public boolean isNearExpiry(LocalDate expiryDate, LocalDate today) {
        return expiryDate != null
                && !isExpired(expiryDate, today)
                && !today.plusDays(nearExpiryDays).isBefore(expiryDate);
    }

    /** 在用试用品的实时卫生状态：EXPIRED / NEAR_EXPIRY / NORMAL */
    public String conditionOf(TesterItem item, LocalDate today) {
        if (item.status == ItemStatus.IN_USE) {
            if (isExpired(item.expiryDate, today)) {
                return "EXPIRED";
            }
            if (isNearExpiry(item.expiryDate, today)) {
                return "NEAR_EXPIRY";
            }
        }
        return "NORMAL";
    }

    /** 未开封备品效期 = 生产日期 + 未开封保质期 */
    public LocalDate sealedExpiry(Product product) {
        if (product == null || product.productionDate == null) {
            return null;
        }
        return product.productionDate.plusMonths(product.shelfMonths);
    }
}
