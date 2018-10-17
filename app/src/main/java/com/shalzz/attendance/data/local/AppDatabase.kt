package com.shalzz.attendance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shalzz.attendance.data.model.dao.PeriodDao
import com.shalzz.attendance.data.model.dao.SubjectDao
import com.shalzz.attendance.data.model.dao.UserDao
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.injection.ApplicationContext
import javax.inject.Singleton

@Singleton
@Database(entities = [User::class, Subject::class, Period::class], version = 12)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectDao(): SubjectDao
    abstract fun periodDao(): PeriodDao

    companion object {
        fun getInstance(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(context,
                    AppDatabase::class.java, "academics.db")
                    .fallbackToDestructiveMigrationFrom(10, 11)
                    .build()
        }

        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val TEMP_TABLE = "user2"
                db.execSQL("ALTER TABLE user RENAME TO $TEMP_TABLE")
                db.execSQL("CREATE TABLE user (\n" +
                        "    id TEXT PRIMARY KEY NOT NULL,\n" +
                        "    phone TEXT NOT NULL,\n" +
                        "    roll_number TEXT,\n" +
                        "    name TEXT,\n" +
                        "    course TEXT,\n" +
                        "    email TEXT\n" +
                        ")")
                db.execSQL("INSERT INTO user (id, phone, roll_number, name, course, email) " +
                        "SELECT id, phone, roll_number, name, course, email FROM " + TEMP_TABLE)
                db.execSQL("DROP TABLE IF EXISTS $TEMP_TABLE")
            }
        }
    }
}