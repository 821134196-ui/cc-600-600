package com.mallhygiene.service;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/** 效期扫描：启动时与每小时把已过期的在用试用品自动下架 */
@ApplicationScoped
public class ExpiryScanJob {

    private static final Logger LOG = Logger.getLogger(ExpiryScanJob.class);

    @Inject
    TesterService testerService;

    void onStart(@Observes StartupEvent event) {
        scan();
    }

    @Scheduled(every = "1h", identity = "expiry-scan")
    void hourly() {
        scan();
    }

    private void scan() {
        try {
            int n = testerService.removeExpiredItems();
            if (n > 0) {
                LOG.infof("效期扫描：%d 件过期试用品已自动下架", n);
            }
        } catch (Exception e) {
            LOG.warn("效期扫描失败: " + e.getMessage());
        }
    }
}
