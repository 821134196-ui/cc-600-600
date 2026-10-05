package com.mallhygiene.service;

import com.mallhygiene.domain.EventType;
import com.mallhygiene.domain.ItemStatus;
import com.mallhygiene.domain.RecallNotice;
import com.mallhygiene.domain.TesterItem;
import com.mallhygiene.dto.Dtos.MockRecall;
import com.mallhygiene.dto.Dtos.SyncResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;

/** 召回同步：从本地模拟接口拉取通知，涉及批次的样品立即下架 */
@ApplicationScoped
public class RecallService {

    @Inject
    @RestClient
    RecallMockClient client;

    @Inject
    TesterService testerService;

    /** 从本地模拟接口拉取最新召回通知 */
    public List<MockRecall> fetchRemote() {
        return client.list();
    }

    /** 落库通知并下架涉及批次的全部在库/在用/冻结样品 */
    @Transactional
    public SyncResult applyNotices(List<MockRecall> remote) {
        int created = 0;
        int flagged = 0;
        for (MockRecall m : remote) {
            RecallNotice notice = RecallNotice.find("noticeCode", m.noticeCode()).firstResult();
            if (notice == null) {
                notice = new RecallNotice();
                notice.noticeCode = m.noticeCode();
                created++;
            }
            notice.batchNo = m.batchNo();
            notice.productName = m.productName();
            notice.reason = m.reason();
            notice.publishedAt = m.publishedAt();
            notice.source = m.source();
            notice.syncedAt = LocalDateTime.now();
            notice.persist();

            List<TesterItem> affected = TesterItem.find(
                    "product.batchNo = ?1 and status in (?2, ?3, ?4)",
                    m.batchNo(), ItemStatus.SEALED, ItemStatus.IN_USE, ItemStatus.FROZEN).list();
            for (TesterItem item : affected) {
                testerService.doRemove(item, "RECALLED", "recall-sync",
                        "召回下架 " + m.noticeCode() + "：" + m.reason());
                flagged++;
            }
        }
        return new SyncResult(remote.size(), created, flagged);
    }
}
