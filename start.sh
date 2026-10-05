#!/usr/bin/env bash
# 一条命令启动：构建前端 → 打包后端 → 启动应用
# 用法: ./start.sh        （首次会自动安装依赖，耗时几分钟）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

# ---- 定位 Java / Maven（优先使用 PATH 中已有的，其次使用本机工具目录） ----
if ! command -v java >/dev/null 2>&1; then
  if [ -x "$HOME/tools/jdk17/bin/java" ]; then
    export JAVA_HOME="$HOME/tools/jdk17"
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo "未找到 Java，请先安装 JDK 17+"; exit 1
  fi
fi
if ! command -v mvn >/dev/null 2>&1; then
  if [ -x "$HOME/tools/maven/bin/mvn" ]; then
    export PATH="$HOME/tools/maven/bin:$PATH"
  else
    echo "未找到 Maven，请先安装 Maven 3.8+"; exit 1
  fi
fi

echo "==> Java: $(java -version 2>&1 | head -1)"

# ---- 1. 构建前端到 Quarkus 静态资源目录 ----
echo "==> 构建前端..."
cd "$ROOT/frontend"
if [ ! -d node_modules ]; then
  npm install --no-audit --no-fund
fi
npm run build

# ---- 2. 打包后端 ----
echo "==> 打包后端..."
cd "$ROOT/backend"
mvn -q package -DskipTests

# ---- 3. 启动 ----
mkdir -p "$ROOT/data"
cd "$ROOT"
echo ""
echo "==> 启动成功后可访问: http://localhost:8080"
echo "==> 召回模拟接口:     http://localhost:8080/mock-api/recalls"
echo ""
exec java -jar backend/target/quarkus-app/quarkus-run.jar
