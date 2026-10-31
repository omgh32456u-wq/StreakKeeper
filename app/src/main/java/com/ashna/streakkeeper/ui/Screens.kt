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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RocketLaunch
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
import mutableStateOfStreakSession
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
    val sessionTarget by vm.sessionTarget.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The Send Session takes over the whole screen while active.
    if (sessionTarget != null) {
        SendSessionScreen(
            vm = vm,
            contact = sessionTarget!!,
            onExit = { vm.endSession() }
        )
        return
    }

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
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Today, null) },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.People, null) },
                    label = { Text("Contacts") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> TodayScreen(
                    contacts = contacts,
                    today = vm.today,
                    onStartSession = { vm.startOrAdvanceSession() },
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
fun SendSessionScreen(
    vm: StreakViewModel,
    contact: ContactEntity,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pendingCount = remember(contact.id) { derivedStateOfPendingCount(vm) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Streak Session") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Send one by one, no browsing. $pendingCount left today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StickerAvatar(contact.stickerEmoji)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "@${contact.tiktokHandle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("🔥 ${contact.streakCount}", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        StreakSession.buildMessage(contact),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val ok = StreakSession.copyMessage(context, contact)
                        vm.markOpened(contact.id)
                        val opened = TikTokHelper.openTikTok(context)
                        when {
                            !ok -> scope.launch { snackbar.showSnackbar("Couldn't access clipboard.") }
                            !opened -> scope.launch { snackbar.showSnackbar("TikTok is not installed.") }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Copy & Open TikTok")
                }
            }
            Button(
                onClick = { vm.confirmAndAdvance() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DoneAll, null)
                Spacer(Modifier.width(6.dp))
                Text("Sent ✓ — Next")
            }
            Text(
                "After sending in TikTok, come back and tap “Sent ✓”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun derivedStateOfPendingCount(vm: StreakViewModel): String = "TODO"

@Composable
fun TodayScreen(
    contacts: List<ContactEntity>,
    today: Long,
    onStartSession: () -> Unit,
    onConfirm: (Long) -> Unit,
    onOpenTikTok: (ContactEntity) -> Unit,
    onEdit: (Long) -> Unit
) {
    val done = contacts.count { it.lastCompletedEpochDay == today }
    val progress by animateFloatAsState(
        if (contacts.isEmpty()) 0f else done.toFloat() / contacts.size, label = "progress"
    )
    val pendingCount = contacts.size - done

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
        if (pendingCount > 0) {
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RocketLaunch, null)
                Spacer(Modifier.width(6.dp))
                Text("Start session ($pendingCount left)")
            }
            Spacer(Modifier.height(8.dp))
        }
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
    val isOpened = !isDone && c.lastOpenedTikTokEpochDay == Today()
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
                    StickerAvatar(c.stickerEmoji)
                    Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "@${c.tickerHandle} • ${formatTime(c.scheduleHour, c.scheduleMinute)}",
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
