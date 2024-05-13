package dev.nohus.rift.database.local

import dev.nohus.rift.utils.createNewFile
import dev.nohus.rift.utils.directories.AppDirectories
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import org.sqlite.SQLiteConfig
import java.sql.DriverManager

@Single
class LocalDatabase(
    appDirectories: AppDirectories,
) {

    private val file = appDirectories.getAppDataDirectory().resolve("local.db").apply { createNewFile() }
    private val targetDatabase: Database
    private val mutex = Mutex()

    init {
        val config = SQLiteConfig()
        config.setTempStore(SQLiteConfig.TempStore.MEMORY)
        targetDatabase = Database.connect(getNewConnection = {
            DriverManager.getConnection("jdbc:sqlite:$file", config.toProperties())
        })

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
