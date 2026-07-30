with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'r') as f:
    content = f.read()

content = content.replace('Regex("<SEARCH>.*?</SEARCH>")', 'Regex("<SEARCH>.*?</SEARCH>", RegexOption.DOT_MATCHES_ALL)')
content = content.replace('Regex("<NAVIGATE>.*?</NAVIGATE>")', 'Regex("<NAVIGATE>.*?</NAVIGATE>", RegexOption.DOT_MATCHES_ALL)')
content = content.replace('Regex("<SCROLL>.*?</SCROLL>")', 'Regex("<SCROLL>.*?</SCROLL>", RegexOption.DOT_MATCHES_ALL)')
content = content.replace('Regex("<INJECT>.*?</INJECT>")', 'Regex("<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL)')
content = content.replace('Regex("<PAYMENT>.*?</PAYMENT>")', 'Regex("<PAYMENT>.*?</PAYMENT>", RegexOption.DOT_MATCHES_ALL)')

with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'w') as f:
    f.write(content)
