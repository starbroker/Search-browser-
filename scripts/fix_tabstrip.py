with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()

# find the package com.example.ui
# and remove it and everything before TabletTabStrip
index = content.rfind("package com.example.ui")
if index != -1:
    before = content[:index]
    after = content[index:]
    
    # Extract only the TabletTabStrip function
    fun_index = after.find("@Composable\nfun TabletTabStrip")
    if fun_index != -1:
        tab_strip = after[fun_index:]
        content = before + "\n" + tab_strip

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "w") as f:
    f.write(content)
