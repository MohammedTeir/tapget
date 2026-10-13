package com.tapget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val Blue = Color(0xFF1769FF)
private val Cyan = Color(0xFF06B6D4)
private val LightBg = Color(0xFFF5F7FB)
private val DarkBg = Color(0xFF0B1220)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TapGetApp() }
    }
}

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }
data class DownloadItem(
    val id: Int, val name: String, val size: String, val url: String,
    val status: DownloadStatus, val progress: Int = 0, val speed: String = ""
)

@Composable
fun TapGetApp() {
    var dark by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Downloads") }
    var showAdd by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DownloadItem?>(null) }
    var nextId by remember { mutableIntStateOf(5) }
    var items by remember {
        mutableStateOf(listOf(
            DownloadItem(1, "Product_demo.mp4", "128 MB", "https://example.com/product_demo.mp4", DownloadStatus.DOWNLOADING, 63, "4.8 MB/s"),
            DownloadItem(2, "Design_assets.zip", "42 MB", "https://example.com/design_assets.zip", DownloadStatus.QUEUED),
            DownloadItem(3, "English_lesson_13.mp3", "18 MB", "https://example.com/lesson13.mp3", DownloadStatus.PAUSED, 41),
            DownloadItem(4, "TapGet_guide.pdf", "2.4 MB", "https://example.com/guide.pdf", DownloadStatus.COMPLETED, 100)
        ))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            items = items.map { if (it.status == DownloadStatus.DOWNLOADING && it.progress < 100) {
                val p = (it.progress + 2).coerceAtMost(100)
                it.copy(progress = p, status = if (p == 100) DownloadStatus.COMPLETED else it.status)
            } else it }
        }
    }
    val colorScheme = if (dark) darkColorScheme(primary = Color(0xFF75A5FF), secondary = Cyan,
        background = DarkBg, surface = Color(0xFF151F30), onSurface = Color(0xFFF1F5F9),
        onBackground = Color(0xFFF1F5F9)) else lightColorScheme(primary = Blue, secondary = Cyan,
        background = LightBg, surface = Color.White, onSurface = Color(0xFF0F172A),
        onBackground = Color(0xFF0F172A))
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    listOf("Downloads", "Queue", "Completed", "Settings").forEach { destination ->
                        NavigationBarItem(
                            selected = tab == destination,
                            onClick = { tab = destination; selected = null },
                            icon = { Icon(when(destination) {
                                "Downloads" -> Icons.Default.Download
                                "Queue" -> Icons.Default.Schedule
                                "Completed" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Settings
                            }, contentDescription = destination) },
                            label = { Text(destination) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab != "Settings") FloatingActionButton(onClick = { showAdd = true }, containerColor = Blue, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "Add download")
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (selected != null) {
                    DetailScreen(selected!!, onBack = { selected = null }, onAction = { action ->
                        items = items.map { item ->
                            if (item.id != selected!!.id) item else when(action) {
                                "Pause" -> item.copy(status = DownloadStatus.PAUSED, speed = "")
                                "Resume" -> item.copy(status = DownloadStatus.DOWNLOADING, speed = "2.1 MB/s")
                                "Retry" -> item.copy(status = DownloadStatus.QUEUED, progress = 0, speed = "")
                                "Cancel" -> item.copy(status = DownloadStatus.CANCELLED, speed = "")
                                else -> item
                            }
                        }
                        selected = items.firstOrNull { it.id == selected!!.id }
                    })
                } else when(tab) {
                    "Downloads" -> DownloadsScreen(items, search, showSearch, dark,
                        onSearchToggle = { showSearch = !showSearch; if (!showSearch) search = "" },
                        onSearch = { search = it }, onSelect = { selected = it },
                        onPauseResume = { item -> items = items.map { if (it.id == item.id) it.copy(status = if (it.status == DownloadStatus.PAUSED) DownloadStatus.DOWNLOADING else DownloadStatus.PAUSED, speed = "") else it } },
                        onCancel = { item -> items = items.map { if (it.id == item.id) it.copy(status = DownloadStatus.CANCELLED, speed = "") else it } })
                    "Queue" -> QueueScreen(items.filter { it.status == DownloadStatus.QUEUED }, onSelect = { selected = it },
                        onStart = { id -> items = items.map { if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING, speed = "2.1 MB/s") else it } })
                    "Completed" -> CompletedScreen(items.filter { it.status == DownloadStatus.COMPLETED }, onSelect = { selected = it },
                        onRemove = { item -> items = items.filterNot { it.id == item.id } })
                    else -> SettingsScreen(dark, onDark = { dark = it })
                }
            }
        }
        if (showAdd) AddDownloadDialog(onDismiss = { showAdd = false }, onAdd = { url, name ->
            items = items + DownloadItem(nextId++, name.ifBlank { url.substringAfterLast('/').ifBlank { "download.bin" } },
                "Unknown size", url, DownloadStatus.DOWNLOADING, 0, "Connecting…")
            showAdd = false; tab = "Downloads"
        })
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f), fontSize = 13.sp)
        }
        action?.invoke()
    }
}

@Composable
private fun BrandHeader(
    dark: Boolean,
    subtitle: String,
    action: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val logoFile = if (dark) "brand/tapget-logo-reversed.png" else "brand/tapget-logo-primary.png"
    val logoBitmap = remember(logoFile) {
        runCatching {
            context.assets.open(logoFile).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap,
                    contentDescription = "TapGet",
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .heightIn(max = 44.dp),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Text(
                    text = "TapGet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        action?.invoke()
    }
}

@Composable
private fun DownloadsScreen(
    items: List<DownloadItem>, search: String, searching: Boolean, dark: Boolean,
    onSearchToggle: () -> Unit, onSearch: (String) -> Unit, onSelect: (DownloadItem) -> Unit,
    onPauseResume: (DownloadItem) -> Unit, onCancel: (DownloadItem) -> Unit
) {
    BrandHeader(dark = dark, subtitle = "Fast. Organized. Under control.") {
        IconButton(onClick = onSearchToggle, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Search, "Search downloads") }
    }
    if (searching) OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        singleLine = true, label = { Text("Search downloads") }, leadingIcon = { Icon(Icons.Default.Search, null) })
    val visible = items.filter { it.name.contains(search, true) || it.url.contains(search, true) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryPill("Active", items.count { it.status == DownloadStatus.DOWNLOADING })
        SummaryPill("Queued", items.count { it.status == DownloadStatus.QUEUED })
        SummaryPill("Done", items.count { it.status == DownloadStatus.COMPLETED })
    }
    if (visible.isEmpty()) EmptyState("Nothing here yet", "Add a URL to start your first download.")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(visible, key = { it.id }) { item ->
            DownloadCard(item, onClick = { onSelect(item) }, onPauseResume = { onPauseResume(item) }, onCancel = { onCancel(item) })
        }
    }
}

@Composable
private fun SummaryPill(label: String, count: Int) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$count", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp)); Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DownloadCard(item: DownloadItem, onClick: () -> Unit, onPauseResume: () -> Unit, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .10f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    StatusChip(item.status)
                }
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> IconButton(onClick = onPauseResume, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Pause, "Pause download")
                    }
                    DownloadStatus.PAUSED -> IconButton(onClick = onPauseResume, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.PlayArrow, "Resume download")
                    }
                    DownloadStatus.QUEUED -> Icon(Icons.Default.Schedule, contentDescription = "Queued")
                    DownloadStatus.FAILED -> Icon(Icons.Default.ErrorOutline, contentDescription = "Failed", tint = statusColor(item.status))
                    else -> {}
                }
                if (item.status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.QUEUED)) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Cancel download") }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.COMPLETED) {
                LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (item.status == DownloadStatus.COMPLETED) item.size else "${item.progress}% · ${item.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), modifier = Modifier.weight(1f))
                    if (item.status == DownloadStatus.DOWNLOADING && item.speed.isNotBlank()) Text(item.speed, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            } else Text(item.size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val tint = statusColor(status)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = if (MaterialTheme.colorScheme.background == DarkBg) 0.18f else 0.10f)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = when (status) {
                    DownloadStatus.QUEUED -> Icons.Default.Schedule
                    DownloadStatus.DOWNLOADING -> Icons.Default.Downloading
                    DownloadStatus.PAUSED -> Icons.Default.PauseCircle
                    DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                    DownloadStatus.FAILED -> Icons.Default.Error
                    DownloadStatus.CANCELLED -> Icons.Default.Cancel
                },
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(statusLabel(status), color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun statusLabel(status: DownloadStatus) = when(status) {
    DownloadStatus.QUEUED -> "In queue"
    DownloadStatus.DOWNLOADING -> "Downloading"
    DownloadStatus.PAUSED -> "Paused"
    DownloadStatus.COMPLETED -> "Completed"
    DownloadStatus.FAILED -> "Failed"
    DownloadStatus.CANCELLED -> "Cancelled"
}
@Composable
private fun statusColor(status: DownloadStatus): Color = when(status) {
    DownloadStatus.QUEUED -> Color(0xFF64748B)
    DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
    DownloadStatus.PAUSED -> if (MaterialTheme.colorScheme.background == DarkBg) Color(0xFFFBBF24) else Color(0xFFB45309)
    DownloadStatus.COMPLETED -> if (MaterialTheme.colorScheme.background == DarkBg) Color(0xFF4ADE80) else Color(0xFF15803D)
    DownloadStatus.FAILED -> if (MaterialTheme.colorScheme.background == DarkBg) Color(0xFFF87171) else Color(0xFFDC2626)
    DownloadStatus.CANCELLED -> Color(0xFF64748B)
}

@Composable
private fun QueueScreen(items: List<DownloadItem>, onSelect: (DownloadItem) -> Unit, onStart: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Queue", "Downloads waiting to start")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("${items.size} queued", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Start a download when you're ready.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (items.isEmpty()) {
            EmptyState("Queue is clear", "New queued downloads will appear here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(item.size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                StatusChip(item.status)
                            }
                            IconButton(onClick = { onStart(item.id) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start ${item.name}", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CompletedScreen(items: List<DownloadItem>, onSelect: (DownloadItem) -> Unit, onRemove: (DownloadItem) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Completed", "Your finished downloads")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF15803D).copy(alpha = .10f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (MaterialTheme.colorScheme.background == DarkBg) Color(0xFF4ADE80) else Color(0xFF15803D))
                }
            }
            Column(Modifier.weight(1f)) {
                Text("${items.size} completed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Files ready to open or manage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (items.isEmpty()) {
            EmptyState("No completed downloads", "Finished files will be collected here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF15803D).copy(alpha = .10f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (MaterialTheme.colorScheme.background == DarkBg) Color(0xFF4ADE80) else Color(0xFF15803D))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(item.size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                StatusChip(item.status)
                            }
                            IconButton(onClick = { onRemove(item) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${item.name} from history")
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DetailScreen(item: DownloadItem, onBack: () -> Unit, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Download details",
            "File information and actions",
            { IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    StatusChip(item.status)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Progress", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text("${item.progress}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("File size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text(item.size, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    if (item.speed.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Transfer speed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(item.speed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Source URL", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(item.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text("Prototype details only. Real file transfer is not enabled yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> Button(onClick = { onAction("Pause") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Pause, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Pause") }
                    DownloadStatus.PAUSED -> Button(onClick = { onAction("Resume") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Resume") }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> Button(onClick = { onAction("Retry") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Retry") }
                    else -> {}
                }
                if (item.status != DownloadStatus.COMPLETED && item.status != DownloadStatus.CANCELLED) {
                    OutlinedButton(onClick = { onAction("Cancel") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Close, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Cancel") }
                }
            }
        }
    }
}


@Composable
private fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", "Make TapGet work your way")
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("APPEARANCE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (dark) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Dark theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(if (dark) "Comfortable in low light" else "Bright, clean appearance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = dark, onCheckedChange = onDark)
                    }
                }
            }
            item {
                Text("DOWNLOADS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsRow(Icons.Default.FolderOpen, "Storage location", "Destination picker will be connected in the download-engine phase")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                        SettingsRow(Icons.Default.Wifi, "Network", "Connection and data preferences")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                        SettingsRow(Icons.Default.Notifications, "Notifications", "Transfer and completion alerts")
                    }
                }
            }
            item {
                Text("ABOUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SettingsRow(Icons.Default.Info, "TapGet", "Fast. Organized. Under control.")
                    Text(
                        "UI prototype · transfers are simulated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .09f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
private fun EmptyState(title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .10f), modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
        }
    }
}

@Composable
private fun AddDownloadDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Add download", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter a direct link to the file you want to download.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = false },
                    label = { Text("File URL") },
                    placeholder = { Text("https://example.com/file.zip") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    isError = error,
                    supportingText = { if (error) Text("Enter a valid HTTP or HTTPS URL.") else Text("HTTP and HTTPS links are supported in the planned engine.") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("File name (optional)") },
                    leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("This prototype adds a simulated transfer; real downloading is the next development milestone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = url.trim()
                val valid = value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)
                if (valid && value.substringAfter("://").isNotBlank()) onAdd(value, name.trim()) else error = true
            }) { Text("Add download") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
