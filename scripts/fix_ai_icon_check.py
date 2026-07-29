with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()
if "androidx.compose.material.icons.Icons.Default.Terrain" in content:
    print("Yes")
