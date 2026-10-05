package com.mallhygiene.service;

import com.mallhygiene.domain.InspectionResult;
import com.mallhygiene.domain.Product;
import com.mallhygiene.dto.Dtos.ItemView;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

/**
 * 演示数据：首次启动（库为空）时写入。
 * 覆盖各种状态：备品 / 在用 / 临期 / 过期(待扫描下架) / 冻结 / 待销毁 / 已销毁 / 召回批次。
 */
@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);

    @ConfigProperty(name = "app.seed", defaultValue = "true")
    boolean seed;

    @Inject
    TesterService testerService;

    @Transactional
    void onStart(@Observes @Priority(1000) StartupEvent event) {
        if (!seed || Product.count() > 0) {
            return;
        }
        LOG.info("写入演示数据...");
        LocalDate today = LocalDate.now();

        Product lancome = product("兰蔻小黑瓶精华 30ml", "LANCOME", "LK-2026-0312",
                today.minusMonths(7), 36, 6);
        Product estee = product("雅诗兰黛持妆粉底液", "ESTEE LAUDER", "EL-2025-1108",
                today.minusMonths(11), 36, 12);
        Product shiseido = product("资生堂红腰子精华 50ml", "SHISEIDO", "SH-2026-0520",
                today.minusMonths(5), 36, 9);
        Product dior = product("迪奥烈艳蓝金唇膏 999", "DIOR", "DR-2026-0130",
                today.minusMonths(9), 36, 12);
        Product chanel = product("香奈儿蔚蓝香水 50ml", "CHANEL", "CH-2024-0915",
                today.minusMonths(25), 60, 24);

        // 未开封备品（T-00004 为召回批次备品，同步召回后会被下架）
        stockIn("T-00001", lancome);
        stockIn("T-00002", shiseido);
        stockIn("T-00003", dior);
        stockIn("T-00004", chanel);

        // 已开封在用
        issue("T-00005", lancome, today.minusDays(3), "A-01");                 // 刚开封
        issue("T-00006", lancome, today.minusMonths(6).plusDays(5), "A-02");   // 临期（5天后到期）
        issue("T-00007", shiseido, today.minusMonths(2), "B-01");
        issue("T-00008", dior, today.minusMonths(12).plusDays(2), "B-02");     // 临期（2天后到期）
        issue("T-00009", estee, today.minusMonths(13), "C-01");                // 已过期，启动扫描自动下架
        issue("T-00010", chanel, today.minusMonths(1), "C-02");                // 召回批次在用

        // 调柜示例：B-01 → B-05，开封日期与效期不变
        testerService.transfer("T-00007", "B-05", "赵倩");

        // 巡查示例
        testerService.inspect("T-00006", InspectionResult.NEAR_EXPIRY, "王敏", "瓶口略有干涸，注意观察");
        testerService.inspect("T-00007", InspectionResult.NORMAL, "王敏", null);

        // 冻结示例
        stockIn("T-00011", estee);
        testerService.freeze("T-00011", "顾客投诉疑似异味，待复检", "李芳");

        // 已下架待销毁
        issue("T-00012", lancome, today.minusMonths(7), "A-03");
        testerService.remove("T-00012", "EXPIRED", "system");

        // 已销毁（销毁批次 D-00001，两件）
        ItemView d1 = issue("T-00013", dior, today.minusMonths(14), "B-03");
        ItemView d2 = issue("T-00014", estee, today.minusMonths(15), "C-03");
        testerService.remove("T-00013", "EXPIRED", "system");
        testerService.remove("T-00014", "EXPIRED", "system");
        testerService.destroy(List.of(d1.id(), d2.id()), "到期集中销毁", "王敏");

        LOG.infof("演示数据就绪：%d 件试用品", com.mallhygiene.domain.TesterItem.count());
    }

    private Product product(String name, String brand, String batchNo, LocalDate productionDate,
                            int shelfMonths, int paoMonths) {
        Product p = new Product();
        p.name = name;
        p.brand = brand;
        p.batchNo = batchNo;
        p.productionDate = productionDate;
        p.shelfMonths = shelfMonths;
        p.paoMonths = paoMonths;
        p.persist();
        return p;
    }

    private void stockIn(String code, Product product) {
        testerService.stockIn(code, product.id, "system");
    }

    private ItemView issue(String code, Product product, LocalDate openedDate, String counter) {
        ItemView view = testerService.stockIn(code, product.id, "system");
        return testerService.issue(view.code(), openedDate, counter, "system");
    }
}
