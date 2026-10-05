package com.mall.tester.service;

import com.mall.tester.dto.Dtos;
import com.mall.tester.model.ProductBatch;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

/**
 * 演示数据：数据库为空时初始化批次、备品与不同状态的试用品，
 * 便于启动后即可看到临期/过期/冻结/待销毁/已销毁等各类实物。
 */
@ApplicationScoped
public class SeedService {

    @Inject
    EntityManager em;

    @Inject
    TesterService testerService;

    @Inject
    Logger log;

    @ConfigProperty(name = "tester.seed.enabled", defaultValue = "true")
    boolean seedEnabled;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        // 启动时先执行一次过期巡检
        int removed = testerService.enforceExpiries();
        if (removed > 0) {
            log.infof("启动巡检：%d 件过期试用品已自动下架", removed);
        }
        if (!seedEnabled || LaunchMode.current() == LaunchMode.TEST) {
            return;
        }
        Long batchCount = em.createQuery("select count(b) from ProductBatch b", Long.class)
                .getSingleResult();
        if (batchCount > 0) {
            return;
        }
        log.info("初始化演示数据...");
        LocalDate today = LocalDate.now();

        ProductBatch lancome = batch("B2026-LC-001", "兰蔻小黑瓶精华肌底液 30ml", "兰蔻", "护肤", 6);
        ProductBatch estee = batch("B2026-EL-002", "雅诗兰黛持妆粉底液 30ml", "雅诗兰黛", "彩妆", 12);
        ProductBatch chanel = batch("B2025-CN-003", "香奈儿五号香水 50ml", "香奈儿", "香水", 24);
        ProductBatch dior = batch("B2026-DR-004", "迪奥烈艳蓝金唇膏 999", "迪奥", "彩妆", 12);

        // 未开封备品
        testerService.stockIn(new Dtos.StockInRequest(lancome.id, 3, "库管-王芳"));
        testerService.stockIn(new Dtos.StockInRequest(estee.id, 2, "库管-王芳"));
        testerService.stockIn(new Dtos.StockInRequest(chanel.id, 2, "库管-王芳"));
        testerService.stockIn(new Dtos.StockInRequest(dior.id, 2, "库管-王芳"));

        // 已开封试用品：覆盖 正常 / 临期 / 过期 / 调柜 / 冻结 / 待销毁 / 已销毁
        String expiredCode = issue(lancome, today.minusMonths(7), "A-01");          // 过期（6M PAO）
        issue(lancome, today.minusMonths(5).minusDays(20), "A-02");                 // 临期
        issue(estee, today.minusMonths(2), "B-01");                                 // 正常
        issue(estee, today.minusMonths(11).minusDays(15), "B-02");                  // 临期
        String transferred = issue(chanel, today.minusMonths(1), "C-01");           // 正常，随后调柜
        String frozen = issue(dior, today.minusMonths(3), "D-01");                  // 随后冻结
        String toDestroy = issue(dior, today.minusMonths(4), "D-02");               // 随后下架
        String destroyed = issue(lancome, today.minusMonths(8), "A-03");            // 随后销毁

        // 调柜：仅位置变化，开封日期与效期沿用
        testerService.transfer(transferred, new Dtos.TransferRequest("C-02", "柜长-李敏"));

        // 冻结
        testerService.freeze(frozen, new Dtos.ReasonRequest("顾客投诉疑似污染，待质检", "柜长-李敏"));

        // 下架待销毁
        testerService.remove(toDestroy, new Dtos.ReasonRequest("包装破损", "店员-张倩"));

        // 销毁一件（演示销毁记录与账实核对）
        testerService.remove(destroyed, new Dtos.ReasonRequest("超过卫生效期", "system"));
        testerService.destroy(new Dtos.DestroyRequest(
                List.of(destroyed), 1, "超过卫生效期，按规销毁", "仓管-赵强"));

        // 巡查记录
        testerService.patrol(findCodeByCounter("A-02"),
                new Dtos.PatrolRequest("NEAR_EXPIRY", "临近效期，注意观察", "店员-张倩", today.minusDays(1)));
        testerService.patrol(findCodeByCounter("B-01"),
                new Dtos.PatrolRequest("NORMAL", null, "店员-张倩", today.minusDays(1)));

        // 过期样品在启动巡检后应处于已下架状态；这里再兜底执行一次
        testerService.enforceExpiries();
        log.info("演示数据初始化完成");
    }

    private ProductBatch batch(String no, String name, String brand, String category, int pao) {
        return testerService.createBatch(new Dtos.BatchRequest(
                no, name, brand, category, pao, LocalDate.now().minusMonths(10)));
    }

    private String issue(ProductBatch batch, LocalDate openedDate, String counter) {
        // 先入库一件备品再领用，保证编号连续
        var items = testerService.stockIn(new Dtos.StockInRequest(batch.id, 1, "库管-王芳"));
        String code = items.get(0).itemCode;
        testerService.issue(new Dtos.IssueRequest(code, openedDate, counter, "柜长-李敏"));
        return code;
    }

    private String findCodeByCounter(String counter) {
        return em.createQuery(
                        "select t.itemCode from TesterItem t where t.counterLocation = :c and t.status = 'IN_USE'",
                        String.class)
                .setParameter("c", counter)
                .setMaxResults(1)
                .getSingleResult();
    }
}
