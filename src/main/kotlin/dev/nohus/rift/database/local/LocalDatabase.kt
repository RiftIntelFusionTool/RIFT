package dev.nohus.rift.database.local

import dev.nohus.rift.database.SqliteInitializer
import dev.nohus.rift.utils.createNewFile
import dev.nohus.rift.utils.directories.AppDirectories
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single

@Single
class LocalDatabase(
    appDirectories: AppDirectories,
    sqliteInitializer: SqliteInitializer,
) {

    private val file = appDirectories.getAppDataDirectory().resolve("local.db").apply { createNewFile() }
    private val targetDatabase = Database.connect("jdbc:sqlite:$file", "org.sqlite.JDBC")
    private val mutex = Mutex()

    init {
        transaction(targetDatabase) {
            SchemaUtils.create(Characters)
        }
    }

    suspend fun <T> transaction(block: Transaction.() -> T): T {
        return mutex.withLock { // Mutex because SQLite support only 1 connection at a time
            transaction(targetDatabase) {
                block()
            }
        }
    }
}
