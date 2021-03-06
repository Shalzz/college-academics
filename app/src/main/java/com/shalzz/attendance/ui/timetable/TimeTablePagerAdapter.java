/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.ui.timetable;

import android.content.SharedPreferences;
import android.util.SparseArray;

import com.shalzz.attendance.R;
import com.shalzz.attendance.billing.BillingProvider;
import com.shalzz.attendance.event.ProKeyPurchaseEvent;
import com.shalzz.attendance.ui.day.DayFragment;
import com.shalzz.attendance.utils.RxEventBus;
import com.shalzz.attendance.utils.RxUtil;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Calendar;
import java.util.Date;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import kotlinx.coroutines.GlobalScope;
import timber.log.Timber;

public class TimeTablePagerAdapter extends FragmentStatePagerAdapter {

    private final int COUNT = 31;

	private final SparseArray<Date> dates = new SparseArray<>();
    private Date mToday;
    private Date mDate;
    private boolean mHideWeekends = false;
    private Callback mCallback;
    private RxEventBus mEventBus;
    private Disposable disposable;

	TimeTablePagerAdapter(FragmentManager fm, AppCompatActivity activity, Callback callback,
                          RxEventBus eventBus) {
		super(fm);
        mCallback = callback;
        mEventBus = eventBus;

        checkPreferences(activity);

        disposable = mEventBus.filteredObservable(ProKeyPurchaseEvent.class)
                .subscribe(proKeyPurchaseEvent -> {
                    checkPreferences(activity);
                    scrollToToday();
                });

        mToday = new Date();
        setDate(mToday);
	}

	private void checkPreferences(AppCompatActivity activity) {
        if (((BillingProvider)activity).isProKeyPurchased()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
            mHideWeekends = sharedPref.getBoolean(activity.getString(R.string.pref_key_hide_weekends), false);
        } else {
            mHideWeekends = false;
        }
    }

    public void destroy() {
        RxUtil.dispose(disposable);
    }
	@Override
	public DayFragment getItem(int position) {
        return DayFragment.Companion.newInstance(dates.get(position));
	}

    @Override
    public int getCount() {
        return COUNT;
    }

    @Override
    public int getItemPosition(Object item) {
	    // TODO: fix performance of fragment and array creation and destruction
//        DayFragment fragment = (DayFragment)item;
//        Date date = fragment.getDate();
//        int position = positions.get(date);
//
//        if (position >= 0) {
//            return position;
//        } else {
//            return POSITION_NONE;
//        }
        return POSITION_NONE;
    }

    public Date getDateForPosition(int position) {
        return dates.get(position);
    }

    public void scrollToDate(Date date) {
        if(mHideWeekends) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            while(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) {
                calendar.add(Calendar.DATE, 1);
            }
            date = calendar.getTime();
        }
        Timber.d("Date: %s", date);
        mCallback.scrollToPosition(indexOfValue(dates, date));
    }

    public void scrollToToday() {
        scrollToDate(mToday);
    }

    public void setDate(@NonNull Date date) {
        if(mDate == null || !DateHelper.toTechnicalFormat(mDate)
                .equals(DateHelper.toTechnicalFormat(date))) {
            mDate = date;
            updateDates();
        }
    }

    private void updateDates() {
        Timber.d("Updating dates");
        int day_offset = 0;
        Calendar calendar = Calendar.getInstance();
        for(int i =0; i < getCount() ; i++) {
            calendar.setTime(mDate);
            calendar.add(Calendar.DATE, -15+i);
            if(mHideWeekends) {
                calendar.add(Calendar.DATE, day_offset);
                while(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) {
                    calendar.add(Calendar.DATE, 1);
                    ++day_offset;
                }
            }
            Date date = calendar.getTime();
            dates.put(i, date);
        }
        notifyDataSetChanged();
    }

    private int indexOfValue(SparseArray<Date> array, Date value) {
        for (int i = 0; i < array.size(); i++) {
            if (DateHelper.toTechnicalFormat(array.valueAt(i))
                    .equals(DateHelper.toTechnicalFormat(value)))
                return i;
        }
        return -1;
    }

    interface Callback {
	    void scrollToPosition(int position);
    }
}
