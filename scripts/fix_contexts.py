with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i in range(len(lines)):
    if 'val context = androidx.compose.ui.platform.LocalContext.current' in lines[i]:
        # Check if the previous or two previous lines had val context = LocalContext.current
        if i > 0 and 'val context = LocalContext.current' in lines[i-1]:
            continue
        if i > 1 and 'val context = LocalContext.current' in lines[i-2]:
            continue
    new_lines.append(lines[i])

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.writelines(new_lines)
