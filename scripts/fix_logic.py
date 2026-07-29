import re

with open("app/src/main/java/com/example/ui/BrowserTranslator.kt", "r") as f:
    translator_code = f.read()

with open("app/src/main/java/com/example/ui/BrowserViewModel.kt", "r") as f:
    viewmodel_code = f.read()

# get all imports
imports = re.findall(r"^import\s+.*", translator_code + "\n" + viewmodel_code, re.MULTILINE)
imports = sorted(list(set(imports)))

# remove imports and package from both
translator_code = re.sub(r"^import\s+.*\n", "", translator_code, flags=re.MULTILINE)
translator_code = re.sub(r"^package\s+.*\n", "", translator_code, flags=re.MULTILINE)
viewmodel_code = re.sub(r"^import\s+.*\n", "", viewmodel_code, flags=re.MULTILINE)
viewmodel_code = re.sub(r"^package\s+.*\n", "", viewmodel_code, flags=re.MULTILINE)

combined = "package com.example.ui\n\n" + "\n".join(imports) + "\n\n" + viewmodel_code + "\n\n" + translator_code

with open("app/src/main/java/com/example/ui/BrowserViewModel.kt", "w") as f:
    f.write(combined)
