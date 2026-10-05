# 商场试用品开封与卫生效期管理系统

化妆品柜台试用品全生命周期管理：按每件试用品的**实际开封日期**管理使用期限，
调柜不延长效期，覆盖 入库 → 领用 → 巡查 → 调柜 → 冻结 → 下架 → 销毁 的完整链路。

## 一条命令启动

```bash
./start.sh
```

脚本依次完成：构建 Svelte 前端 → 打包 Quarkus 后端 → 启动应用。
启动后访问：

| 入口 | 地址 |
|---|---|
| 管理界面 | http://localhost:8080 |
| 召回模拟接口 | http://localhost:8080/mock-api/recalls |
| API | http://localhost:8080/api/... |

要求：JDK 17+、Maven 3.8+、Node 18+（本仓库环境中已预置于 `~/tools`，脚本会自动探测）。
数据库为 SQLite，首次启动自动创建 `data/hygiene.db` 并写入演示数据（14 件各状态试用品）。

## 运行测试

```bash
cd backend && mvn test
```

11 个测试覆盖需求中要求重点核对的场景：

- **效期计算**（`ExpiryServiceTest`）：到期日 = 开封日期 + 开封后使用月数（PAO），
  含月末对齐（1/31 + 1月 = 2/28）、到期日当天仍可用、7 天临期窗口边界。
- **调柜日期保持**（`TesterFlowTest`）：领用后多次调柜，开封日期与到期日均不变，
  履历中记录起止柜位。
- **销毁逐件核对**（`DestructionTest`）：申报数 = 实际处理数 = 库内已销毁件数；
  同一件样品重复销毁返回 409；未下架（在用）样品不可销毁，事务整体回滚。
- **召回同步**（`RecallSyncTest`）：本地模拟接口提供通知，涉及批次样品立即下架，
  重复同步幂等。

## 业务规则

| 规则 | 实现 |
|---|---|
| 每件试用品独立编号 | `TesterItem.code` 唯一约束，贯穿全部记录 |
| 效期按实际开封日期 | `expiryDate = openedDate + paoMonths`，领用时计算 |
| 调柜不延长效期 | 调柜仅更新 `counterLocation`，不回写日期（有测试保证） |
| 临期/待处理优先 | 列表按 待销毁→已过期→临期→冻结→在用→备品 排序 |
| 备品与已开封分开统计 | `/api/stats` 分状态计数 |
| 过期/召回立即下架 | 定时扫描（启动时+每小时）+ 召回同步自动下架；巡查发现污染/到期也立即下架 |
| 冻结不得再次领用 | 领用接口对 FROZEN 状态返回 409 |
| 销毁登记 | 批次记录数量/原因/经手人，守卫式状态迁移防止一件样品重复处理 |
| 全程可追溯 | `GET /api/items/{code}/trail` 返回事件时间线+巡查+销毁信息 |
| 账实核对 | `GET /api/destructions/reconcile` 逐批与全局核对 |

## 技术栈与结构

- **后端**：Java 17 · Quarkus 3.15 · Hibernate ORM (Panache) · SQLite（quarkiverse jdbc-sqlite）· Scheduler
- **前端**：Svelte 4 + Vite，构建产物输出到 `backend/src/main/resources/META-INF/resources`，由 Quarkus 托管
- **召回通知**：`GET /mock-api/recalls`（本地模拟接口，数据见 `backend/src/main/resources/mock-recalls.json`），
  业务侧经 REST Client 拉取并落库

```
backend/src/main/java/com/mallhygiene/
├── domain/      # Product / TesterItem / ItemEvent / InspectionRecord / RecallNotice / DestructionBatch
├── service/     # ExpiryService(效期) TesterService(核心流转) RecallService ExpiryScanJob DataSeeder
├── resource/    # REST 接口 + MockRecallResource(模拟接口) + ErrorMapper
└── dto/Dtos.java
frontend/src/
├── App.svelte            # 导航与全局样式
└── components/           # 仪表盘/台账/入库领用/巡查/召回/销毁/履历
```

## 主要 API

```
GET  /api/stats                        分状态统计
GET  /api/items?status=&q=             台账（待处理优先排序）
POST /api/items/stock-in               备品入库
POST /api/items/{code}/issue           领用（登记批次/开封日期/柜位）
POST /api/items/{code}/transfer        调柜（日期不变）
POST /api/items/{code}/freeze|unfreeze 冻结/解冻
POST /api/items/{code}/inspect         巡查（污染/到期自动下架）
POST /api/items/{code}/remove          下架
GET  /api/items/{code}/trail           履历追溯
POST /api/maintenance/scan-expired     手动效期扫描
GET  /api/recalls  POST /api/recalls/sync   召回通知与同步
POST /api/destructions                 销毁登记（数量/原因/经手人）
GET  /api/destructions/reconcile       销毁账实核对
```

开发模式（可选）：后端 `cd backend && mvn quarkus:dev`，前端 `cd frontend && npm run dev`（Vite 代理 8080）。
