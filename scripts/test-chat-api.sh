#!/bin/bash
# 智能客服 /chat 接口测试（需先启动应用，默认 http://localhost:8080）
BASE_URL="${1:-http://localhost:8080}"

echo "=== 智能客服接口测试 (BASE_URL=$BASE_URL) ==="

# 1. 单轮对话
echo ""
echo "1. 单轮对话: message=你好"
curl -s -X POST "$BASE_URL/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"你好"}' | jq .

# 2. 带 sessionId 多轮（同一 session 可连续对话）
echo ""
echo "2. 多轮对话: message=找一些风景图, sessionId=test-session-001"
curl -s -X POST "$BASE_URL/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"找一些风景图","sessionId":"test-session-001"}' | jq .

# 3. 上传排行榜
echo ""
echo "3. 问排行榜: message=上传排行榜前5名是谁"
curl -s -X POST "$BASE_URL/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"上传排行榜前5名是谁"}' | jq .

# 4. 空消息应返回参数错误
echo ""
echo "4. 空消息(期望 code=40000):"
curl -s -X POST "$BASE_URL/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":""}' | jq .

echo ""
echo "=== 测试结束 ==="
