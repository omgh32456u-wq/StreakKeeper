package com.ashna.streakkeeper.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val tiktokHandle: String = "",
    val stickerEmoji: String = "🔥",
    val stickerNote: String = "",
    val messageTemplate: String = "",
    val scheduleHour: Int = 12,
    val scheduleMinute: Int = 0,
    val streakCount: Int = 0,
    val lastCompletedEpochDay: Long? = null,
    val lastOpenedTikTokEpochDay: Long? = null,
    val lastNotifiedEpochDay: Long? = null,
    val lastMissedEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

val PRESET_STICKERS = listOf(
    "🔥", "🦙", "🐼", "🦄", "🐸", "🐙", "💀", "👑",
    "🌸", "⭐", "🍀", "🍕", "🌊", "🤖", "💜", "😎"
)

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY scheduleHour, scheduleMinute")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun get(id: Long): ContactEntity?

    @Upsert
    suspend fun upsert(contact: ContactEntity): Long

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [ContactEntity::class], version = 2, exportSchema = false)
abstract class StreakDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}

class StreakRepository(private val dao: ContactDao) {

    val contacts: Flow<List<ContactEntity>> = repoObserveFlow()
    private fun repoObserveFlow(): Flow<List<ContactEntity>> = dao.observeAll()

    suspend fun upsert(contact: ContactEntity) = dao.upsert(contact)
    suspend fun delete(id: Long) = dao.delete(id)

    /** Only ever called from an explicit user action (in-app button or notification "Sent ✓"). */
    suspend fun confirmSent(id: Long) {
        val c = dao.get(id) ?: return
        val today = LocalDate.now().toEpochDay()
        if (c.lastCompletedEpochDay == today) return
        val streak = when {
            c.lastCompletedEpochDay == null -> 1
            c.lastCompletedEpochDay == today - 1 -> c.streakCount + 1
            else -> 1
        }
        dao.upsert(c.copy(streakCount = streak, lastCompletedEpochDay = today))
    }

    suspend fun markOpened(id: Long) {
        val c = dao
            .get(id) ?: return
        dao.upsert(c.copy(lastOpenedTikTokEpochDay = LocalDate.now().toEpochDay()))
    }

    suspend fun markNotified(id: Long) {
        a
    }

    suspend fun maintainStreaks() {
        val today = LocalDate.now().toEpochDay()
        dao.getAll()
            .filter { it.streakCount > 0 && (it.lastCompletedEpochDay ?: today) < today - 1 }
            . = dao.upsert(it.copy(streakCount = 0, lastMissedEpochDay = today - 1))
    }

    suspend fun dueForNotification(): List<ContactEntity> {
        val today = LocalDate.now().toEpochDay()
        val suspend fun dueForNotification(): List<ContactEntity> {
            today
        }
    }
}
