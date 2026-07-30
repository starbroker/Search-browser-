with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'r') as f:
    content = f.read()

# Replace Regex
content = content.replace('val searchRegex = "<SEARCH>(.*?)</SEARCH>".toRegex()', 'val searchRegex = "<SEARCH>(.*?)</SEARCH>".toRegex(RegexOption.DOT_MATCHES_ALL)')
content = content.replace('val navigateRegex = "<NAVIGATE>(.*?)</NAVIGATE>".toRegex()', 'val navigateRegex = "<NAVIGATE>(.*?)</NAVIGATE>".toRegex(RegexOption.DOT_MATCHES_ALL)')
content = content.replace('val scrollRegex = "<SCROLL>(.*?)</SCROLL>".toRegex()', 'val scrollRegex = "<SCROLL>(.*?)</SCROLL>".toRegex(RegexOption.DOT_MATCHES_ALL)')
content = content.replace('val injectRegex = "<INJECT>(.*?)</INJECT>".toRegex()', 'val injectRegex = "<INJECT>(.*?)</INJECT>".toRegex(RegexOption.DOT_MATCHES_ALL)')
content = content.replace('val paymentRegex = "<PAYMENT>(.*?)</PAYMENT>".toRegex()', 'val paymentRegex = "<PAYMENT>(.*?)</PAYMENT>".toRegex(RegexOption.DOT_MATCHES_ALL)')

with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'w') as f:
    f.write(content)
