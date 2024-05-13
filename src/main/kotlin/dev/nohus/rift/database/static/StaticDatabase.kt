package dev.nohus.rift.database.static

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single
import org.sqlite.SQLiteConfig
import java.sql.DriverManager

@Single
class StaticDatabase {

    private val targetDatabase: Database

    init {
        val config = SQLiteConfig()
        config.setTempStore(SQLiteConfig.TempStore.MEMORY)
        targetDatabase = Database.connect(getNewConnection = {
            DriverManager.getConnection("jdbc:sqlite::resource:static.db", config.toProperties())
        })
    }

    fun <T> transaction(block: Transaction.() -> T): T {
        return transaction(targetDatabase) {
            block()
        }
    }
}
