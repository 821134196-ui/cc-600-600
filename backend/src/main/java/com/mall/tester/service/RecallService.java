package com.mall.tester.service;

import com.mall.tester.dto.Dtos;
import com.mall.tester.model.ProductBatch;
import com.mall.tester.model.RecallNotice;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 召回同步：从本地模拟接口拉取召回通知，落库并触发批次召回处理。
 */
@ApplicationScoped
public class RecallService {

    @Inject
    EntityManager em;

    @Inject
    TesterService testerService;

    @ConfigProperty(name = "tester.recall.mock-base-url")
    String mockBaseUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 从本地模拟接口拉取召回通知并生效。 */
    @Transactional
    public Dtos.RecallSyncResult syncFromMock() {
        List<RecallNotice> fetched = fetchNotices();
        int newNotices = 0, batchesMarked = 0, itemsRemoved = 0, stockFrozen = 0;
        for (RecallNotice notice : fetched) {
            Long exists = em.createQuery(
                            "select count(r) from RecallNotice r where r.noticeNo = :no", Long.class)
                    .setParameter("no", notice.noticeNo).getSingleResult();
            if (exists > 0) {
                continue; // 幂等：已同步过的通知跳过
            }
            em.persist(notice);
            newNotices++;
            List<ProductBatch> batches = em.createQuery(
                            "select b from ProductBatch b where b.batchNo = :no", ProductBatch.class)
                    .setParameter("no", notice.batchNo).getResultList();
            for (ProductBatch batch : batches) {
                if (!batch.recalled) {
                    batchesMarked++;
                }
                int[] result = testerService.applyRecall(batch, notice.reason, notice.noticeDate);
                itemsRemoved += result[0];
                stockFrozen += result[1];
            }
        }
        return new Dtos.RecallSyncResult(fetched.size(), newNotices, batchesMarked,
                itemsRemoved, stockFrozen);
    }

    public List<RecallNotice> listSynced() {
        return em.createQuery("select r from RecallNotice r order by r.syncedAt desc", RecallNotice.class)
                .getResultList();
    }

    /** 调用本地模拟接口（模拟外部召回系统） */
    List<RecallNotice> fetchNotices() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mockBaseUrl + "/recall-notices"))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw BizException.badRequest("召回模拟接口响应异常: HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            List<RecallNotice> result = new java.util.ArrayList<>();
            for (JsonNode node : root) {
                RecallNotice n = new RecallNotice();
                n.noticeNo = node.get("noticeNo").asText();
                n.batchNo = node.get("batchNo").asText();
                n.productName = node.hasNonNull("productName") ? node.get("productName").asText() : null;
                n.reason = node.hasNonNull("reason") ? node.get("reason").asText() : null;
                n.noticeDate = node.hasNonNull("noticeDate")
                        ? LocalDate.parse(node.get("noticeDate").asText()) : null;
                n.source = node.hasNonNull("source") ? node.get("source").asText() : "mock";
                result.add(n);
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.badRequest("无法连接召回模拟接口: " + e.getMessage());
        }
    }
}
