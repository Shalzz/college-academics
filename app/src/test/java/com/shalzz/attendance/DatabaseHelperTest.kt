package com.shalzz.attendance

import androidx.room.Room
import com.shalzz.attendance.data.local.AppDatabase
import com.shalzz.attendance.data.local.DatabaseHelper
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.util.DefaultConfig
import com.shalzz.attendance.util.RxSchedulersOverrideRule
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.*

/**
 * Unit tests integration with a SQLite Database using Robolectric
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [DefaultConfig.EMULATE_SDK])
class DatabaseHelperTest {

    @Rule @JvmField
    val mOverrideSchedulersRule = RxSchedulersOverrideRule()

    private lateinit var mDatabaseHelper: DatabaseHelper
    private lateinit var mDb : AppDatabase

    @Before
    fun setup() {
        mDb = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.application,
                AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        mDatabaseHelper = DatabaseHelper(mDb)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun writeUserAndReadUser() {
        val user = TestDataFactory.makeUser("u1")

        val writeResult = TestObserver<User>()
        mDatabaseHelper.setUser(user).subscribe(writeResult)
        writeResult.assertNoErrors()
        writeResult.assertValue(user)

        val readResult = TestObserver<User>()
        mDatabaseHelper.user.subscribe(readResult)
        readResult.assertNoErrors()
        readResult.onNext(user) // Since this is reactive streams, onComplete will never be called.
    }

    @Test
    fun writeAndReadSubjects() {
        val subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"))

        mDatabaseHelper.setSubjects(subjects).subscribe()

        val result = TestObserver<List<Subject>>()
        mDatabaseHelper.getSubjects(null).subscribe(result)
        result.assertNoErrors()
        result.onNext(subjects)
    }

    @Test
    fun writeAndReadPeriods() {
        val day = Date()
        val periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
                TestDataFactory.makePeriod("p2", day))

        mDatabaseHelper.setPeriods(periods).subscribe()

        val result = TestObserver<List<Period>>()
        mDatabaseHelper.getPeriods(day).subscribe(result)
        result.assertNoErrors()
        result.onNext(periods)
    }

    @Test
    fun getUserCount() {
        val result = TestObserver<Int>()
        mDatabaseHelper.userCount.subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }

    @Test
    fun getSubjectCount() {
        val result = TestObserver<Int>()
        mDatabaseHelper.subjectCount.subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }

    @Test
    fun getPeriodCount() {
        val result = TestObserver<Int>()
        mDatabaseHelper.getPeriodCount(Date()).subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }
}