with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'r') as f:
    content = f.read()

content = content.replace('replace("\'", "\\\'")', 'replace("\'", "\\\\\'")')
content = content.replace('replace(\'"\', \'\\"\')', 'replace(\'"\', \'\\\\"\')')

with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'w') as f:
    f.write(content)
