package com.mall.tester.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * 定时巡检：过期试用品自动下架。
 */
@ApplicationScoped
public class ExpiryScheduler {

    @Inject
    TesterService testerService;

    @Inject
    Logger log;

    @Scheduled(every = "1h", identity = "expiry-enforcement")
    void enforce() {
        int removed = testerService.enforceExpiries();
        if (removed > 0) {
            log.infof("定时巡检：%d 件过期试用品已自动下架", removed);
        }
    }
}
