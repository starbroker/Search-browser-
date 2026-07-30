#!/bin/bash
curl -X POST "https://api.groq.com/openai/v1/chat/completions" \
  -H "Authorization: Bearer $GROQ_API_KEY_3" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.3-70b-versatile",
    "messages": [
      {
        "role": "user",
        "content": "say hello"
      }
    ],
    "stream": true
  }'
