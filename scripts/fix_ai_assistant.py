import re

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    manager_code = f.read()

with open("app/src/main/java/com/example/ui/AICommandHandler.kt", "r") as f:
    handler_code = f.read()

# get all imports
imports = re.findall(r"^import\s+.*", manager_code + "\n" + handler_code, re.MULTILINE)
imports = sorted(list(set(imports)))

# remove imports and package from both
manager_code = re.sub(r"^import\s+.*\n", "", manager_code, flags=re.MULTILINE)
manager_code = re.sub(r"^package\s+.*\n", "", manager_code, flags=re.MULTILINE)
handler_code = re.sub(r"^import\s+.*\n", "", handler_code, flags=re.MULTILINE)
handler_code = re.sub(r"^package\s+.*\n", "", handler_code, flags=re.MULTILINE)

combined = "package com.example.ui\n\n" + "\n".join(imports) + "\n\n" + manager_code + "\n\n" + handler_code

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(combined)
