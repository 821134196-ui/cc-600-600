package com.mall.tester;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 全流程集成测试：批次 -> 入库 -> 领用 -> 调柜 -> 冻结 -> 巡查 -> 销毁 -> 追溯。
 */
@QuarkusTest
@TestProfile(TestProfiles.Flow.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TesterFlowTest {

    private static Long batchId;
    private static String code1;
    private static String code2;
    private static final LocalDate OPENED = LocalDate.now().minusMonths(1);

    @Test
    @Order(1)
    void createBatch() {
        batchId = given().contentType(ContentType.JSON)
                .body(Map.of("batchNo", "T-BATCH-001", "productName", "测试精华 30ml",
                        "brand", "测试品牌", "category", "护肤", "paoMonths", 6,
                        "productionDate", "2026-01-01"))
                .when().post("/api/batches")
                .then().statusCode(200)
                .body("batchNo", is("T-BATCH-001"))
                .extract().jsonPath().getLong("id");
        assertNotNull(batchId);
    }

    @Test
    @Order(2)
    void duplicateBatchRejected() {
        given().contentType(ContentType.JSON)
                .body(Map.of("batchNo", "T-BATCH-001", "productName", "重复批次", "paoMonths", 6))
                .when().post("/api/batches")
                .then().statusCode(409);
    }

    @Test
    @Order(3)
    void stockInCreatesUnopenedItems() {
        List<String> codes = given().contentType(ContentType.JSON)
                .body(Map.of("batchId", batchId, "count", 2, "operator", "库管"))
                .when().post("/api/testers/stock-in")
                .then().statusCode(200)
                .body("size()", is(2))
                .body("status", everyItem(is("UNOPENED")))
                .extract().jsonPath().getList("itemCode", String.class);
        assertEquals(2, codes.size());
        assertNotEquals(codes.get(0), codes.get(1), "每件试用品编号必须独立");
        code1 = codes.get(0);
        code2 = codes.get(1);

        // 未开封与已开封分开统计
        given().get("/api/testers/stats")
                .then().body("unopened", is(2)).body("inUse", is(0));
    }

    @Test
    @Order(4)
    void issueRegistersBatchOpenedDateAndCounter() {
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code1, "openedDate", OPENED.toString(),
                        "counterLocation", "A-01", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(200)
                .body("status", is("IN_USE"))
                .body("openedDate", is(OPENED.toString()))
                .body("counterLocation", is("A-01"))
                .body("batchNo", is("T-BATCH-001"))
                .body("expiryDate", is(OPENED.plusMonths(6).toString()));

        given().get("/api/testers/stats")
                .then().body("unopened", is(1)).body("inUse", is(1));
    }

    @Test
    @Order(5)
    void futureOpenedDateRejected() {
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code2, "openedDate", LocalDate.now().plusDays(1).toString(),
                        "counterLocation", "A-02", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(400);
    }

    @Test
    @Order(6)
    void transferKeepsOpenedDateAndExpiry() {
        // 调柜仅改变柜位，开封日期与效期必须沿用
        given().contentType(ContentType.JSON)
                .body(Map.of("counterLocation", "B-02", "operator", "柜长"))
                .when().post("/api/testers/{code}/transfer", code1)
                .then().statusCode(200)
                .body("counterLocation", is("B-02"))
                .body("openedDate", is(OPENED.toString()))
                .body("expiryDate", is(OPENED.plusMonths(6).toString()));

        // 事件链中应有调柜记录，且注明开封日期不变
        List<String> types = given().get("/api/testers/{code}/events", code1)
                .then().statusCode(200)
                .extract().jsonPath().getList("eventType", String.class);
        assertTrue(types.containsAll(List.of("STOCK_IN", "ISSUED", "TRANSFERRED")));
    }

    @Test
    @Order(7)
    void frozenItemCannotBeIssuedAgain() {
        given().contentType(ContentType.JSON)
                .body(Map.of("reason", "疑似污染待检", "operator", "柜长"))
                .when().post("/api/testers/{code}/freeze", code1)
                .then().statusCode(200).body("status", is("FROZEN"));

        // 冻结后不得再次领用
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code1, "openedDate", OPENED.toString(),
                        "counterLocation", "C-01", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(409)
                .body("error", containsString("冻结"));
    }

    @Test
    @Order(8)
    void patrolContaminationRemovesItem() {
        // 先领用第二件
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code2, "openedDate", OPENED.toString(),
                        "counterLocation", "A-02", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(200);

        // 巡查发现污染 -> 立即下架
        given().contentType(ContentType.JSON)
                .body(Map.of("condition", "CONTAMINATED", "note", "瓶口霉斑",
                        "inspector", "店员甲", "patrolDate", LocalDate.now().toString()))
                .when().post("/api/testers/{code}/patrol", code2)
                .then().statusCode(200);

        given().get("/api/testers/{code}", code2)
                .then().body("status", is("REMOVED"));
    }

    @Test
    @Order(9)
    void destroyRejectsQuantityMismatch() {
        // 登记数量与实际件数不符 -> 拒绝
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCodes", List.of(code2), "declaredQuantity", 5,
                        "reason", "污染销毁", "handler", "仓管"))
                .when().post("/api/destructions")
                .then().statusCode(409)
                .body("error", containsString("核对失败"));
    }

    @Test
    @Order(10)
    void destroyInUseItemRejected() {
        // 未下架的在用品不能销毁（code1 处于冻结，允许；这里新领用一件再试）
        String code3 = given().contentType(ContentType.JSON)
                .body(Map.of("batchId", batchId, "count", 1, "operator", "库管"))
                .when().post("/api/testers/stock-in")
                .then().extract().jsonPath().getString("[0].itemCode");
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code3, "openedDate", OPENED.toString(),
                        "counterLocation", "A-03", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCodes", List.of(code3), "declaredQuantity", 1,
                        "reason", "违规操作", "handler", "仓管"))
                .when().post("/api/destructions")
                .then().statusCode(409)
                .body("error", containsString("下架"));
    }

    @Test
    @Order(11)
    void destroySucceedsAndCannotRepeat() {
        // 销毁两件待处理样品（code1 冻结、code2 已下架）
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCodes", List.of(code1, code2), "declaredQuantity", 2,
                        "reason", "污染及过期销毁", "handler", "仓管-赵强"))
                .when().post("/api/destructions")
                .then().statusCode(200)
                .body("message", containsString("2 件"));

        given().get("/api/testers/{code}", code1).then().body("status", is("DESTROYED"));
        given().get("/api/testers/{code}", code2).then().body("status", is("DESTROYED"));

        // 同一件样品不得被重复销毁
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCodes", List.of(code1), "declaredQuantity", 1,
                        "reason", "重复销毁", "handler", "仓管"))
                .when().post("/api/destructions")
                .then().statusCode(409)
                .body("error", anyOf(containsString("重复"), containsString("已销毁")));

        // 账实核对：销毁记录数 == 已销毁样品数 == 记录数量合计
        given().get("/api/destructions/reconcile")
                .then().statusCode(200)
                .body("destroyedItemCount", is(2))
                .body("destructionRecordCount", is(2))
                .body("totalDestroyedQuantity", is(2))
                .body("consistent", is(true));
    }

    @Test
    @Order(12)
    void fullTraceFromIssueToDestroy() {
        // 沿编号查询从领用到销毁的全部记录
        List<String> types = given().get("/api/testers/{code}/events", code2)
                .then().statusCode(200)
                .extract().jsonPath().getList("eventType", String.class);
        assertEquals(
                List.of("STOCK_IN", "ISSUED", "PATROL", "REMOVED", "DESTROYED"),
                types, "事件链应完整覆盖 入库->领用->巡查->下架->销毁");

        // 销毁记录包含数量、原因、经手人
        given().get("/api/destructions")
                .then().statusCode(200)
                .body("find { it.itemCode == '" + code2 + "' }.quantity", is(1))
                .body("find { it.itemCode == '" + code2 + "' }.reason", is("污染及过期销毁"))
                .body("find { it.itemCode == '" + code2 + "' }.handler", is("仓管-赵强"));
    }

    @Test
    @Order(13)
    void expiredItemAutoRemovedByEnforcement() {
        // 领用一件开封日期已超过效期的样品，触发巡检后应立即下架
        String code4 = given().contentType(ContentType.JSON)
                .body(Map.of("batchId", batchId, "count", 1, "operator", "库管"))
                .when().post("/api/testers/stock-in")
                .then().extract().jsonPath().getString("[0].itemCode");
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code4,
                        "openedDate", LocalDate.now().minusMonths(7).toString(),
                        "counterLocation", "A-04", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(200);

        given().post("/api/testers/enforce-expiries")
                .then().statusCode(200).body("removed", is(1));

        given().get("/api/testers/{code}", code4)
                .then().body("status", is("REMOVED"));
    }

    @Test
    @Order(14)
    void listPrioritizesPendingAndNearExpiry() {
        // 再备一件临期样品（效期 6M，开封 5 个月 20 天前 -> 剩约 10 天）
        String code5 = given().contentType(ContentType.JSON)
                .body(Map.of("batchId", batchId, "count", 1, "operator", "库管"))
                .when().post("/api/testers/stock-in")
                .then().extract().jsonPath().getString("[0].itemCode");
        given().contentType(ContentType.JSON)
                .body(Map.of("itemCode", code5,
                        "openedDate", LocalDate.now().minusMonths(5).minusDays(20).toString(),
                        "counterLocation", "A-05", "operator", "柜长"))
                .when().post("/api/testers/issue")
                .then().statusCode(200)
                .body("nearExpiry", is(true));

        // 列表首位应为待处理（已下架待销毁）实物，临期样品紧随其后
        List<Map<String, Object>> list = given().get("/api/testers")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");
        assertFalse(list.isEmpty());
        assertEquals(0, ((Number) list.get(0).get("priority")).intValue(), "待处理实物应排在最前");
        int nearIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (code5.equals(list.get(i).get("itemCode"))) {
                nearIdx = i;
                break;
            }
        }
        assertTrue(nearIdx >= 0, "临期样品应出现在列表中");
        // 临期之前只允许有待处理实物
        for (int i = 0; i < nearIdx; i++) {
            assertEquals(0, ((Number) list.get(i).get("priority")).intValue());
        }
    }
}
