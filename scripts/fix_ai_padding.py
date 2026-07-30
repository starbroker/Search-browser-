with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "r") as f:
    content = f.read()

old_code = """            val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
            val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(androidx.compose.ui.platform.LocalDensity.current)
            val isIme = imeBottom > navBottom
            val bottomPadding = if (isIme) with(androidx.compose.ui.platform.LocalDensity.current) { imeBottom.toDp() } + 1.dp else if (isTablet) 8.dp else 86.dp
            val modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding)"""

new_code = """            val density = androidx.compose.ui.platform.LocalDensity.current
            val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density)
            val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(density)
            val isIme = imeBottom > navBottom
            val navBottomDp = with(density) { navBottom.toDp() }
            
            val basePadding = if (isTablet) {
                8.dp
            } else if (showTabs) {
                112.dp
            } else {
                82.dp
            }
            
            val bottomPadding = if (isIme) {
                with(density) { imeBottom.toDp() } + 1.dp
            } else {
                basePadding + navBottomDp
            }
            
            val modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding)"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/BrowserScreen.kt", "w") as f:
    f.write(content)
