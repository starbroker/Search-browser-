with open("app/src/main/java/com/example/ui/BrowserViewModel.kt", "r") as f:
    content = f.read()

content = content.replace('@file:Suppress("DEPRECATION")', '')
content = '@file:Suppress("DEPRECATION")\n' + content

with open("app/src/main/java/com/example/ui/BrowserViewModel.kt", "w") as f:
    f.write(content)
