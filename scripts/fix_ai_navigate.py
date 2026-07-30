import re

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "r") as f:
    content = f.read()

# Fix executeCommand to add ADD_TAB
add_tab = """            "NAVIGATE" -> viewModel.navigateActiveTabFromAI(arg)
            "ADD_TAB" -> viewModel.addNewTab(arg)"""
content = content.replace('            "NAVIGATE" -> viewModel.navigateActiveTabFromAI(arg)', add_tab)

# Update the parsing logic in parseAndExecuteLLMResponse
old_nav = """        val navigateRegex = "<NAVIGATE>(.*?)</NAVIGATE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        navigateRegex.findAll(spokenText).forEach {
            executeCommand("NAVIGATE", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }"""

new_nav = """        val navigateRegex = "<NAVIGATE>(.*?)</NAVIGATE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        var navCount = 0
        navigateRegex.findAll(spokenText).forEach {
            if (navCount == 0) {
                executeCommand("NAVIGATE", it.groupValues[1])
            } else {
                executeCommand("ADD_TAB", it.groupValues[1])
            }
            navCount++
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }"""

content = content.replace(old_nav, new_nav)

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(content)
