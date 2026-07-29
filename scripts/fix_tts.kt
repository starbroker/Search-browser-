import java.io.File

fun main() {
    val file = File("app/src/main/java/com/example/ui/AIAssistantManager.kt")
    var content = file.readText()
    
    // Instead of relying on a specific voice name, let's just make sure it sets it to female, 
    // or we can set the pitch to make it sound female.
    val newVoiceCode = """
            val result = tts?.setLanguage(Locale.US)
            // Pick a female voice
            val voices = tts?.voices
            if (voices != null) {
                for (voice in voices) {
                    if (voice.name.contains("female", ignoreCase = true) || voice.name.contains("sfg", ignoreCase = true)) {
                        tts?.voice = voice
                        break
                    }
                }
            }
            // Fallback pitch adjustment if we want it to sound more female
            tts?.setPitch(1.2f)
"""
    content = content.replace("            val result = tts?.setLanguage(Locale.US)\n            // Try to pick a female voice if available\n            tts?.voices?.firstOrNull { it.name.contains(\"female\", ignoreCase = true) }?.let {\n                tts?.voice = it\n            }", newVoiceCode.trim())
    
    file.writeText(content)
}
