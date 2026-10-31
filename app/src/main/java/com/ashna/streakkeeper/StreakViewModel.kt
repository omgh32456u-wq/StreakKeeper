package com.ashna.streakkeeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashna.streakkeeper.data.ContactEntity
import com.ashna.streakkeeper.data.StreakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StreakViewModel(val repo: StreakRepository) : ViewModel() {

    val today: Long = LocalDate.now().toEpochDay()

    val contacts: StateFlow<List<ContactEntity>> = repo.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The contact currently being handled in the session, or null when no session is active. */
    private val _sessionTarget = MutableStateFlow<ContactEntity?>(null)
    val sessionTarget: StateFlow<ContactEntity?> = _sessionTarget

    fun save(c: ContactEntity) = viewModelScope.launch { repo.upsert(c) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun confirmSent(id: Long) = viewModelScope.launch { repo.confirmSent(id) }
    fun markOpened(id: Long) = viewModelScope.launch { repo.markOpened(id) }
    fun refreshStreaks() = viewModelScope.launch { repo.maintainStreaks() }

    /** Starts or advances the session: picks the next contact not yet confirmed today. */
    fun startOrAdvanceSession() {
        viewModelScope.launch {
            val pending = repo.contactsSnapshot().filter { it.lastCompletedEpochDay != today }
            _sessionTarget.value = pending.firstOrNull()
        }
    }

    /** Confirms the current session contact and moves to the next pending one. */
    fun confirmAndAdvance() {
        val current = _sessionTarget.value ?: return
        viewModelScope.launch {
            repo.confirmSent(current.id)
            startOrAdvanceSession()
        }
    }

    fun endSession() {
        _sessionTarget.value = null
    }
}
