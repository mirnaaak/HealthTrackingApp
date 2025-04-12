import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.healthtrackingapp.History

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "HealthDatabase.db"  // Name of the database
        private const val DATABASE_VERSION = 4 // Database version
    }

    // Cache to store the history data in memory
    private val historyCache = mutableListOf<History>()

    override fun onCreate(db: SQLiteDatabase) {
        // Create the History table with Date as TEXT and formatted as 'yyyy-MM-dd HH:mm:ss'
        val createTable1 = """
        CREATE TABLE History (
            Date TEXT PRIMARY KEY, 
            HR INTEGER, 
            SPO2 INTEGER,
            Status TEXT
        )
        """

        // Create the EContact table with PhoneNumber as TEXT
        val createTable2 = """
        CREATE TABLE EContact (
            PhoneNumber TEXT PRIMARY KEY
        )
        """

        // Execute the SQL statements
        db.execSQL(createTable1)
        db.execSQL(createTable2)
    }

    // Called when the database version is updated
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Check if the database version is less than 2
        if (oldVersion < 2) {
            // Add the Status column to the History table if it's missing
            db.execSQL("ALTER TABLE History ADD COLUMN Status TEXT")
        }
        // No need to drop tables, we just add new columns
    }

    // Caching and fetching all histories
    @SuppressLint("Range")
    fun getAllHistories(): List<History> {
        // If cache is not empty, return cached data
        if (historyCache.isNotEmpty()) {
            return historyCache
        }

        // Otherwise, load from the database
        val histories = mutableListOf<History>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM History ORDER BY Date DESC", null)
        if (cursor.moveToFirst()) {
            do {
                histories.add(History(cursor.getString(cursor.getColumnIndexOrThrow("Date")), cursor.getInt(cursor.getColumnIndex("HR")), cursor.getInt(cursor.getColumnIndex("SPO2")), cursor.getString(cursor.getColumnIndex("Status"))))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        // Cache the result before returning
        historyCache.clear()  // Clear previous cache to avoid stale data
        historyCache.addAll(histories)

        return histories
    }

    // Filter histories based on date range with caching
    @SuppressLint("Range")
    fun filterHistories(date1: String, date2: String): List<History> {
        val cacheKey = "$date1-$date2"
        val cachedData = historyCache.filter { it.date >= date1 && it.date <= date2 }

        // If data is cached, return it directly
        if (cachedData.isNotEmpty()) {
            return cachedData
        }

        // Otherwise, load from the database
        val histories = mutableListOf<History>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM History WHERE Date BETWEEN ? AND ? ORDER BY Date DESC",
            arrayOf(date1, date2)
        )
        if (cursor.moveToFirst()) {
            do {
                histories.add(History(cursor.getString(cursor.getColumnIndexOrThrow("Date")), cursor.getInt(cursor.getColumnIndex("HR")), cursor.getInt(cursor.getColumnIndex("SPO2")), cursor.getString(cursor.getColumnIndex("Status"))))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        // Cache the result
        historyCache.clear()
        historyCache.addAll(histories)

        return histories
    }

    // Filter histories based on a start date (>= date) with caching
    @SuppressLint("Range")
    fun filterFromHistories(date: String): List<History> {
        val cachedData = historyCache.filter { it.date >= date }

        // If data is cached, return it directly
        if (cachedData.isNotEmpty()) {
            return cachedData
        }

        // Otherwise, load from the database
        val histories = mutableListOf<History>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM History WHERE Date >= ? ORDER BY Date DESC",
            arrayOf(date)
        )
        if (cursor.moveToFirst()) {
            do {
                histories.add(History(cursor.getString(cursor.getColumnIndexOrThrow("Date")), cursor.getInt(cursor.getColumnIndex("HR")), cursor.getInt(cursor.getColumnIndex("SPO2")), cursor.getString(cursor.getColumnIndex("Status"))))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        // Cache the result
        historyCache.clear()
        historyCache.addAll(histories)

        return histories
    }

    // Filter histories based on an end date (<= date) with caching
    @SuppressLint("Range")
    fun filterToHistories(date: String): List<History> {
        val cachedData = historyCache.filter { it.date <= date }

        // If data is cached, return it directly
        if (cachedData.isNotEmpty()) {
            return cachedData
        }

        // Otherwise, load from the database
        val histories = mutableListOf<History>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM History WHERE Date <= ? ORDER BY Date DESC",
            arrayOf(date)
        )
        if (cursor.moveToFirst()) {
            do {
                histories.add(History(cursor.getString(cursor.getColumnIndexOrThrow("Date")), cursor.getInt(cursor.getColumnIndex("HR")), cursor.getInt(cursor.getColumnIndex("SPO2")), cursor.getString(cursor.getColumnIndex("Status"))))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        // Cache the result
        historyCache.clear()
        historyCache.addAll(histories)

        return histories
    }

    // Reset cache if needed (e.g., after updating the database)
    fun resetCache() {
        historyCache.clear()
    }
}
