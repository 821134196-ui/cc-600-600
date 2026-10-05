# 商场试用品开封与卫生效期管理系统

化妆品柜台试用品的全生命周期管理：按每件试用品的实际开封日期计算卫生效期，
覆盖 **备品入库 → 领用开封 → 调柜 → 巡查 → 冻结/下架 → 销毁** 全链路，
并支持召回通知同步与全链路追溯。

## 快速开始

```bash
./start.sh          # 一条命令：构建前端 + 后端并启动，访问 http://localhost:8080
./start.sh test     # 运行后端测试（效期计算 / 调柜日期保持 / 销毁数量核对等 21 个用例）
```

首次启动会自动初始化演示数据（含正常、临期、过期、冻结、待销毁、已销毁等各类样品），
数据库文件为 `data/tester-expiry.db`（SQLite，自动建表）。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Quarkus 3.15 · Hibernate ORM |
| 前端 | Svelte 4 · Vite（构建产物由 Quarkus 托管，单端口访问） |
| 数据库 | SQLite（quarkus-jdbc-sqlite） |

## 业务规则

- **效期计算**：效期 = 开封日期 + PAO（开封后使用月数）；截止日当天可用，次日过期；
  剩余 ≤ 30 天为临期（`tester.near-expiry-days` 可调）。
- **调柜**：仅变更柜位，开封日期与效期原样沿用，事件留痕。
- **统计口径**：未开封备品与已开封试用品分开统计；列表按
  待处理（已下架/过期/召回）→ 临期 → 冻结 → 正常 → 备品 → 已销毁 排序。
- **自动下架**：过期样品由定时巡检（每小时，亦可手动触发）自动下架；
  巡查登记"污染/到期"立即下架；召回批次的在用样品立即下架、未开封备品冻结。
- **冻结**：冻结的试用品不得再次领用（服务端强制校验）。
- **销毁**：登记数量必须与实际提交样品件数一致；每件样品仅可销毁一次
  （业务校验 + `item_id` 唯一约束双保险）；销毁记录含数量、原因、经手人；
  `/api/destructions/reconcile` 提供账实核对（已销毁样品数 = 销毁记录数 = 记录数量合计）。
- **追溯**：沿独立编号查询从入库到销毁的全部事件（`/api/testers/{code}/events`）。

## 召回模拟接口

召回通知由本地模拟接口提供：`GET /mock-api/recall-notices`，
数据来自 `data/recall-notices.json`（可编辑模拟新通报）。
前端"召回中心"或 `POST /api/recalls/sync` 触发同步，幂等处理。

## 主要接口

```
GET  /api/testers                 试用品列表（优先级排序，支持状态/柜位/关键字过滤）
GET  /api/testers/stats           统计（未开封/在用/临期/待销毁等分开统计）
POST /api/testers/stock-in        备品入库
POST /api/testers/issue           领用开封（登记批次、开封日期、柜位）
POST /api/testers/{code}/transfer 调柜（开封日期不变）
POST /api/testers/{code}/freeze   冻结
POST /api/testers/{code}/remove   下架
POST /api/testers/{code}/patrol   巡查登记（污染/临期/到期）
GET  /api/testers/{code}/events   全链路追溯
POST /api/destructions            销毁（数量核对 + 防重复）
GET  /api/destructions/reconcile  销毁账实核对
POST /api/recalls/sync            同步召回通知
GET  /api/batches                 批次列表
```

## 目录结构

```
├── start.sh                 # 一键启动 / 测试
├── backend/                 # Quarkus 后端
│   └── src/main/java/com/mall/tester/
│       ├── model/           # 实体：批次/试用品/事件/巡查/销毁/召回
│       ├── service/         # 效期计算、业务规则、召回同步、定时巡检
│       └── resource/        # REST 接口 + 本地模拟召回接口
├── frontend/                # Svelte 前端（工作台/列表/领用/巡查/销毁/召回/追溯）
└── data/                    # SQLite 数据库与召回模拟数据
```
