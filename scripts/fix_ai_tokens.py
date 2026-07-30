import re

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "r") as f:
    content = f.read()

content = content.replace('put("max_tokens", 512)', 'put("max_tokens", 2048)')

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(content)
