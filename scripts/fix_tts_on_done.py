with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_code = """                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        _aiState.value = AIState.IDLE
                    }
                }"""

new_code = """                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        if (_aiState.value == AIState.SPEAKING) {
                            _aiState.value = AIState.IDLE
                        }
                    }
                }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
