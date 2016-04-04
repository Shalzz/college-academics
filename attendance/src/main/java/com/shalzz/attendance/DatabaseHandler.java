/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;

import com.shalzz.attendance.model.ListFooterModel;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.model.SubjectModel;
import com.shalzz.attendance.model.UserModel;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.MyPreferencesManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.security.auth.Subject;

/**
 * Helper Class for SQLite database
 * @author shalzz
 *
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    /**
     * Database Version
     */
    private static final int DATABASE_VERSION = 8;

    /**
     * Database Name
     */
    private static final String DATABASE_NAME = "attendanceManager";

    /**
     *  Attendance table name
     */
    private static final String TABLE_ATTENDANCE = "Attendance";

    /**
     *  Table for storing the days
     *  marked as absent for a subject
     */
    private static final String TABLE_DAYS_ABSENT = "days_absent";

    /**
     *  Timetable table name
     */
    private static final String TABLE_TIMETABLE = "TimeTable";

    /**
     * ListHeader table name
     */
    private static final String TABLE_USER = "User";

    /**
     * Attendance Table Columns names
     */
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "Subject_Name";
    private static final String KEY_CLASSES_HELD = "No_Classes_Held";
    private static final String KEY_CLASSES_ATTENDED = "No_Classes_Attended";
    private static final String KEY_DAY_ABSENT = "Days_Absent";

    /**
     *  TimeTable Table Column names
     */
    //private static final String KEY_TT_ID = "id";
    private static final String KEY_DAY = "Day";
    private static final String KEY_SUBJECT_NAME = "Subject_Name";
    private static final String KEY_TEACHER = "Teacher";
    private static final String KEY_ROOM = "Room";
    private static final String KEY_START = "Start";
    private static final String KEY_END = "End";
    private static final String KEY_BATCH = "batch";

    /**
     * ListHeader Table Columns names
     */
    private static final String KEY_STU_NAME = "student_name";
    private static final String KEY_COURSE = "course_name";
    private static final String KEY_SAPID = "sapid";
    private static final String KEY_PASSWORD = "password";

    /**
     * ListFooter Table Column names
     */
    private static final String KEY_TOTAL_HELD = "Classes_held";
    private static final String KEY_TOTAL_ATTEND = "Classes_attend";

    /**
     * Attribute used to timestamp record inserts and updates.
     */
    private static final String KEY_LAST_UPDATED = "lastUpdated";

    /**
     * Attendance CREATE TABLE SQL query.
     */
    private static final String CREATE_ATTENDANCE_TABLE = "CREATE TABLE " + TABLE_ATTENDANCE + " ( "
            + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_NAME + " TEXT, "
            + KEY_CLASSES_HELD + " REAL, " + KEY_CLASSES_ATTENDED + " REAL, "
            + KEY_LAST_UPDATED + " INTEGER, " + KEY_DAY_ABSENT + " TEXT "  + ");";

    /**
     * Days Absent CREATE TABLE SQL query.
     */
    private static final String CREATE_DAYS_ABSENT_TABLE = "CREATE TABLE " + TABLE_DAYS_ABSENT +
            " ( " + KEY_ID + " INTEGER, " + KEY_DAY_ABSENT + " TEXT, " +
            " UNIQUE ( " + KEY_ID + "," + KEY_DAY_ABSENT + " )," +
            " FOREIGN KEY(" + KEY_ID + " ) REFERENCES " + TABLE_ATTENDANCE + " ( " + KEY_ID +
            " ) ON DELETE CASCADE ); ";

    /**
     * Timetable CREATE TABLE SQL query.
     */
    private static final String CREATE_TIME_TABLE = "CREATE TABLE " + TABLE_TIMETABLE + " ( "
            + KEY_ID + " INTEGER, " + KEY_DAY + " TEXT, " + KEY_SUBJECT_NAME + " TEXT , "
            + KEY_TEACHER + " TEXT , " + KEY_ROOM + " TEXT, " + KEY_BATCH + " TEXT, "
            + KEY_LAST_UPDATED + " INTEGER, "+ KEY_START + " TEXT, "
            + KEY_END + " TEXT, " + "PRIMARY KEY(" + KEY_ID + "," + KEY_DAY + ") " + ");";

    /**
     * User CREATE TABLE SQL query.
     */
    private static final String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + " ( "
            + KEY_STU_NAME + " TEXT, " + KEY_COURSE + " TEXT, "
            + KEY_PASSWORD + " TEXT, " + KEY_SAPID + "  INTEGER PRIMARY KEY " + ");";

    /**
     * Constructor.
     */
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Create Table.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ATTENDANCE_TABLE);
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_TIME_TABLE);
        db.execSQL(CREATE_DAYS_ABSENT_TABLE);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        }
        db.enableWriteAheadLogging();
    }

    /**
     * Drop the table if it exist and create a new table.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        // TODO: Clean up from previous versions, remove in later releases.
        db.execSQL("DROP TABLE IF EXISTS " + "ListFooter");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIMETABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DAYS_ABSENT);

        // Create tables again
        onCreate(db);

        // remove conflicting shared preferences b/w versions
        MyPreferencesManager.removeSettings();
        MyPreferencesManager.removeDefaultSharedPreferences();
    }

    /**
     * Add new SubjectModel
     * @param subject the {@link SubjectModel} to add
     */
    public void addSubject(SubjectModel subject, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, subject.getID());
        values.put(KEY_NAME, subject.getName());
        values.put(KEY_CLASSES_HELD, subject.getClassesHeld());
        values.put(KEY_CLASSES_ATTENDED, subject.getClassesAttended());
        values.put(KEY_LAST_UPDATED, timestamp);
        db.insert(TABLE_ATTENDANCE, null, values);

        // Store the dates in another table corresponding to the same id
        ContentValues dates = new ContentValues();
        dates.put(KEY_ID, subject.getID());
        for(Date date : subject.getAbsentDates()) {
            dates.put(KEY_DAY_ABSENT, DateHelper.formatToTechnicalFormat(date));
            db.insertWithOnConflict(TABLE_DAYS_ABSENT, null, dates, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    /**
     * Update a single Subject
     * @param subject the {@link SubjectModel} to update
     * @return no. of rows affected
     */
    public int updateSubject(SubjectModel subject, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, subject.getName());
        values.put(KEY_CLASSES_HELD, subject.getClassesHeld());
        values.put(KEY_CLASSES_ATTENDED, subject.getClassesAttended());
        values.put(KEY_LAST_UPDATED, timestamp);

        ArrayList<Date> newDates = (ArrayList<Date>) subject.getAbsentDates().clone();

        String datesQuery = "SELECT " + KEY_DAY_ABSENT + " FROM " + TABLE_DAYS_ABSENT +
                " WHERE " + KEY_ID + " = " + subject.getID() + ";";

        Cursor dateCursor = db.rawQuery(datesQuery, null);
        if (dateCursor.moveToFirst()) {
            do {

                // fixme: make sure the same date object is created every time
                // fixme: for the same date string
                Date date = DateHelper.parseDate(dateCursor.getString(0));
                // insert only new dates
                if(newDates.contains(date)) {
                    newDates.remove(date);
                }
                // and delete dates that are no longer marked as absent
                else {
                    db.delete(TABLE_DAYS_ABSENT,KEY_ID + "=? and " + KEY_DAY_ABSENT + "=?",
                    new String[] { String.valueOf(subject.getID()),
                            dateCursor.getString(0)});
                }
            } while (dateCursor.moveToNext());
        }
        dateCursor.close();

        // Store the dates in another table corresponding to the same id
        ContentValues insert = new ContentValues();
        insert.put(KEY_ID, subject.getID());
        for(Date date : newDates) {
            insert.put(KEY_DAY_ABSENT, DateHelper.formatToTechnicalFormat(date));
            db.insertWithOnConflict(TABLE_DAYS_ABSENT, null, insert, SQLiteDatabase.CONFLICT_IGNORE);
        }

        return db.update(TABLE_ATTENDANCE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(subject.getID()) });
    }

    /**
     * Adds a new Subject if it doesn't exists otherwise updates it.
     * @param subject the {@link Subject} to add
     */
    public void addOrUpdateSubject(SubjectModel subject, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[] { KEY_ID}, KEY_ID + "=?",
                new String[] { String.valueOf(subject.getID()) }, null, null, null, null);
        if (cursor.getCount() == 0)
        {
            addSubject(subject, timestamp);
        }
        else
        {
            updateSubject(subject, timestamp);
        }
        cursor.close();
    }

    /**
     * Get All Subjects
     * @return subjectList
     */
    public List<SubjectModel> getAllSubjects(AsyncTaskLoader callback, String filter) {
        List<SubjectModel> subjectList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;

        if (filter != null) {
            cursor = db.query(TABLE_ATTENDANCE, new String[]{KEY_ID, KEY_NAME, KEY_CLASSES_HELD,
                            KEY_CLASSES_ATTENDED, KEY_DAY_ABSENT}, KEY_NAME + " LIKE '%" +
                            filter + "%'",
                    null, null, null, KEY_NAME, null);
        } else {
            cursor = db.query(TABLE_ATTENDANCE, new String[]{KEY_ID, KEY_NAME, KEY_CLASSES_HELD,
                            KEY_CLASSES_ATTENDED, KEY_DAY_ABSENT}, null,
                    null, null, null, KEY_NAME, null);
        }

        if (cursor != null && cursor.moveToFirst()) {
            try {
                do {
                    // Check isLoadInBackgroundCanceled() to cancel out early
                    if(callback != null && callback.isLoadInBackgroundCanceled()) {
                        break;
                    }

                    SubjectModel subject = new SubjectModel();
                    subject.setID(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                    subject.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                    subject.setClassesHeld(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_CLASSES_HELD)));
                    subject.setClassesAttended(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_CLASSES_ATTENDED)));

                    String datesQuery = "SELECT " + KEY_DAY_ABSENT + " FROM " + TABLE_DAYS_ABSENT +
                            " WHERE " + KEY_ID + " = " + subject.getID() + ";";

                    Cursor dateCursor = db.rawQuery(datesQuery, null);
                    ArrayList<Date> dates = new ArrayList<>();
                    if (dateCursor.moveToFirst()) {
                        do {
                            Date date = DateHelper.parseDate(dateCursor.getString(0));
                            dates.add(date);
                        } while (dateCursor.moveToNext());
                    }
                    dateCursor.close();
                    subject.setAbsentDates(dates);

                    subjectList.add(subject);
                } while (cursor.moveToNext());
            } finally {
                cursor.close();
            }
        }

        return subjectList;
    }

    /**
     * Get All Subjects matching the wildcard.
     * @return subjectList
     */
    public List<SubjectModel> getAllSubjectsLike(String wildcard) {
        List<SubjectModel> subjectList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{KEY_ID, KEY_NAME, KEY_CLASSES_HELD,
                        KEY_CLASSES_ATTENDED, KEY_DAY_ABSENT}, KEY_NAME + " LIKE '%" + wildcard + "%'",
                null, null, null, KEY_NAME, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                SubjectModel subject = new SubjectModel();
                subject.setID(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                subject.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                subject.setClassesHeld(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_CLASSES_HELD)));
                subject.setClassesAttended(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_CLASSES_ATTENDED)));

                String datesQuery = "SELECT " + KEY_DAY_ABSENT + " FROM " + TABLE_DAYS_ABSENT +
                        " WHERE " + KEY_ID + " = " + subject.getID() + ";";

                Cursor dateCursor = db.rawQuery(datesQuery, null);
                ArrayList<Date> dates = new ArrayList<>();
                if (dateCursor.moveToFirst()) {
                    do {
                        Date date = DateHelper.parseDate(dateCursor.getString(0));
                        dates.add(date);
                    } while (dateCursor.moveToNext());
                }
                dateCursor.close();
                subject.setAbsentDates(dates);

                subjectList.add(subject);
            } while (cursor.moveToNext());
        }

        db.close();
        cursor.close();

        return subjectList;
    }

    public List<Integer> getAbsentSubjects(Date date) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Integer> subjectIDs = new ArrayList<>();
        Cursor cursor = db.query(TABLE_DAYS_ABSENT, new String[]{KEY_ID}, KEY_DAY_ABSENT + " = ?",
                new String[] { String.valueOf(DateHelper.formatToTechnicalFormat(date)) },
                null, null, null, null);

        try {
            if(cursor.moveToFirst()) {
                do {
                    subjectIDs.add(cursor.getInt(0));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return subjectIDs;
    }

    /**
     * Checks for any obsolete data, based on the timestamp,
     * and deletes if any.
     * @return 1 if one or more subjects are purged else 0
     */
    public int purgeOldSubjects() {
        int purged = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{KEY_ID },
                KEY_LAST_UPDATED + " != (SELECT max("+ KEY_LAST_UPDATED +") FROM " +
                        TABLE_ATTENDANCE + ")",
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            purged = 1;
            do {
                db.delete(TABLE_ATTENDANCE, KEY_ID + " = ?",
                        new String[] { String.valueOf(cursor.getInt(0)) });
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return purged;
    }

    public void addUser(UserModel user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_STU_NAME, user.getName());
        values.put(KEY_COURSE, user.getCourse());
        values.put(KEY_SAPID, user.getSapid());
        values.put(KEY_PASSWORD, user.getPassword());

        // Inserting Row
        db.insert(TABLE_USER, null, values);
        db.close(); // Closing database connection
    }

    public int updateUser(UserModel user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_STU_NAME, user.getName());
        values.put(KEY_COURSE, user.getCourse());
        values.put(KEY_PASSWORD, user.getPassword());

        // updating row
        int rows_affected = db.update(TABLE_USER, values, KEY_SAPID + " = ?",
                new String[] { String.valueOf(user.getSapid()) });
        db.close();

        return rows_affected;
    }

    public void addOrUpdateUser(UserModel user) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_USER, new String[]{KEY_SAPID}, KEY_SAPID + "=?",
                new String[]{String.valueOf(user.getSapid())}, null, null, null, null);
        if (cursor.getCount() == 0) {
            addUser(user);
        }
        else {
            updateUser(user);
        }
        cursor.close();
        db.close();
    }

    public long getLastSync() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT max( " + KEY_LAST_UPDATED + " ) from ( SELECT " +
                KEY_LAST_UPDATED + " from " + TABLE_ATTENDANCE + " union all SELECT " +
                KEY_LAST_UPDATED + " from " + TABLE_TIMETABLE + " ) t;";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToFirst()) {
            long now = new Date().getTime();
            long lastSync = cursor.getLong(0);
            return (now-lastSync)/(1000*60*60);
        }
        cursor.close();
        db.close();
        return -1;
    }

    public UserModel getUser() {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_USER + ";";
        Cursor cursor = db.rawQuery(selectQuery, null);

        UserModel user = new UserModel();
        if (cursor.moveToFirst()) {
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_STU_NAME)));
            user.setCourse(cursor.getString(cursor.getColumnIndexOrThrow(KEY_COURSE)));
            user.setSapid(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SAPID)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)));
        }

        db.close();
        cursor.close();
        return user;
    }

    public ListFooterModel getListFooter() {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  sum(" + KEY_CLASSES_ATTENDED+ ") as " + KEY_TOTAL_ATTEND
                + ",sum(" + KEY_CLASSES_HELD+ ") as " + KEY_TOTAL_HELD
                + " FROM " + TABLE_ATTENDANCE + ";";
        Cursor cursor = db.rawQuery(selectQuery, null);

        ListFooterModel footer = new ListFooterModel();
        if (cursor.moveToFirst()) {
            footer.setHeld(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_TOTAL_HELD)));
            footer.setAttended(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_TOTAL_ATTEND)));
        }
        db.close();
        cursor.close();

        return footer;
    }

    public void addPeriod(PeriodModel period, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, period.getId());
        values.put(KEY_DAY, period.getDay());
        values.put(KEY_SUBJECT_NAME, period.getSubjectName());
        values.put(KEY_TEACHER, period.getTeacher());
        values.put(KEY_ROOM, period.getRoom().trim());
        values.put(KEY_START, period.getStartTime());
        values.put(KEY_END, period.getEndTime());
        values.put(KEY_BATCH, period.getBatch());
        values.put(KEY_LAST_UPDATED, timestamp);

        // Inserting Row
        db.insert(TABLE_TIMETABLE, null, values);
        db.close(); // Closing database connection
    }

    public int updatePeriod(PeriodModel period, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SUBJECT_NAME, period.getSubjectName());
        values.put(KEY_TEACHER, period.getTeacher());
        values.put(KEY_ROOM, period.getRoom().trim());
        values.put(KEY_START, period.getStartTime());
        values.put(KEY_END, period.getEndTime());
        values.put(KEY_BATCH, period.getBatch());
        values.put(KEY_LAST_UPDATED, timestamp);

        // updating row
        int rows_affected = db.update(TABLE_TIMETABLE, values, KEY_DAY + " = ? and " + KEY_ID + "" +
                        " = ?",
                new String[] { String.valueOf(period.getDay()), String.valueOf(period.getId())} );
        db.close(); // Closing database connection

        return rows_affected;
    }

    public void addOrUpdatePeriod(PeriodModel period, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_TIMETABLE, new String[] { KEY_SUBJECT_NAME},
                KEY_DAY + " = ? and " + KEY_ID + " = ?",
                new String[] { String.valueOf(period.getDay()), String.valueOf(period.getId()) },
                null, null, KEY_SUBJECT_NAME, null);

        if (cursor.getCount() == 0) {
            addPeriod(period, timestamp);
        }
        else {
            updatePeriod(period, timestamp);
        }
        cursor.close();
        db.close(); // Closing database connection
    }

    public ArrayList<PeriodModel> getAllPeriods(Date date, AsyncTaskLoader callback) {
        String dayName = DateHelper.getShortWeekday(date);
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TIMETABLE, null, KEY_DAY + "=?",
                new String[]{String.valueOf(dayName)}, null, null, KEY_START, null);

        ArrayList<PeriodModel> periods = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                // Check isLoadInBackgroundCanceled() to cancel out early
                if(callback != null && callback.isLoadInBackgroundCanceled()) {
                    break;
                }
                PeriodModel period = new PeriodModel();
                period.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                period.setDay(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DAY)));
                period.setSubjectName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SUBJECT_NAME)));
                period.setTeacher(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TEACHER)));
                period.setRoom(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROOM)));
                period.setBatch(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BATCH)));
                period.setStart(cursor.getString(cursor.getColumnIndexOrThrow(KEY_START)));
                period.setEnd(cursor.getString(cursor.getColumnIndexOrThrow(KEY_END)));
                periods.add(period);
            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();

        return periods;
    }

    /**
     * Checks for any obsolete data, based on the timestamp,
     * and deletes if any.
     * @return 1 if one or more Periods are purged else 0
     */
    public int purgeOldPeriods() {
        int purged = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_TIMETABLE, new String[]{KEY_ID},
                KEY_LAST_UPDATED + " != (SELECT max("+ KEY_LAST_UPDATED +") FROM " +
                        TABLE_TIMETABLE + ")",
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            purged = 1;
            do {
                db.delete(TABLE_TIMETABLE, KEY_ID + " = ?",
                        new String[] { String.valueOf(cursor.getInt(0)) });
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return purged;
    }

    /**
     * Check if the attendance data is in database.
     * */
    public int getRowCount() {
        String countQuery = "SELECT  * FROM " + TABLE_ATTENDANCE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();
        db.close();
        cursor.close();

        return rowCount;
    }

    /**
     * Check if the Student data is in database.
     * */
    public int getUserCount() {
        String countQuery = "SELECT  * FROM " + TABLE_USER;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();
        db.close();
        cursor.close();

        return rowCount;
    }

    public int getTimetableCount() {
        String countQuery = "SELECT  * FROM " + TABLE_TIMETABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();
        db.close();
        cursor.close();

        return rowCount;
    }

    /**
     * Delete all tables and create them again
     * */
    public void resetTables(){
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_ATTENDANCE, "1", null);
        db.delete(TABLE_TIMETABLE, "1", null);
        db.delete(TABLE_USER, "1", null);
        db.delete(TABLE_DAYS_ABSENT, "1", null);
        db.close();
    }

    public void deleteAllSubjects() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_ATTENDANCE, "1", null);
        db.close();
    }

    public void deleteAllPeriods() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_TIMETABLE, "1", null);
        db.close();
    }
}