package com.mall.tester;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 召回流程：本地模拟接口 -> 同步 -> 批次召回 -> 在用样品立即下架、备品冻结。
 */
@QuarkusTest
@TestProfile(TestProfiles.Recall.class)
class RecallFlowTest {

    private static final Path MOCK_FILE = Path.of("target", "test-recall-notices.json");

    @BeforeAll
    static void prepareMockApiData() throws IOException {
        Files.createDirectories(MOCK_FILE.getParent());
        Files.writeString(MOCK_FILE, """
                [
                  {
                    "noticeNo": "RC-TEST-001",
                    "batchNo": "T-RECALL-001",
                    "productName": "召回测试面霜 50ml",
                    "reason": "微生物超标",
                    "noticeDate": "2026-10-01",
                    "source": "模拟召回系统"
                  }
                ]
                """);
    }

    @Test
    void recallSyncRemovesInUseAndFreezesStock() {
        // 准备：批次 + 1 件备品冻结候选 + 1 件在用
        Long batchId = given().contentType(ContentType.JSON)
                .body(Map.of("batchNo", "T-RECALL-001", "productName", "召回测试面霜 50ml",
                        "paoMonths", 12))
                .when().post("/api/batches")
                .then().statusCode(200)
                .extract().jsonPath().getLong("id");

        List<String> codes = given().contentType(ContentType.JSON)
                .body(Map.of("batchId", batchId, "count", 2, "operator", "库管"))
                .when().post("/api/testers/stock-in")
                .then().extract().jsonPath().getList("itemCode", String.class);
        String inUseCode = codes.get(0);
        String stockCode = codes.get(1);
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", inUseCode,
                        "openedDate", LocalDate.now().minusMonths(1).toString(),
                        "counterLocation", "R-01", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(200);

        // 同步召回通知
        given().post("/api/recalls/sync")
                .then().statusCode(200)
                .body("fetched", is(1))
                .body("newNotices", is(1))
                .body("batchesMarked", is(1))
                .body("itemsRemoved", is(1))
                .body("stockFrozen", is(1));

        // 涉及召回的样品应立即下架
        given().get("/api/testers/{code}", inUseCode)
                .then().body("status", is("REMOVED"))
                .body("recalled", is(true));
        // 未开封备品应被冻结，不得领用
        given().get("/api/testers/{code}", stockCode)
                .then().body("status", is("FROZEN"));
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", stockCode,
                        "openedDate", LocalDate.now().toString(),
                        "counterLocation", "R-02", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(409);

        // 再次同步幂等：不产生新通知、不重复处理
        given().post("/api/recalls/sync")
                .then().statusCode(200)
                .body("newNotices", is(0))
                .body("itemsRemoved", is(0))
                .body("stockFrozen", is(0));

        // 召回通知可查询
        given().get("/api/recalls")
                .then().statusCode(200)
                .body("find { it.noticeNo == 'RC-TEST-001' }.batchNo", is("T-RECALL-001"));

        // 召回下架原因写入追溯事件
        List<String> types = given().get("/api/testers/{code}/events", inUseCode)
                .then().extract().jsonPath().getList("eventType", String.class);
        org.junit.jupiter.api.Assertions.assertTrue(types.contains("RECALL_REMOVED"));
    }
}
