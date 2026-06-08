package dhp.thl.tpl.unlock.photos

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val pin by prefs.pinFlow.collectAsState(initial = "")
    val imageUri by prefs.imageUriFlow.collectAsState(initial = null)
    val pointsStr by prefs.pointsFlow.collectAsState(initial = "")
    val openImmediately by prefs.openImmediatelyFlow.collectAsState(initial = false)
    val showOrder by prefs.showOrderFlow.collectAsState(initial = false)
    val tapTolerance by prefs.tapToleranceFlow.collectAsState(initial = 0.08f)
    val stealthMode by prefs.stealthModeFlow.collectAsState(initial = false)
    val showClickedPoints by prefs.showClickedPointsFlow.collectAsState(initial = true)
    val pointCount by prefs.pointCountFlow.collectAsState(initial = 3)

    var pinInput by remember { mutableStateOf("") }
    LaunchedEffect(pin) {
        if (pin != null) pinInput = pin!!
    }

    val points = remember(pointsStr) { deserializePoints(pointsStr).toMutableList() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch { prefs.saveImageUri(it.toString()) }
        }
    }

    var isSettingPoints by remember { mutableStateOf(false) }

    if (isSettingPoints && !imageUri.isNullOrBlank()) {
        SetPointsScreen(
            imageUri = imageUri!!,
            points = points,
            tapTolerance = tapTolerance,
            requiredPoints = pointCount,
            onPointsChanged = { newPoints ->
                scope.launch { prefs.savePoints(serializePoints(newPoints)) }
            },
            onClose = { isSettingPoints = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Unlock Setup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shizuku Status", style = MaterialTheme.typography.titleMedium)
                    var shizukuOk by remember { mutableStateOf(Shizuku.pingBinder()) }
                    if (shizukuOk) {
                        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            Button(onClick = { Shizuku.requestPermission(0) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Request Shizuku Permission")
                            }
                        } else {
                            Text("Running & Permitted", color = Color(0xFF388E3C))
                        }
                    } else {
                        Text("Not running", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { shizukuOk = Shizuku.pingBinder() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Refresh Shizuku Status")
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unlock PIN", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            pinInput = it
                            scope.launch { prefs.savePin(it) }
                        },
                        label = { Text("Device PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Preferences", style = MaterialTheme.typography.titleMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open immediately after correct points")
                        Switch(checked = openImmediately, onCheckedChange = { scope.launch { prefs.setOpenImmediately(it) } })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show order (1, 2, 3...)")
                        Switch(checked = showOrder, onCheckedChange = { scope.launch { prefs.setShowOrder(it) } })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stealth Mode (Hides UI on lock screen)")
                        Switch(checked = stealthMode, onCheckedChange = { scope.launch { prefs.setStealthMode(it) } })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show clicked points")
                        Switch(checked = showClickedPoints, onCheckedChange = { scope.launch { prefs.setShowClickedPoints(it) } })
                    }

                    Column {
                        Text("Tap Tolerance (Radius)")
                        Slider(
                            value = tapTolerance,
                            onValueChange = { scope.launch { prefs.setTapTolerance(it) } },
                            valueRange = 0.01f..0.20f,
                            steps = 19
                        )
                    }

                    Column {
                        Text("Number of Points: $pointCount")
                        Slider(
                            value = pointCount.toFloat(),
                            onValueChange = { scope.launch { prefs.setPointCount(it.toInt()) } },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Image Configuration", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (imageUri.isNullOrBlank()) "Select Image" else "Change Image")
                    }

                    if (!imageUri.isNullOrBlank()) {
                        Text("Points set: ${points.size}")
                        Button(onClick = { isSettingPoints = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Set/Edit Points")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    prefs.saveImageUri("")
                                    prefs.savePoints("")
                                }
                            }, 
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remove Image and Points")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetPointsScreen(
    imageUri: String,
    points: MutableList<PointData>,
    tapTolerance: Float,
    requiredPoints: Int,
    onPointsChanged: (List<PointData>) -> Unit,
    onClose: () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

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
                        if (size != IntSize.Zero && points.size < requiredPoints) {
                            val newPoint = PointData(
                                xPct = offset.x / size.width,
                                yPct = offset.y / size.height
                            )
                            val newList = points.toMutableList().apply { add(newPoint) }
                            onPointsChanged(newList)
                        }
                    }
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            points.forEach { pt ->
                val x = pt.xPct * size.width
                val y = pt.yPct * size.height
                // Draw tolerance radius faintly
                drawCircle(color = Color.Red.copy(alpha = 0.3f), radius = tapTolerance * size.width, center = Offset(x, y))
                // Draw exact center point
                drawCircle(color = Color.Red, radius = 15f, center = Offset(x, y))
            }
        }
        
        points.forEachIndexed { index, pt ->
            val x = pt.xPct * size.width
            val y = pt.yPct * size.height
            val xDp = with(density) { x.toDp() }
            val yDp = with(density) { y.toDp() }
            Text(
                text = "${index + 1}",
                color = Color.White,
                modifier = Modifier.offset(x = xDp - 4.dp, y = yDp - 10.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                if (points.isNotEmpty()) {
                    val newList = points.toMutableList().apply { removeLast() }
                    onPointsChanged(newList)
                }
            }) {
                Text("Remove Last Point")
            }
            Button(
                onClick = onClose,
                enabled = points.size == requiredPoints
            ) {
                Text("Done (${points.size}/$requiredPoints)")
            }
        }
    }
}
