package dhp.thl.tpl.unlock.photos

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import kotlin.math.abs

class UnlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    UnlockScreen(onUnlockSuccess = {
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun UnlockScreen(onUnlockSuccess: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<String?>(null) }
    var targetPoints by remember { mutableStateOf<List<PointData>>(emptyList()) }
    var openImmediately by remember { mutableStateOf(false) }
    var showOrder by remember { mutableStateOf(false) }
    var tapTolerance by remember { mutableStateOf(0.08f) }
    var stealthMode by remember { mutableStateOf(false) }
    var showClickedPoints by remember { mutableStateOf(true) }
    var pin by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        imageUri = prefs.imageUriFlow.first()
        targetPoints = deserializePoints(prefs.pointsFlow.first())
        openImmediately = prefs.openImmediatelyFlow.first()
        showOrder = prefs.showOrderFlow.first()
        tapTolerance = prefs.tapToleranceFlow.first()
        stealthMode = prefs.stealthModeFlow.first()
        showClickedPoints = prefs.showClickedPointsFlow.first()
        pin = prefs.pinFlow.first()
    }

    var currentTaps by remember { mutableStateOf(mutableListOf<PointData>()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    fun unlock() {
        scope.launch {
            if (Shizuku.pingBinder() && !pin.isNullOrBlank()) {
                try {
                    // Use Shizuku to run adb command to input PIN and press enter
                    Shizuku.newProcess(arrayOf("sh", "-c", "input text $pin && input keyevent 66"), null, null).waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onUnlockSuccess()
        }
    }

    if (imageUri.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No image configured.", color = Color.White)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = rememberAsyncImagePainter(Uri.parse(imageUri)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (size != IntSize.Zero) {
                            val tapPct = PointData(
                                xPct = offset.x / size.width,
                                yPct = offset.y / size.height
                            )
                            currentTaps = currentTaps.toMutableList().apply { add(tapPct) }

                            var sequenceMatches = true
                            if (currentTaps.size > targetPoints.size) {
                                currentTaps = mutableListOf(tapPct) // Reset
                                sequenceMatches = false
                            } else {
                                for (i in currentTaps.indices) {
                                    val tap = currentTaps[i]
                                    val target = targetPoints[i]
                                    if (abs(tap.xPct - target.xPct) > tapTolerance || 
                                        abs(tap.yPct - target.yPct) > tapTolerance) {
                                        sequenceMatches = false
                                        break
                                    }
                                }
                            }

                            if (!sequenceMatches) {
                                if (currentTaps.size == targetPoints.size) {
                                    // User entered the complete sequence but it was wrong
                                    Toast.makeText(context, "Incorrect", Toast.LENGTH_SHORT).show()
                                    currentTaps = mutableListOf()
                                } else {
                                    // Partial mismatch, clear completely except we already added it above. Let's clear completely.
                                    // Or wait, if there is a mismatch at any point, do we clear everything?
                                    // The prompt says "if user press incorrect, output incorrect only". 
                                    // We should probably just clear it and maybe show Toast if it was the last point.
                                    // Let's clear it completely so they have to start over.
                                    currentTaps = mutableListOf()
                                    // If we want to show incorrect on every wrong tap, it might be annoying. Let's just clear.
                                    // Or we show Toast "Incorrect" when they fail.
                                    Toast.makeText(context, "Incorrect", Toast.LENGTH_SHORT).show()
                                }
                            } else if (currentTaps.size == targetPoints.size) {
                                if (openImmediately) {
                                    unlock()
                                }
                            }
                        }
                    }
                }
        )

        if (!stealthMode && showClickedPoints) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                currentTaps.forEach { pt ->
                    val x = pt.xPct * size.width
                    val y = pt.yPct * size.height
                    drawCircle(color = Color.Green, radius = 30f, center = Offset(x, y))
                }
            }
            
            if (showOrder) {
                currentTaps.forEachIndexed { index, pt ->
                    val x = pt.xPct * size.width
                    val y = pt.yPct * size.height
                    val xDp = with(density) { x.toDp() }
                    val yDp = with(density) { y.toDp() }
                    Text(
                        text = "${index + 1}",
                        color = Color.Black,
                        modifier = Modifier.offset(x = xDp - 4.dp, y = yDp - 10.dp)
                    )
                }
            }
        }

        if (!stealthMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = {
                    if (currentTaps.isNotEmpty()) {
                        currentTaps = currentTaps.toMutableList().apply { removeLast() }
                    }
                }) {
                    Text("Remove Last Point")
                }
                if (!openImmediately && currentTaps.size == targetPoints.size && currentTaps.isNotEmpty()) {
                    Button(onClick = { unlock() }) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}
