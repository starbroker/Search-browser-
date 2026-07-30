with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'r') as f:
    content = f.read()

bad_part = '''                                        intermediateText = intermediateText.replace(Regex("<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL)
                                        intermediateText = intermediateText.replace(Regex("<TYPE>.*?</TYPE>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<CLICK>.*?</CLICK>", RegexOption.DOT_MATCHES_ALL), ""), "")'''

good_part = '''                                        intermediateText = intermediateText.replace(Regex("<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<TYPE>.*?</TYPE>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<CLICK>.*?</CLICK>", RegexOption.DOT_MATCHES_ALL), "")'''

content = content.replace(bad_part, good_part)

with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'w') as f:
    f.write(content)
