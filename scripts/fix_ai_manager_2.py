with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'r') as f:
    content = f.read()

part_to_replace = '''
                var isFirstTurn = true
                var iterations = 0
                val maxIterations = 5

                while (iterations < maxIterations) {
                    iterations++

                    val pageContent = viewModel.getActiveTabContent()
                    val pageContext = if (pageContent.isNotBlank()) "\\n\\nCURRENT WEBPAGE TEXT:\\n$pageContent" else ""

                    val systemPrompt = """
                        You are Storm AI, an autonomous agent built into the Storm Web Browser. Your goal is to browse the web, find products, read information, and help the user proactively.
                        
                        - You are given the "CURRENT WEBPAGE TEXT:" which contains the text of the page the user is currently on.
                        - You can take one action at a time by outputting ONE of the commands below.
                        - You will be repeatedly called after each action until you find the final answer.
                        - When you are finished and have found what the user wants, describe the final result to the user using natural spoken language (your response will be spoken via Text-to-Speech). DO NOT output any command tags in your final answer.

                        CRITICAL RULES:
                        - If the user asks you to buy/find something, output a <NAVIGATE>url</NAVIGATE> to a search page (e.g. Amazon, Google).
                        - If the search results page loads, output a <INJECT> command to click on the best product, or <SCROLL>down</SCROLL> if needed. (e.g. <INJECT>document.querySelector('a').click()</INJECT>)
                        - Keep taking actions until you reach the final product page (e.g. you see the "Add to Cart" or "Buy Now" button).
                        - Once on the final page, output your spoken response describing the product, price, etc. WITHOUT ANY COMMANDS to finish the task.
                        - Do NOT ask for user consent to navigate or click. Act autonomously.
                        - If the user asks for a recipe, navigate to a search engine, click a link, scroll to the recipe, and read it. Do this autonomously.
                        - If the user asks a conversational question or a question about the page that doesn't require further action, just speak the answer naturally WITHOUT any commands.
                        - Output ONLY ONE command per step.

                        Commands you can output (use exact format):
                        <SEARCH>your search query</SEARCH>
                        <SCROLL>down or up</SCROLL>
                        <NAVIGATE>url</NAVIGATE>
                        <INJECT>javascript code to execute on the page</INJECT>
                        
                        Keep conversational responses concise, friendly, and suitable for a voice assistant. Do not output Markdown.
""".trimIndent()

                    val messagesArray = org.json.JSONArray()
                    messagesArray.put(org.json.JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })

                    val userMsg = org.json.JSONObject().apply {
                        put("role", "user")
                        if (isFirstTurn) {
                            put("content", "User Request: $currentInput" + pageContext)
                        } else {
                            put("content", "Action executed. $pageContext\\n\\nAre you finished? If yes, speak the final answer without commands. If no, output your next command (NAVIGATE, SCROLL, INJECT, SEARCH).")
                        }
                    }
                    messagesArray.put(userMsg)

                    val jsonPayload = org.json.JSONObject().apply {
                        put("model", "qwen-2.5-32b")
                        put("messages", messagesArray)
                        put("temperature", 0.6)
                        put("max_tokens", 512)
                        put("stream", true)
                    }
'''

new_part = '''
                var isFirstTurn = true
                var iterations = 0
                val maxIterations = 5

                val messagesArray = org.json.JSONArray()
                
                val systemPrompt = """
                    You are Storm AI, an autonomous agent built into the Storm Web Browser. Your goal is to browse the web, find products, read information, and help the user proactively.
                    
                    - You are given the "CURRENT WEBPAGE TEXT:" which contains the text of the page the user is currently on.
                    - You can take one action at a time by outputting ONE of the commands below.
                    - You will be repeatedly called after each action until you find the final answer.
                    - When you are finished and have found what the user wants, describe the final result to the user using natural spoken language (your response will be spoken via Text-to-Speech). DO NOT output any command tags in your final answer.

                    CRITICAL RULES:
                    - If the user asks you to buy/find something, output a <NAVIGATE>url</NAVIGATE> to a search page (e.g. Amazon, Google).
                    - If the search results page loads, output a <INJECT> command to click on the best product, or <SCROLL>down</SCROLL> if needed. (e.g. <INJECT>document.querySelector('a').click()</INJECT>)
                    - Keep taking actions until you reach the final product page (e.g. you see the "Add to Cart" or "Buy Now" button).
                    - Once on the final page, output your spoken response describing the product, price, etc. WITHOUT ANY COMMANDS to finish the task.
                    - Do NOT ask for user consent to navigate or click. Act autonomously.
                    - If the user asks for a recipe, navigate to a search engine, click a link, scroll to the recipe, and read it. Do this autonomously.
                    - If the user asks a conversational question or a question about the page that doesn't require further action, just speak the answer naturally WITHOUT any commands.
                    - Output ONLY ONE command per step.

                    Commands you can output (use exact format):
                    <SEARCH>your search query</SEARCH>
                    <SCROLL>down or up</SCROLL>
                    <NAVIGATE>url</NAVIGATE>
                    <INJECT>javascript code to execute on the page</INJECT>
                    
                    Keep conversational responses concise, friendly, and suitable for a voice assistant. Do not output Markdown.
                """.trimIndent()
                
                messagesArray.put(org.json.JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                while (iterations < maxIterations) {
                    iterations++

                    val pageContent = viewModel.getActiveTabContent()
                    val pageContext = if (pageContent.isNotBlank()) "\\n\\nCURRENT WEBPAGE TEXT:\\n$pageContent" else ""

                    val userMsg = org.json.JSONObject().apply {
                        put("role", "user")
                        if (isFirstTurn) {
                            put("content", "User Request: $currentInput" + pageContext)
                        } else {
                            put("content", "Action executed. $pageContext\\n\\nAre you finished? If yes, speak the final answer without commands. If no, output your next command (NAVIGATE, SCROLL, INJECT, SEARCH).")
                        }
                    }
                    messagesArray.put(userMsg)

                    val jsonPayload = org.json.JSONObject().apply {
                        put("model", "llama3-70b-8192")
                        put("messages", messagesArray)
                        put("temperature", 0.6)
                        put("max_tokens", 512)
                        put("stream", true)
                    }
'''

content = content.replace(part_to_replace, new_part)

# Also after reading the full reply, add it to messagesArray
reply_save_old = '''                        val reply = fullReply.toString()
                        
                        val commandHandler = AICommandHandler(viewModel)'''

reply_save_new = '''                        val reply = fullReply.toString()
                        messagesArray.put(org.json.JSONObject().apply {
                            put("role", "assistant")
                            put("content", reply)
                        })
                        
                        val commandHandler = AICommandHandler(viewModel)'''

content = content.replace(reply_save_old, reply_save_new)

with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'w') as f:
    f.write(content)
