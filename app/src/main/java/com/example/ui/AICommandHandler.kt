package com.example.ui

import java.net.URLEncoder

class AICommandHandler(private val viewModel: BrowserViewModel) {

    fun executeCommand(commandType: String, arg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
        when (commandType.uppercase()) {
            "SEARCH" -> viewModel.performSearchFromAI(arg)
            "NAVIGATE" -> viewModel.navigateActiveTabFromAI(arg)
            "SCROLL" -> viewModel.scrollActiveTabFromAI(arg)
            "INJECT" -> viewModel.injectJSInActiveTab(arg)
        }
        }
    }

    fun parseAndExecuteLLMResponse(reply: String): String {
        var spokenText = reply
        
        val searchRegex = "<SEARCH>(.*?)</SEARCH>".toRegex()
        searchRegex.find(spokenText)?.let {
            executeCommand("SEARCH", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
        }
        
        val navigateRegex = "<NAVIGATE>(.*?)</NAVIGATE>".toRegex()
        navigateRegex.find(spokenText)?.let {
            executeCommand("NAVIGATE", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
        }
        
        val scrollRegex = "<SCROLL>(.*?)</SCROLL>".toRegex()
        scrollRegex.find(spokenText)?.let {
            executeCommand("SCROLL", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
        }
        
        val injectRegex = "<INJECT>(.*?)</INJECT>".toRegex()
        injectRegex.find(spokenText)?.let {
            executeCommand("INJECT", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
        }
        
        val paymentRegex = "<PAYMENT>(.*?)</PAYMENT>".toRegex()
        paymentRegex.find(spokenText)?.let {
            spokenText = spokenText.replace(it.value, "")
        }
        
        return spokenText.trim()
    }

    fun parseAndExecuteLocalIntent(input: String): Boolean {
        val lowerInput = input.lowercase().trim()

        val openAndSearch = Regex("open (.*?) and (?:search|find|type) (?:for )?(.*)").find(lowerInput)
        if (openAndSearch != null) {
            val site = openAndSearch.groupValues[1].trim()
            val query = openAndSearch.groupValues[2].trim()
            
            when (site) {
                "amazon" -> executeCommand("NAVIGATE", "https://www.amazon.com/s?k=${URLEncoder.encode(query, "UTF-8")}")
                "youtube" -> executeCommand("NAVIGATE", "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
                "google" -> executeCommand("NAVIGATE", "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
                "chat gpt", "chatgpt" -> executeCommand("NAVIGATE", "https://chatgpt.com/?q=${URLEncoder.encode(query, "UTF-8")}")
                else -> executeCommand("SEARCH", query)
            }
            return true
        }
        
        val searchAmazonMatch = Regex("search (?:in|on) amazon (?:for )?(.*)").find(lowerInput)
        if (searchAmazonMatch != null) {
            executeCommand("NAVIGATE", "https://www.amazon.com/s?k=${URLEncoder.encode(searchAmazonMatch.groupValues[1], "UTF-8")}")
            return true
        }

        if (lowerInput.startsWith("open ") || lowerInput.startsWith("go to ")) {
            val site = lowerInput.removePrefix("open ").removePrefix("go to ").trim()
            val url = when {
                site.contains(".") -> if (site.startsWith("http")) site else "https://$site"
                site == "amazon" -> "https://www.amazon.com"
                site == "youtube" -> "https://www.youtube.com"
                site == "google" -> "https://www.google.com"
                site == "chatgpt" || site == "chat gpt" -> "https://chatgpt.com"
                else -> "https://duckduckgo.com/?q=${URLEncoder.encode(site, "UTF-8")}"
            }
            executeCommand("NAVIGATE", url)
            return true
        }
        
        val typeMatch = Regex("type (.*)").find(lowerInput)
        if (typeMatch != null) {
            val textToType = typeMatch.groupValues[1].replace("\"", "\\\"").replace("'", "\\'")
            val js = """
                (function() {
                    var inputs = document.querySelectorAll('input[type="text"], input[type="search"], textarea');
                    if (inputs.length > 0) {
                        inputs[0].value = "$textToType";
                        inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                        var form = inputs[0].closest('form');
                        if (form) form.submit();
                        else {
                            var btn = document.querySelector('button[type="submit"], input[type="submit"], button[aria-label="Search"]');
                            if(btn) btn.click();
                        }
                    } else {
                        var editable = document.querySelector('[contenteditable="true"]');
                        if (editable) {
                            editable.textContent = "$textToType";
                            editable.dispatchEvent(new Event('input', { bubbles: true }));
                            var btn = document.querySelector('button[data-testid="send-button"], button[aria-label="Send message"]');
                            if(btn) setTimeout(function() { btn.click(); }, 500);
                        }
                    }
                })();
            """.trimIndent()
            executeCommand("INJECT", js)
            return true
        }

        return false
    }
}
