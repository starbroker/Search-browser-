import re

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "r") as f:
    content = f.read()

add_tab_parsing = """
        val addTabRegex = "<ADD_TAB>(.*?)</ADD_TAB>".toRegex(RegexOption.DOT_MATCHES_ALL)
        addTabRegex.findAll(spokenText).forEach {
            executeCommand("ADD_TAB", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
"""

content = content.replace('val scrollRegex = "<SCROLL>(.*?)</SCROLL>".toRegex(RegexOption.DOT_MATCHES_ALL)', add_tab_parsing.strip() + '\n\n        val scrollRegex = "<SCROLL>(.*?)</SCROLL>".toRegex(RegexOption.DOT_MATCHES_ALL)')

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(content)
