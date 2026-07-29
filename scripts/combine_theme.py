import re
import os

files = [
    "app/src/main/java/com/example/ui/theme/Color.kt",
    "app/src/main/java/com/example/ui/theme/Type.kt",
    "app/src/main/java/com/example/ui/theme/Theme.kt"
]

combined_content = ""
imports = []

for file in files:
    with open(file, "r") as f:
        content = f.read()
    
    # Extract imports
    file_imports = re.findall(r"^import\s+.*", content, re.MULTILINE)
    imports.extend(file_imports)
    
    # Remove imports and package
    content = re.sub(r"^import\s+.*\n", "", content, flags=re.MULTILINE)
    content = re.sub(r"^package\s+.*\n", "", content, flags=re.MULTILINE)
    
    combined_content += content.strip() + "\n\n"

# Deduplicate imports
imports = sorted(list(set(imports)))

final_content = "package com.example.ui.theme\n\n" + "\n".join(imports) + "\n\n" + combined_content

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(final_content)

os.remove("app/src/main/java/com/example/ui/theme/Color.kt")
os.remove("app/src/main/java/com/example/ui/theme/Type.kt")
