with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()

old_code = """    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {"""

new_code = """    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "w") as f:
    f.write(content)
