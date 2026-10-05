package com.mall.tester;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * 每个测试类使用独立的 SQLite 文件库，互相隔离。
 */
public final class TestProfiles {

    private TestProfiles() {
    }

    public static class Flow implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.datasource.jdbc.url", "jdbc:sqlite:target/test-flow.db",
                    "quarkus.hibernate-orm.database.generation", "drop-and-create");
        }
    }

    public static class Recall implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.datasource.jdbc.url", "jdbc:sqlite:target/test-recall.db",
                    "quarkus.hibernate-orm.database.generation", "drop-and-create");
        }
    }
}
