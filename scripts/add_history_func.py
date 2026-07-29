with open("app/src/main/java/com/example/ui/BrowserViewModel.kt", "r") as f:
    content = f.read()

new_func = """    fun addAIChatMessage(role: String, content: String) {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("ai_chat_history", android.content.Context.MODE_PRIVATE)
        val historyString = prefs.getString("history", "[]") ?: "[]"
        val historyArray = org.json.JSONArray(historyString)
        val newObj = org.json.JSONObject()
        newObj.put("role", role)
        newObj.put("content", content)
        historyArray.put(newObj)
        prefs.edit().putString("history", historyArray.toString()).apply()
    }
"""

# Insert before getAIChatHistory
content = content.replace("    fun getAIChatHistory()", new_func + "\n    fun getAIChatHistory()")

with open("app/src/main/java/com/example/ui/BrowserViewModel.kt", "w") as f:
    f.write(content)
