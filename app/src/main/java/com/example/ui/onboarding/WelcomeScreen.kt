package com.example.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreenSheet(
    hazeState: HazeState,
    onSetupCompleted: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var isClosing by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isClosing) 0f else 1f,
        animationSpec = tween(500, easing = LinearEasing),
        finishedListener = {
            if (isClosing) onSetupCompleted()
        }
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isClosing) 0.9f else 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing)
    )

    val pages = listOf(
        Pair("Welcome to Aether", "The fluid, fast, and modern browsing experience on ColorOS 16."),
        Pair("Privacy First", "Built-in Ad-Blocker and tracking protection keep your footprint small."),
        Pair("Glassy & Elegant", "Redesigned with the aquamorphic blurry glass layers for sheer beauty.")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f * alphaAnim))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // Block touches
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    alpha = alphaAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .hazeChild(state = hazeState, style = dev.chrisbanes.haze.HazeStyle(blurRadius = 40.dp, tint = null))
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = pages[page].first,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.padding(bottom = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = pages[page].second,
                            fontSize = 16.sp,
                            color = Color(0xFF4A4A4A),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Page Indicators
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(if (isSelected) 24.dp else 10.dp)
                        val color by animateColorAsState(if (isSelected) Color(0xFF0066FF) else Color(0x660066FF))

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .height(10.dp)
                                .width(width)
                                .background(color, CircleShape)
                        )
                    }
                }

                // Next / Finish Button (Circular with arrow, glassy)
                // "Circular button with arrow blurry glassy on all pages... fix active button css"
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .size(64.dp)
                        .hazeChild(state = hazeState, style = dev.chrisbanes.haze.HazeStyle(blurRadius = 25.dp, tint = null))
                        .background(
                            if (pagerState.currentPage == 2) Color(0xFF0066FF).copy(alpha = 0.8f)
                            else Color.White.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(bounded = false, radius = 32.dp)
                        ) {
                            if (pagerState.currentPage < 2) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                isClosing = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        tint = if (pagerState.currentPage == 2) Color.White else Color(0xFF1E1E1E),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
