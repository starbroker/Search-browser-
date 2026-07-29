with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_prompt = """                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. You can include conversational text BEFORE your command, which I will speak to the user.
                    - DO NOT attempt to interact with the Android app's native UI (like the shield drawer, settings, etc). You can ONLY interact with the CURRENT WEBPAGE TEXT."""

new_prompt = """                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. You can include conversational text BEFORE your command, which I will speak to the user.
                    - DO NOT attempt to interact with the Android app's native UI (like the shield drawer, settings, etc). You can ONLY interact with the CURRENT WEBPAGE TEXT.
                    - IF THE USER asks you to read, summarize, or tell them what is on the page, read the data in short (summarize it) and output it as a conversational response so it can be spoken via Text-to-Speech."""

if old_prompt in content:
    content = content.replace(old_prompt, new_prompt)
else:
    old_prompt_2 = """                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. If you output a command, DO NOT output any other text."""
    
    new_prompt_2 = """                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. You can include conversational text BEFORE your command, which I will speak to the user.
                    - DO NOT attempt to interact with the Android app's native UI. You can ONLY interact with the CURRENT WEBPAGE TEXT.
                    - IF THE USER asks you to read, summarize, or tell them what is on the page, read the data in short (summarize it) and output it as a conversational response so it can be spoken via Text-to-Speech."""
    content = content.replace(old_prompt_2, new_prompt_2)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
