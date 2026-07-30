with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_code = """    fun processTextPrompt(text: String) {
        val commandHandler = AICommandHandler(viewModel)
        _aiState.value = AIState.THINKING
        currentChatJob?.cancel()
                currentChatJob = scope.launch(Dispatchers.IO) {"""

new_code = """    fun processTextPrompt(text: String) {
        val commandHandler = AICommandHandler(viewModel)
        
        // Fast path: Check for local intent
        if (commandHandler.parseAndExecuteLocalIntent(text)) {
            _aiState.value = AIState.IDLE
            _aiResponseText.value = "Done."
            return
        }

        _aiState.value = AIState.THINKING
        currentChatJob?.cancel()
        currentChatJob = scope.launch(Dispatchers.IO) {"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
