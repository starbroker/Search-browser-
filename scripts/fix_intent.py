with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'val textToType =' in line and 'typeMatch' in lines[i-1]:
        lines[i] = '                val textToType = typeMatch.groupValues[1].replace(' + chr(34) + '\\"' + chr(34) + ', ' + chr(34) + '\\\\\\"' + chr(34) + ').replace(' + chr(34) + "'" + chr(34) + ', ' + chr(34) + "\\\\'" + chr(34) + ')\n'

with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'w') as f:
    f.writelines(lines)
