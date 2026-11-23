package com.example.app_events.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.example.app_events.domain.Event
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "event")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val status: String,
    val availableSpots: Int,
    val listed: Boolean = false
) {
    fun toDomain() = Event(id, title, description, location, date, status, availableSpots)
}

@Entity(
    tableName = "subscription",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SubscriptionEntity(@PrimaryKey val eventId: String)

@Dao
interface EventDao {
    @Query("SELECT * FROM event WHERE listed = 1 ORDER BY date, id")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM event WHERE id = :id")
    fun observeEvent(id: String): Flow<EventEntity?>

    @Query("SELECT listed FROM event WHERE id = :id")
    suspend fun isListed(id: String): Boolean?

    @Query("UPDATE event SET listed = 0 WHERE listed = 1")
    suspend fun clearListing()

    @Upsert
    suspend fun upsert(events: List<EventEntity>)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT e.* FROM event e INNER JOIN subscription s ON e.id = s.eventId ORDER BY e.date, e.id")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM subscription WHERE eventId = :id)")
    fun observeSubscribed(id: String): Flow<Boolean>

    @Query("DELETE FROM subscription")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(subscriptions: List<SubscriptionEntity>)
}

@Database(entities = [EventEntity::class, SubscriptionEntity::class], version = 1, exportSchema = false)
abstract class EventlyDatabase : RoomDatabase() {
    abstract val events: EventDao
    abstract val subscriptions: SubscriptionDao
}
