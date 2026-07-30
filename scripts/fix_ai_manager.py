with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'r') as f:
    content = f.read()

# Change model
content = content.replace('"qwen-2.5-32b"', '"llama3-70b-8192"')

# Keep history:
# We need to create messagesArray OUTSIDE the while loop.
import re
start_idx = content.find('val messagesArray = org.json.JSONArray()')
end_idx = content.find('val jsonPayload = org.json.JSONObject().apply', start_idx)
print(content[start_idx:end_idx])
