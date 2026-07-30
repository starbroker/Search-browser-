with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_prompt = """                    - Output ONLY ONE command per step. If you output a command, DO NOT output any other text.
                    
                    Commands you can output (use exact format, do NOT wrap in markdown blocks like ```xml):"""

new_prompt = """                    - Output ONLY ONE command per step. You can include conversational text BEFORE your command, which I will speak to the user.
                    - DO NOT attempt to interact with the Android app's native UI (like the shield drawer, settings, etc). You can ONLY interact with the CURRENT WEBPAGE TEXT.
                    
                    Commands you can output (use exact format, do NOT wrap in markdown blocks like ```xml):"""

content = content.replace(old_prompt, new_prompt)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
