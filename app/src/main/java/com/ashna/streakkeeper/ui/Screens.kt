package com.ashna.streakkeeper.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashna.streakkeeper.StreakViewModel
import com.ashna.streakkeeper.TikTokHelper
import com.ashna.streakkeeper.data.ContactEntity
import com.ashna.streakkeeper.data.PRESET_STICKERS
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun formatTime(h: Int, m: Int) = "%02d:%02d".format(h, m)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: StreakViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val contacts by vm.contacts.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (editingId != null) {
        val initial = contacts.firstOrNull { it.id == editingId } ?: ContactEntity()
        EditContactScreen(
            initial = initial,
            isNew = editingId == -1L,
            onSave = { vm.save(it); editingId = null },
            onCancel = { editingId = null }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 },
                    { Icon(Icons.Default.Today, null) }, { Text("Today") })
                NavigationBarItem(tab == 1, { tab = 1 },
                    { Icon(Icons.Default.People, null) }, { Text("Contacts") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> TodayScreen(
                    contacts = contacts,
                    today = vm.today,
                    onConfirm = { vm.confirmSent(it) },
                    onOpenTikTok = { c ->
                        vm.markOpened(c.id)
                        if (!TikTokHelper.openTikTok(context)) {
                            scope.launch {
                                snackbar.showSnackbar("TikTok is not installed on this device.")
                            }
                        }
                    },
                    onEdit = { editingId = it }
                )
                else -> ContactsScreen(
                    contacts = contacts,
                    onAdd = { editingId = -1L },
                    onEdit = { editingId = it },
                    onDelete = { vm.delete(it) }
                )
            }
        }
    }
}

@Composable
fun TodayScreen(
    contacts: List<ContactEntity>,
    today: Long,
    onConfirm: (Long) -> Unit,
    onOpenTikTok: (ContactEntity) -> Unit,
    onEdit: (Long) -> Unit
) {
    val done = contacts.count { it.lastCompletedEpochDay == today }
    val progress by animateFloatAsState(
        if (contacts.isEmpty()) 0f else done.toFloat() / contacts.size, label = "progress"
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            LocalDate.ofEpochDay(today).let {
                it.dayOfWeek.name.lowercase().replaceFirstChar { c -> c.uppercase() } +
                    ", " + it.dayOfMonth + "/" + it.monthValue
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$done of ${contacts.size} done", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("🔥", fontSize = 20.sp)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (contacts.isEmpty()) {
            EmptyState("No contacts yet.\nAdd your first TikTok friend in the Contacts tab.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    contacts.sortedWith(compareBy { it.lastCompletedEpochDay == today }),
                    key = { it.id }
                ) { c ->
                    ContactTaskCard(c, today, onConfirm, onOpenTikTok, onEdit)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ContactTaskCard(
    c: ContactEntity,
    today: Long,
    onConfirm: (Long) -> Unit,
    onOpenTikTok: (ContactEntity) -> Unit,
    onEdit: (Long) -> Unit
) {
    val isDone = c.lastCompletedEpochDay == today
    val isOpened = !isDone && c.lastOpenedTikTokEpochDay == today
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { onEdit(c.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StickerAvatar(c.stickerEmoji)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "@${c.tiktokHandle} • ${formatTime(c.scheduleHour, c.scheduleMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (c.stickerNote.isNotBlank()) {
                        Text("Sticker: ${c.stickerNote}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("🔥 ${c.streakCount}", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(isDone, isOpened)
                Spacer(Modifier.weight(1f))
                if (!isDone) {
                    OutlinedButton(onClick = { onOpenTikTok(c) }) { Text("Open TikTok") }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { onConfirm(c.id) }) { Text("Sent ✓") }
                }
            }
        }
    }
}

@Composable
fun StickerAvatar(emoji: String) {
    Box(
        Modifier.size(52.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 26.sp) }
}

@Composable
fun StatusChip(done: Boolean, opened: Boolean) {
    val text = when {
        done -> "Confirmed sent"
        opened -> "Opened in TikTok"
        else -> "Pending"
    }
    val color = when {
        done -> Color(0xFF4ADE80)
        opened -> Color(0xFFFACC15)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(text) },
        leadingIcon = {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
    )
}

@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add contact") }
            )
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("No contacts yet.\nTap \"Add contact\" to begin.")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contacts, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { onEdit(c.id) }) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StickerAvatar(c.stickerEmoji)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "@${c.tiktokHandle} • ${formatTime(c.scheduleHour, c.scheduleMinute)} daily",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("🔥 ${c.streakCount}", style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { onDelete(c.id) }) {
                                Icon(Icons.Default.Delete, "Delete contact")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    initial: ContactEntity,
    isNew: Boolean,
    onSave: (ContactEntity) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var handle by remember { mutableStateOf(initial.tiktokHandle) }
    var sticker by remember { mutableStateOf(initial.stickerEmoji.ifBlank { "🔥" }) }
    var note by remember { mutableStateOf(initial.stickerNote) }
    val time = rememberTimePickerState(
        initialHour = initial.scheduleHour,
        initialMinute = initial.scheduleMinute,
        is24Hour = true
    )

    fun build(): ContactEntity? {
        if (name.isBlank()) return null
        return initial.copy(
            name = name.trim(),
            tiktokHandle = handle.trim().removePrefix("@"),
            stickerEmoji = sticker.ifBlank { "🔥" },
            stickerNote = note.trim(),
            scheduleHour = time.hour,
            scheduleMinute = time.minute
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New contact" else "Edit contact") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    TextButton(onClick = { build()?.let(onSave) }, enabled = name.isNotBlank()) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, isError = name.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = handle, onValueChange = { handle = it },
                label = { Text("TikTok username (optional)") },
                prefix = { Text("@") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Sticker for this contact", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_STICKERS.forEach { emoji ->
                    val selected = emoji == sticker
                    Box(
                        Modifier.size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { sticker = emoji },
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 24.sp) }
                }
            }
            OutlinedTextField(
                value = sticker, onValueChange = { sticker = it.take(4) },
                label = { Text("Custom sticker emoji") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Sticker note (e.g. \"the dancing llama\")") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Daily reminder time", style = MaterialTheme.typography.titleSmall)
            TimePicker(state = time)
            Button(
                onClick = { build()?.let(onSave) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isNew) "Add contact" else "Save changes") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
