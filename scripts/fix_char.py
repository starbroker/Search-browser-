with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'arg\.replace.*?replace.*?\)', 'arg.replace("\'", "\\\\\'").replace("\\"", "\\\\\\\"")', content)

with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'w') as f:
    f.write(content)
