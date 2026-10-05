package com.mallhygiene;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * 召回同步测试：本地模拟接口提供通知，涉及批次样品立即下架；重复同步幂等。
 */
@QuarkusTest
class RecallSyncTest {

    @Test
    void recallSyncRemovesAffectedItemsAndIsIdempotent() {
        // 造一批召回通知涉及的正装批次（mock-recalls.json 中的 CH-2024-0915）
        long productId = given().contentType(ContentType.JSON)
                .body(Map.of("name", "召回测试香水", "brand", "TEST", "batchNo", "CH-2024-0915",
                        "productionDate", "2024-09-15", "shelfMonths", 60, "paoMonths", 24))
                .when().post("/api/products")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        // 一件备品 + 一件在用
        String sealedCode = given().contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "operator", "测试员"))
                .when().post("/api/items/stock-in")
                .then().statusCode(200)
                .extract().jsonPath().getString("code");
        String inUseCode = given().contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "operator", "测试员"))
                .when().post("/api/items/stock-in")
                .then().statusCode(200)
                .extract().jsonPath().getString("code");
        given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2026-08-01", "counterLocation", "G-01", "operator", "测试员"))
                .when().post("/api/items/" + inUseCode + "/issue")
                .then().statusCode(200);

        // 同步召回 → 两件都被下架
        given().when().post("/api/recalls/sync")
                .then().statusCode(200)
                .body("noticesReceived", equalTo(2))
                .body("itemsFlagged", greaterThanOrEqualTo(2));

        given().when().get("/api/items/" + sealedCode + "/trail")
                .then().statusCode(200)
                .body("item.status", equalTo("REMOVED"))
                .body("item.removedReason", equalTo("RECALLED"));
        given().when().get("/api/items/" + inUseCode + "/trail")
                .then().statusCode(200)
                .body("item.status", equalTo("REMOVED"))
                .body("item.removedReason", equalTo("RECALLED"));

        // 通知已落库
        given().when().get("/api/recalls")
                .then().statusCode(200)
                .body("noticeCode", hasItem("RC-2026-0918"))
                .body("batchNo", hasItem("CH-2024-0915"));

        // 再次同步：幂等，不再重复下架
        given().when().post("/api/recalls/sync")
                .then().statusCode(200)
                .body("noticesCreated", equalTo(0))
                .body("itemsFlagged", equalTo(0));
    }
}
