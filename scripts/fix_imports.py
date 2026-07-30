with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()

imports = [
    "import androidx.compose.foundation.lazy.LazyRow",
    "import androidx.compose.foundation.lazy.items",
    "import androidx.compose.material.icons.filled.Add"
]

for imp in imports:
    if imp not in content:
        content = content.replace("import androidx.compose.foundation.lazy.LazyColumn", "import androidx.compose.foundation.lazy.LazyColumn\n" + imp)

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "w") as f:
    f.write(content)
