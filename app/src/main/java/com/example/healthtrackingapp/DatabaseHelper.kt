import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "HealthDatabase.db"  // Name of the database
        private const val DATABASE_VERSION = 2  // Database version
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create the History table with Date as TEXT and formatted as 'yyyy-MM-dd HH:mm:ss'
        val createTable1 = """
        CREATE TABLE History (
            Date TEXT PRIMARY KEY, 
            HR INTEGER, 
            SPO2 INTEGER
        )
    """

        // Create the EContact table with PhoneNumber as TEXT (to handle large numbers and preserve format)
        val createTable2 = """
        CREATE TABLE EContact (
            PhoneNumber TEXT  PRIMARY KEY
        )
    """

        // Execute the SQL statements
        db.execSQL(createTable1)
        db.execSQL(createTable2)
    }

    // Called when the database version is updated
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop the old tables if they exist
        db.execSQL("DROP TABLE IF EXISTS History")
        db.execSQL("DROP TABLE IF EXISTS EContact")
        // Create the tables again
        onCreate(db)
    }
}
