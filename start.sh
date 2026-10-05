#!/usr/bin/env bash
# 商场试用品开封与卫生效期管理系统 —— 一键启动
# 用法：
#   ./start.sh        构建前端 + 后端并启动（http://localhost:8080）
#   ./start.sh test   运行后端测试（效期计算 / 调柜日期保持 / 销毁核对等）
set -euo pipefail
cd "$(dirname "$0")"

# ---- 工具链：优先使用项目自带 .tools，否则使用系统环境 ----
if [ -d ".tools/jdk" ]; then
  export JAVA_HOME="$PWD/.tools/jdk"
  export PATH="$JAVA_HOME/bin:$PWD/.tools/maven/bin:$PATH"
fi

if [ "${1:-}" = "test" ]; then
  (cd backend && mvn test)
  exit 0
fi

echo "==> [1/4] 构建前端 (Svelte + Vite)"
if [ ! -d frontend/node_modules ]; then
  (cd frontend && npm install)
fi
(cd frontend && npm run build)

echo "==> [2/4] 前端产物交由 Quarkus 托管"
rm -rf backend/src/main/resources/META-INF/resources
mkdir -p backend/src/main/resources/META-INF/resources
cp -r frontend/dist/. backend/src/main/resources/META-INF/resources/

echo "==> [3/4] 构建后端 (Quarkus + Hibernate + SQLite)"
mkdir -p data
(cd backend && mvn -q package -DskipTests)

echo "==> [4/4] 启动服务"
echo "    系统入口: http://localhost:8080"
echo "    模拟召回接口: http://localhost:8080/mock-api/recall-notices"
exec java -jar backend/target/quarkus-app/quarkus-run.jar
