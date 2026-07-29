with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if 'val textToType =' in line:
        new_lines.append('                val textToType = arg.replace(' + chr(34) + '\\"' + chr(34) + ', ' + chr(34) + '\\\\\\"' + chr(34) + ').replace(' + chr(34) + "'" + chr(34) + ', ' + chr(34) + "\\\\'" + chr(34) + ')\n')
    elif 'val textToClick =' in line:
        new_lines.append('                val textToClick = arg.replace(' + chr(34) + '\\"' + chr(34) + ', ' + chr(34) + '\\\\\\"' + chr(34) + ').replace(' + chr(34) + "'" + chr(34) + ', ' + chr(34) + "\\\\'" + chr(34) + ')\n')
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'w') as f:
    f.writelines(new_lines)
