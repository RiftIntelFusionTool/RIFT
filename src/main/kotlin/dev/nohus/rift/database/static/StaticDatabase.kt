package dev.nohus.rift.database.static

import dev.nohus.rift.database.SqliteInitializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single

@Single
class StaticDatabase(
    sqliteInitializer: SqliteInitializer,
) {

    private val targetDatabase = Database.connect("jdbc:sqlite::resource:static.db", "org.sqlite.JDBC")

    fun <T> transaction(block: Transaction.() -> T): T {
        return transaction(targetDatabase) {
            block()
        }
    }
}
