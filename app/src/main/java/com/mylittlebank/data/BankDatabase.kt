package com.mylittlebank.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "activity_type")
data class ActivityTypeEntity(
    @PrimaryKey val key: String,
    val title: String,
    val emoji: String,
    val unit: String,
    val enabled: Boolean = true
)

@Entity(tableName = "reward_rule", foreignKeys = [ForeignKey(
    entity = ActivityTypeEntity::class,
    parentColumns = ["key"], childColumns = ["activityKey"], onDelete = ForeignKey.CASCADE
)], indices = [Index("activityKey")])
data class RewardRuleEntity(
    @PrimaryKey val activityKey: String,
    val ratePoints: Long,
    val effectiveAt: Long,
    val dailyQuantityLimit: Long? = null,
    val dailyPointsLimit: Long? = null
)

@Entity(tableName = "ledger_transaction", indices = [Index(value = ["idempotencyKey"], unique = true)])
data class LedgerTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String,
    val kind: String,
    val description: String,
    val createdAt: Long
)

@Entity(tableName = "ledger_entry", foreignKeys = [ForeignKey(
    entity = LedgerTransactionEntity::class,
    parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.RESTRICT
)], indices = [Index("transactionId"), Index("account")])
data class LedgerEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val account: String,
    val pointsDelta: Long
)

data class ActivityWithRule(
    @androidx.room.Embedded val activity: ActivityTypeEntity,
    @androidx.room.ColumnInfo(name = "ratePoints") val ratePoints: Long,
    @androidx.room.ColumnInfo(name = "dailyQuantityLimit") val dailyQuantityLimit: Long?,
    @androidx.room.ColumnInfo(name = "dailyPointsLimit") val dailyPointsLimit: Long?
)

data class PassbookRow(
    val id: String,
    val kind: String,
    val description: String,
    val createdAt: Long,
    val pointsDelta: Long
)

@Dao
interface BankDao {
    @Query("SELECT a.*, r.ratePoints, r.dailyQuantityLimit, r.dailyPointsLimit FROM activity_type a JOIN reward_rule r ON r.activityKey = a.key ORDER BY a.rowid")
    fun observeActivities(): Flow<List<ActivityWithRule>>
    @Query("UPDATE activity_type SET enabled = :enabled WHERE key = :key") suspend fun setActivityEnabled(key: String, enabled: Boolean)

    @Query("SELECT COALESCE(SUM(pointsDelta), 0) FROM ledger_entry WHERE account = :account")
    fun observeBalance(account: String): Flow<Long>

    @Query("SELECT t.id, t.kind, t.description, t.createdAt, COALESCE(SUM(e.pointsDelta), 0) AS pointsDelta FROM ledger_transaction t JOIN ledger_entry e ON e.transactionId = t.id WHERE e.account = 'AVAILABLE' GROUP BY t.id ORDER BY t.createdAt DESC LIMIT 100")
    fun observePassbook(): Flow<List<PassbookRow>>

    @Query("SELECT * FROM activity_type WHERE key = :key") suspend fun activity(key: String): ActivityTypeEntity?
    @Query("SELECT * FROM reward_rule WHERE activityKey = :key") suspend fun rule(key: String): RewardRuleEntity?
    @Query("SELECT COALESCE(SUM(pointsDelta), 0) FROM ledger_entry WHERE account = :account") suspend fun balance(account: String): Long
    @Query("SELECT COALESCE(SUM(pointsDelta), 0) FROM ledger_entry WHERE account = :account AND transactionId IN (SELECT id FROM ledger_transaction WHERE kind = 'REWARD') AND transactionId IN (SELECT transactionId FROM activity_submission WHERE activityKey = :key AND submittedAt >= :start AND submittedAt < :end)") suspend fun dailyReward(account: String, key: String, start: Long, end: Long): Long
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM activity_submission WHERE activityKey = :key AND submittedAt >= :start AND submittedAt < :end") suspend fun dailyQuantity(key: String, start: Long, end: Long): Long

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTransaction(entity: LedgerTransactionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEntries(entities: List<LedgerEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSubmission(entity: ActivitySubmissionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRule(rule: RewardRuleEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seedActivities(items: List<ActivityTypeEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seedRules(items: List<RewardRuleEntity>)

    @Transaction
    suspend fun initialize() {
        seedActivities(listOf(
            ActivityTypeEntity("READING", "Reading", "📚", "page"),
            ActivityTypeEntity("MATHS", "Maths", "🧮", "correct answer"),
            ActivityTypeEntity("HOMEWORK", "Homework", "✏️", "task"),
            ActivityTypeEntity("CHORES", "Chores", "🧹", "chore"),
            ActivityTypeEntity("GOOD_HABITS", "Good habits", "🌱", "completion")
        ))
        seedRules(listOf(
            RewardRuleEntity("READING", 5, System.currentTimeMillis()),
            RewardRuleEntity("MATHS", 2, System.currentTimeMillis()),
            RewardRuleEntity("HOMEWORK", 10, System.currentTimeMillis()),
            RewardRuleEntity("CHORES", 10, System.currentTimeMillis()),
            RewardRuleEntity("GOOD_HABITS", 5, System.currentTimeMillis())
        ))
    }

    @Transaction
    suspend fun postReward(activityKey: String, quantity: Long, note: String, idempotencyKey: String, now: Long): Long {
        require(quantity > 0) { "Enter a quantity greater than zero." }
        val activity = activity(activityKey) ?: error("This activity is unavailable.")
        val rule = rule(activityKey) ?: error("A reward has not been set for this activity.")
        check(activity.enabled) { "This activity is currently unavailable." }
        val earned = Math.multiplyExact(rule.ratePoints, quantity)
        val zone = java.time.ZoneId.systemDefault()
        val day = java.time.LocalDate.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        rule.dailyQuantityLimit?.let { cap ->
            check(Math.addExact(dailyQuantity(activityKey, start, end), quantity) <= cap) { "Today's activity limit has been reached. Try again tomorrow!" }
        }
        rule.dailyPointsLimit?.let { cap ->
            check(Math.addExact(dailyReward("AVAILABLE", activityKey, start, end), earned) <= cap) { "Today's points limit has been reached. Try again tomorrow!" }
        }
        val id = UUID.randomUUID().toString()
        val transaction = LedgerTransactionEntity(id = id, idempotencyKey = idempotencyKey, kind = "REWARD", description = note.ifBlank { activity.title }, createdAt = now)
        insertTransaction(transaction)
        insertEntries(listOf(
            LedgerEntryEntity(transactionId = id, account = "AVAILABLE", pointsDelta = earned),
            LedgerEntryEntity(transactionId = id, account = "REWARD_FUND", pointsDelta = Math.negateExact(earned))
        ))
        insertSubmission(ActivitySubmissionEntity(idempotencyKey = idempotencyKey, activityKey = activityKey, quantity = quantity, submittedAt = now, transactionId = id))
        check(earned + Math.negateExact(earned) == 0L)
        return earned
    }

    @Transaction
    suspend fun transfer(points: Long, toSavings: Boolean, now: Long, idempotencyKey: String) {
        require(points > 0) { "Enter an amount greater than zero." }
        val from = if (toSavings) "AVAILABLE" else "SAVINGS"
        val to = if (toSavings) "SAVINGS" else "AVAILABLE"
        check(balance(from) >= points) { if (toSavings) "There are not enough points in your available balance." else "There are not enough points in savings." }
        val id = UUID.randomUUID().toString()
        insertTransaction(LedgerTransactionEntity(id = id, idempotencyKey = idempotencyKey, kind = if (toSavings) "SAVE" else "WITHDRAW", description = if (toSavings) "Moved to savings" else "Moved from savings", createdAt = now))
        insertEntries(listOf(LedgerEntryEntity(transactionId = id, account = from, pointsDelta = Math.negateExact(points)), LedgerEntryEntity(transactionId = id, account = to, pointsDelta = points)))
    }
}

@Entity(tableName = "activity_submission", indices = [Index(value = ["idempotencyKey"], unique = true), Index("activityKey")])
data class ActivitySubmissionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idempotencyKey: String,
    val activityKey: String,
    val quantity: Long,
    val submittedAt: Long,
    val transactionId: String
)

@Database(entities = [ActivityTypeEntity::class, RewardRuleEntity::class, LedgerTransactionEntity::class, LedgerEntryEntity::class, ActivitySubmissionEntity::class], version = 1, exportSchema = true)
abstract class BankDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao

    companion object {
        @Volatile private var instance: BankDatabase? = null
        fun get(context: Context): BankDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, BankDatabase::class.java, "my_little_bank.db").build().also { instance = it }
        }
    }
}
