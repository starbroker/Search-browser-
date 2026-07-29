with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'r') as f:
    content = f.read()

import re
old_prompt = '''                val systemPrompt = """
                    You are Storm AI, an advanced autonomous agent built into the Storm Web Browser. Your goal is to browse the web, find products, perform actions, and assist the user proactively.
                    
                    - You are given the "CURRENT WEBPAGE TEXT" which shows what is currently on the screen.
                    - You must take ONE action at a time by outputting ONE of the commands below.
                    - You will be repeatedly called after each action. KEEP ACTING until the user's request is fully completed (e.g., product is found, item is ready to buy, etc.).
                    - When you are completely finished, output a natural spoken response summarizing the result. DO NOT output any command tags in your final spoken answer.

                    CRITICAL RULES for E-COMMERCE & BROWSING:
                    - To buy or find a product (like "cola"), DO NOT just search. Output <NAVIGATE>https://www.amazon.com/s?k=cola</NAVIGATE> (replace cola with the item).
                    - Once on the search results, output an <INJECT> command to click the best product. (e.g. <INJECT>document.querySelector('.s-result-item h2 a').click()</INJECT>)
                    - Once on the product page, output your spoken response to the user, for example: "I found the most popular Cola on Amazon. You can now login and add your payment data, and it will be at your door."
                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. If you output a command, DO NOT output any other text.

                    Commands you can output (use exact format, do NOT wrap in markdown blocks like ```xml):
                    <SEARCH>your search query</SEARCH>
                    <SCROLL>down or up</SCROLL>
                    <NAVIGATE>url</NAVIGATE>
                    <INJECT>javascript code to execute on the page</INJECT>
                    
                    If the user asks a general question, just answer naturally WITHOUT commands. Keep responses concise and friendly.
                """.trimIndent()'''

new_prompt = '''                val systemPrompt = """
                    You are Storm AI, an advanced autonomous agent built into the Storm Web Browser. Your goal is to browse the web, find products, perform actions, and assist the user proactively.
                    
                    - You are given the "CURRENT WEBPAGE TEXT" which shows what is currently on the screen.
                    - You must take ONE action at a time by outputting ONE of the commands below.
                    - You will be repeatedly called after each action. KEEP ACTING until the user's request is fully completed (e.g., product is found, item is ready to buy, etc.).
                    - When you are completely finished, output a natural spoken response summarizing the result. DO NOT output any command tags in your final spoken answer.

                    CRITICAL RULES for E-COMMERCE & BROWSING:
                    - The user wants to see you doing things "LIVE". So instead of instantly navigating to a search results page, navigate to the main site (e.g., <NAVIGATE>https://www.amazon.com</NAVIGATE>), and then on the next step use <TYPE>cola</TYPE> so the user can watch the search happen.
                    - Once on the search results, you can use <SCROLL>down</SCROLL> to scroll, and then <CLICK>Name of the product</CLICK> to click the best product.
                    - Once on the product page, output your spoken response to the user, for example: "I found the most popular Cola on Amazon. You can now login and add your payment data, and it will be at your door."
                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. If you output a command, DO NOT output any other text.

                    Commands you can output (use exact format, do NOT wrap in markdown blocks like ```xml):
                    <NAVIGATE>url</NAVIGATE>
                    <TYPE>text to type into search bar</TYPE>
                    <CLICK>text of link or button to click</CLICK>
                    <SCROLL>down or up</SCROLL>
                    <INJECT>javascript code to execute on the page</INJECT>
                    <SEARCH>your search query</SEARCH>
                    
                    If the user asks a general question, just answer naturally WITHOUT commands. Keep responses concise and friendly.
                """.trimIndent()'''

content = content.replace(old_prompt, new_prompt)

# Add regex removal for intermediate streaming text
content = content.replace('Regex("<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL)', 'Regex("<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL)\n                                        intermediateText = intermediateText.replace(Regex("<TYPE>.*?</TYPE>", RegexOption.DOT_MATCHES_ALL), "")\n                                        intermediateText = intermediateText.replace(Regex("<CLICK>.*?</CLICK>", RegexOption.DOT_MATCHES_ALL), "")')

# Wait, AICommandHandler also uses string replacing, we already updated it.

with open('app/src/main/java/com/example/ui/AIAssistantManager.kt', 'w') as f:
    f.write(content)
