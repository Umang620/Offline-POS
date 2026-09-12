package com.umang620.offline_pos.data.local

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ExpenseEntity::class,
        AddOnEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun addOnDao(): AddOnDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        private fun addColumnIfNotExists(db: SupportSQLiteDatabase, tableName: String, columnName: String, columnDef: String) {
            val cursor: Cursor = db.query("PRAGMA table_info(`$tableName`)")
            var exists = false
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex != -1 && cursor.getString(nameIndex) == columnName) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef")
            }
        }

        private fun applySchemaUpdates(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `add_ons` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `price` REAL NOT NULL,
                    `category` TEXT NOT NULL DEFAULT 'General',
                    `isActive` INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
            addColumnIfNotExists(db, "orders", "orderType", "TEXT DEFAULT NULL")
            addColumnIfNotExists(db, "orders", "surchargeFee", "REAL NOT NULL DEFAULT 0.0")
            addColumnIfNotExists(db, "orders", "surchargeName", "TEXT NOT NULL DEFAULT 'Delivery Fee'")
            addColumnIfNotExists(db, "order_items", "addOnsSummary", "TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_orderId` ON `order_items` (`orderId`)")
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "products", "sku", "TEXT NOT NULL DEFAULT ''")
                addColumnIfNotExists(db, "products", "isActive", "INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "products", "itemType", "TEXT NOT NULL DEFAULT 'PRODUCT'")
                addColumnIfNotExists(db, "products", "unit", "TEXT NOT NULL DEFAULT 'pcs'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                applySchemaUpdates(db)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                applySchemaUpdates(db)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                applySchemaUpdates(db)
            }
        }

        fun getDatabase(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
