with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()

old_code = """            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(aiBrush)
                    .clickable { viewModel.toggleAIVoicePill() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White.copy(alpha=0.6f))
                )

            }"""

new_code = """            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(aiBrush)
                    .clickable { viewModel.toggleAIVoicePill() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Terrain,
                    contentDescription = "AI Assistant",
                    tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF1C1C1E).copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "w") as f:
    f.write(content)
