const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/BrowserScreen.kt', 'utf8');

const injection = `
            appUpdateProposal?.let { update ->
                AlertDialog(
                    onDismissRequest = { viewModel.appUpdateProposal.value = null },
                    title = {
                        Text(
                            text = "Update Available: " + update.latestVersion,
                            fontFamily = activeFont,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "A new version of this app is available.\\n\\nRelease Notes:\\n" + update.releaseNotes,
                                fontFamily = activeFont,
                                maxLines = 10,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.appUpdateProposal.value = null
                                val defaultUrl = "https://github.com/HimankC/StormX/releases/latest"
                                val url = if (update.downloadUrl.isNotEmpty()) update.downloadUrl else defaultUrl
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            },
                        ) {
                            Text(text = "Download Update", fontFamily = activeFont)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.appUpdateProposal.value = null }) {
                            Text(text = "Later", fontFamily = activeFont)
                        }
                    },
                    containerColor = glassCardColor(isDark),
                    tonalElevation = 10.dp
                )
            }
`;

const matchStr = "                    tonalElevation = 10.dp\n                )\n            }";
const idx = code.indexOf(matchStr, code.indexOf("permissionProposal?.let"));

if (idx !== -1) {
    code = code.substring(0, idx + matchStr.length) + "\n" + injection + code.substring(idx + matchStr.length);
    fs.writeFileSync('app/src/main/java/com/example/ui/BrowserScreen.kt', code);
    console.log("Injected");
} else {
    console.log("Not matched");
}
