package com.mallhygiene;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * 核心流程测试：领用效期计算、调柜日期保持、冻结限制、巡查自动下架、效期扫描。
 */
@QuarkusTest
class TesterFlowTest {

    private long createProduct(String batchNo, int paoMonths) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", "测试精华-" + batchNo, "brand", "TEST", "batchNo", batchNo,
                        "productionDate", "2026-01-15", "shelfMonths", 36, "paoMonths", paoMonths))
                .when().post("/api/products")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private String stockIn(long productId) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "operator", "测试员"))
                .when().post("/api/items/stock-in")
                .then().statusCode(200)
                .extract().jsonPath().getString("code");
    }

    @Test
    void issueComputesExpiryAndTransferKeepsDates() {
        long productId = createProduct("UT-FLOW-1", 6);
        String code = stockIn(productId);

        // 领用：登记开封日期与柜位，效期 = 开封日期 + 6 个月
        given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2026-04-09", "counterLocation", "A-01", "operator", "小李"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(200)
                .body("status", equalTo("IN_USE"))
                .body("openedDate", equalTo("2026-04-09"))
                .body("expiryDate", equalTo("2026-10-09"))
                .body("counterLocation", equalTo("A-01"));

        // 调柜：仅柜位变化，开封日期与到期日保持不变
        given().contentType(ContentType.JSON)
                .body(Map.of("toLocation", "B-02", "operator", "小王"))
                .when().post("/api/items/" + code + "/transfer")
                .then().statusCode(200)
                .body("counterLocation", equalTo("B-02"))
                .body("openedDate", equalTo("2026-04-09"))
                .body("expiryDate", equalTo("2026-10-09"));

        // 再调一次柜，日期依然不变
        given().contentType(ContentType.JSON)
                .body(Map.of("toLocation", "C-03", "operator", "小王"))
                .when().post("/api/items/" + code + "/transfer")
                .then().statusCode(200)
                .body("counterLocation", equalTo("C-03"))
                .body("openedDate", equalTo("2026-04-09"))
                .body("expiryDate", equalTo("2026-10-09"));

        // 履历：完整事件链，调柜事件记录起止柜位
        given().when().get("/api/items/" + code + "/trail")
                .then().statusCode(200)
                .body("events.type", hasItems("STOCK_IN", "ISSUE", "TRANSFER"))
                .body("events.find { it.type == 'TRANSFER' }.fromLocation", equalTo("A-01"))
                .body("events.find { it.type == 'TRANSFER' }.toLocation", equalTo("B-02"));
    }

    @Test
    void frozenItemCannotBeIssued() {
        long productId = createProduct("UT-FLOW-2", 6);
        String code = stockIn(productId);

        given().contentType(ContentType.JSON)
                .body(Map.of("reason", "疑似污染待复检", "operator", "主管"))
                .when().post("/api/items/" + code + "/freeze")
                .then().statusCode(200)
                .body("status", equalTo("FROZEN"));

        // 冻结后不得领用
        given().contentType(ContentType.JSON)
                .body(Map.of("counterLocation", "A-09", "operator", "小李"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(409);

        // 解冻后可正常领用
        given().contentType(ContentType.JSON).body(Map.of("operator", "主管"))
                .when().post("/api/items/" + code + "/unfreeze")
                .then().statusCode(200)
                .body("status", equalTo("SEALED"));

        given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2026-05-01", "counterLocation", "A-09", "operator", "小李"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(200)
                .body("status", equalTo("IN_USE"))
                .body("expiryDate", equalTo("2026-11-01"));
    }

    @Test
    void inspectionExpiredOrContaminatedAutoRemoves() {
        long productId = createProduct("UT-FLOW-3", 6);
        String code = stockIn(productId);
        given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2026-06-01", "counterLocation", "D-01", "operator", "小李"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(200);

        // 巡查发现污染 → 立即下架
        given().contentType(ContentType.JSON)
                .body(Map.of("result", "CONTAMINATED", "inspector", "王敏", "note", "瓶口霉斑"))
                .when().post("/api/items/" + code + "/inspect")
                .then().statusCode(200);

        given().when().get("/api/items/" + code + "/trail")
                .then().statusCode(200)
                .body("item.status", equalTo("REMOVED"))
                .body("item.removedReason", equalTo("CONTAMINATED"))
                .body("events.type", hasItems("INSPECT", "REMOVE"));
    }

    @Test
    void expiredItemsAreAutoRemovedByScan() {
        long productId = createProduct("UT-FLOW-4", 6);
        String code = stockIn(productId);
        // 开封日期很早 → 已过期
        given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2025-01-01", "counterLocation", "E-01", "operator", "小李"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(200)
                .body("condition", equalTo("EXPIRED"));

        // 触发效期扫描 → 自动下架
        given().when().post("/api/maintenance/scan-expired")
                .then().statusCode(200)
                .body("removed", greaterThanOrEqualTo(1));

        given().when().get("/api/items/" + code + "/trail")
                .then().statusCode(200)
                .body("item.status", equalTo("REMOVED"))
                .body("item.removedReason", equalTo("EXPIRED"));
    }
}
