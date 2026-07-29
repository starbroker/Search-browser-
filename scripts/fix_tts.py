with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_code = """            val result = tts?.setLanguage(Locale.US)
            // Try to pick a female voice if available
            tts?.voices?.firstOrNull { it.name.contains("female", ignoreCase = true) }?.let {
                tts?.voice = it
            }"""

new_code = """            val result = tts?.setLanguage(Locale.US)
            // Pick a female voice
            val voices = tts?.voices
            if (voices != null) {
                var found = false
                for (v in voices) {
                    if (v.name.contains("female", ignoreCase = true) || v.name.contains("sfg", ignoreCase = true) || v.name.contains("en-us-x-sfg", ignoreCase = true)) {
                        tts?.voice = v
                        found = true
                        break
                    }
                }
                if (!found) {
                    tts?.setPitch(1.2f)
                }
            } else {
                tts?.setPitch(1.2f)
            }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
