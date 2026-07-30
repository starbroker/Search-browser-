with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'r') as f:
    content = f.read()

import re

# We will add type and click commands
new_execute = '''    fun executeCommand(commandType: String, arg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
        when (commandType.uppercase()) {
            "SEARCH" -> viewModel.performSearchFromAI(arg)
            "NAVIGATE" -> viewModel.navigateActiveTabFromAI(arg)
            "SCROLL" -> {
                val js = if (arg.contains("down", ignoreCase = true)) "window.scrollBy({top: window.innerHeight * 0.8, behavior: 'smooth'});" else "window.scrollBy({top: -window.innerHeight * 0.8, behavior: 'smooth'});"
                viewModel.injectJSInActiveTab(js)
            }
            "INJECT" -> viewModel.injectJSInActiveTab(arg)
            "TYPE" -> {
                val textToType = arg.replace("'", "\\'").replace('"', '\\"')
                val js = """
                    (function() {
                        var inputs = document.querySelectorAll('input[type="text"], input[type="search"], textarea');
                        if (inputs.length > 0) {
                            var el = inputs[0];
                            el.focus();
                            el.value = '';
                            var i = 0;
                            var text = "$textToType";
                            var interval = setInterval(function() {
                                el.value += text.charAt(i);
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                i++;
                                if (i >= text.length) {
                                    clearInterval(interval);
                                    var form = el.closest('form');
                                    if (form) {
                                        setTimeout(function() { form.submit(); }, 500);
                                    } else {
                                        var btn = document.querySelector('button[type="submit"], input[type="submit"], button[aria-label="Search"]');
                                        if(btn) setTimeout(function() { btn.click(); }, 500);
                                    }
                                }
                            }, 100);
                        }
                    })();
                """.trimIndent()
                viewModel.injectJSInActiveTab(js)
            }
            "CLICK" -> {
                // Click the first link matching the text
                val textToClick = arg.replace("'", "\\'").replace('"', '\\"')
                val js = """
                    (function() {
                        var links = Array.from(document.querySelectorAll('a, button, [role="button"]'));
                        var target = links.find(el => el.innerText.toLowerCase().includes("$textToClick".toLowerCase()));
                        if (target) {
                            target.style.border = '2px solid red'; // Highlight before click
                            setTimeout(function() { target.click(); }, 500);
                        } else {
                            // If no exact match, try querySelector
                            var el = document.querySelector("$textToClick");
                            if (el) {
                                el.style.border = '2px solid red';
                                setTimeout(function() { el.click(); }, 500);
                            }
                        }
                    })();
                """.trimIndent()
                viewModel.injectJSInActiveTab(js)
            }
        }
        }
    }'''

content = re.sub(r'    fun executeCommand.*?    }', new_execute, content, flags=re.DOTALL)

# Add TYPE and CLICK to parseAndExecuteLLMResponse
new_parse = '''
        val typeRegex = "<TYPE>(.*?)</TYPE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        typeRegex.find(spokenText)?.let {
            executeCommand("TYPE", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }

        val clickRegex = "<CLICK>(.*?)</CLICK>".toRegex(RegexOption.DOT_MATCHES_ALL)
        clickRegex.find(spokenText)?.let {
            executeCommand("CLICK", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
        
        return Pair(spokenText.trim(), hasCommand)
'''

content = content.replace('        return Pair(spokenText.trim(), hasCommand)', new_parse)

with open('app/src/main/java/com/example/ui/AICommandHandler.kt', 'w') as f:
    f.write(content)
