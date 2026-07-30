import re

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "r") as f:
    content = f.read()

# Fix the stray block
stray_block = """                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        if (_aiState.value == AIState.THINKING) {
                                            _aiState.value = AIState.IDLE
                                        }
                                    }
                                    } catch (e: Exception) {}"""

fixed_block = """                                } catch (e: Exception) {}"""

content = content.replace(stray_block, fixed_block)

# Add ADD_TAB stripping
content = content.replace(
    'intermediateText = intermediateText.replace(Regex("<NAVIGATE>.*?</NAVIGATE>", RegexOption.DOT_MATCHES_ALL), "")',
    'intermediateText = intermediateText.replace(Regex("<NAVIGATE>.*?</NAVIGATE>", RegexOption.DOT_MATCHES_ALL), "")\n                                        intermediateText = intermediateText.replace(Regex("<ADD_TAB>.*?</ADD_TAB>", RegexOption.DOT_MATCHES_ALL), "")'
)

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(content)
