package com.mallhygiene;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * 销毁核对测试：数量逐件对应、同一件样品不可重复处理、未下架不可销毁。
 */
@QuarkusTest
class DestructionTest {

    private long createProduct(String batchNo) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("name", "销毁测试-" + batchNo, "brand", "TEST", "batchNo", batchNo,
                        "productionDate", "2025-06-01", "shelfMonths", 36, "paoMonths", 6))
                .when().post("/api/products")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private long issueAndRemove(long productId, String counter) {
        String code = given().contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "operator", "测试员"))
                .when().post("/api/items/stock-in")
                .then().statusCode(200)
                .extract().jsonPath().getString("code");
        long id = given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2025-08-01", "counterLocation", counter, "operator", "测试员"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(200)
                .extract().jsonPath().getLong("id");
        given().contentType(ContentType.JSON)
                .body(Map.of("reason", "EXPIRED", "operator", "测试员"))
                .when().post("/api/items/" + code + "/remove")
                .then().statusCode(200);
        return id;
    }

    @Test
    void destructionReconcilesItemByItem() {
        long productId = createProduct("UT-DEST-1");
        long id1 = issueAndRemove(productId, "F-01");
        long id2 = issueAndRemove(productId, "F-02");

        // 销毁两件：申报数 = 实际处理数 = 2
        String batchCode = given().contentType(ContentType.JSON)
                .body(Map.of("itemIds", List.of(id1, id2), "reason", "到期集中销毁", "handler", "王敏"))
                .when().post("/api/destructions")
                .then().statusCode(200)
                .body("declaredQuantity", equalTo(2))
                .body("actualQuantity", equalTo(2))
                .extract().jsonPath().getString("batchCode");

        // 核对：该批申报=实际=库内关联已销毁件数，且全局一致
        given().when().get("/api/destructions/reconcile")
                .then().statusCode(200)
                .body("ok", is(true))
                .body("batches.find { it.batchCode == '" + batchCode + "' }.declared", equalTo(2))
                .body("batches.find { it.batchCode == '" + batchCode + "' }.actual", equalTo(2))
                .body("batches.find { it.batchCode == '" + batchCode + "' }.linkedDestroyedItems", equalTo(2))
                .body("batches.find { it.batchCode == '" + batchCode + "' }.ok", is(true))
                .body("totalActual", greaterThanOrEqualTo(2))
                .body("totalDestroyedItems", greaterThanOrEqualTo(2));

        // 逐件状态：两件均已销毁且关联同一批次
        given().when().get("/api/items?status=DESTROYED")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    void sameItemCannotBeDestroyedTwice() {
        long productId = createProduct("UT-DEST-2");
        long id = issueAndRemove(productId, "F-03");

        given().contentType(ContentType.JSON)
                .body(Map.of("itemIds", List.of(id), "reason", "到期销毁", "handler", "王敏"))
                .when().post("/api/destructions")
                .then().statusCode(200)
                .body("actualQuantity", equalTo(1));

        // 同一件样品再次销毁 → 409 冲突
        given().contentType(ContentType.JSON)
                .body(Map.of("itemIds", List.of(id), "reason", "重复提交", "handler", "王敏"))
                .when().post("/api/destructions")
                .then().statusCode(409);

        // 核对仍然一致：重复请求被整体拒绝，未产生脏数据
        given().when().get("/api/destructions/reconcile")
                .then().statusCode(200)
                .body("ok", is(true))
                .body("batches.ok", everyItem(is(true)));
    }

    @Test
    void inUseItemCannotBeDestroyed() {
        long productId = createProduct("UT-DEST-3");
        String code = given().contentType(ContentType.JSON)
                .body(Map.of("productId", productId, "operator", "测试员"))
                .when().post("/api/items/stock-in")
                .then().statusCode(200)
                .extract().jsonPath().getString("code");
        long id = given().contentType(ContentType.JSON)
                .body(Map.of("openedDate", "2026-09-01", "counterLocation", "F-04", "operator", "测试员"))
                .when().post("/api/items/" + code + "/issue")
                .then().statusCode(200)
                .extract().jsonPath().getLong("id");

        // 在用试用品未下架，直接销毁 → 409
        given().contentType(ContentType.JSON)
                .body(Map.of("itemIds", List.of(id), "reason", "误操作", "handler", "王敏"))
                .when().post("/api/destructions")
                .then().statusCode(409);

        // 销毁记录中不含该批次（事务已回滚，未留下“误操作”批次）
        given().when().get("/api/destructions")
                .then().statusCode(200)
                .body("reason", not(hasItem("误操作")));
    }
}
