import re

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()

# Replace the aiMicPermissionLauncher and clickable behavior
# Wait, let's just make it check permission on click.
# The clickable block in BrowserScreen is:
old_clickable = """                .clickable { 
                    if (aiState == AIState.LISTENING) {
                        viewModel.aiAssistantManager.stopListeningAndProcess()
                    } else {
                        aiMicPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },"""

new_clickable = """                .clickable { 
                    if (aiState == AIState.LISTENING) {
                        viewModel.aiAssistantManager.stopListeningAndProcess()
                    } else {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            viewModel.aiAssistantManager.startListening()
                        } else {
                            aiMicPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },"""

content = content.replace(old_clickable, new_clickable)

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "w") as f:
    f.write(content)
