import re

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "r") as f:
    content = f.read()

# Replace find with findAll().forEach
patterns = [
    ("searchRegex.find(spokenText)?.let {", "searchRegex.findAll(spokenText).forEach {"),
    ("navigateRegex.find(spokenText)?.let {", "navigateRegex.findAll(spokenText).forEach {"),
    ("scrollRegex.find(spokenText)?.let {", "scrollRegex.findAll(spokenText).forEach {"),
    ("injectRegex.find(spokenText)?.let {", "injectRegex.findAll(spokenText).forEach {"),
    ("paymentRegex.find(spokenText)?.let {", "paymentRegex.findAll(spokenText).forEach {"),
    ("typeRegex.find(spokenText)?.let {", "typeRegex.findAll(spokenText).forEach {"),
    ("clickRegex.find(spokenText)?.let {", "clickRegex.findAll(spokenText).forEach {")
]

for old, new in patterns:
    content = content.replace(old, new)

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(content)
