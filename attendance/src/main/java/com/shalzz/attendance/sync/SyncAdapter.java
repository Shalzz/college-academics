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

package com.shalzz.attendance.sync;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.data.model.remote.ImmutablePeriod;
import com.shalzz.attendance.data.model.remote.ImmutableSubject;
import com.shalzz.attendance.data.network.DataAPI;
import com.shalzz.attendance.wrapper.MyPreferencesManager;

import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	// Global variables
	private String myTag = "Sync Adapter";
	private Context mContext;

    private final MyPreferencesManager preferencesManager;
    private final DataAPI api;

	/**
	 * Set up the sync adapter. This form of the
	 * constructor maintains compatibility with Android 3.0
	 * and later platform versions
	 */
	public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs, MyPreferencesManager preferencesManager, DataAPI api) {
		super(context, autoInitialize, allowParallelSyncs);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
		mContext = context;
        this.preferencesManager = preferencesManager;
        this.api = api;
    }

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

        Call<List<ImmutableSubject>> call = api.getAttendance(preferencesManager.getBasicAuthCredentials());
        call.enqueue(new Callback<List<ImmutableSubject>>() {
            @Override
            public void onResponse(Call<List<ImmutableSubject>> call, Response<List<ImmutableSubject>> response) {
                if(response.isSuccessful()) {
                    try {
                        DatabaseHandler db = new DatabaseHandler(mContext);
                        long now = new Date().getTime();
                        for (ImmutableSubject subject : response.body()) {
                            db.addSubject(subject, now);
                        }
                        db.purgeOldSubjects();
                        db.close();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ImmutableSubject>> call, Throwable t) {
                t.printStackTrace();
            }
        });

        Call<List<ImmutablePeriod>> call2 = api.getTimetable(preferencesManager.getBasicAuthCredentials());
        call2.enqueue(new Callback<List<ImmutablePeriod>>() {
            @Override
            @SuppressLint("InlinedApi")
            public void onResponse(Call<List<ImmutablePeriod>> call,
                                   Response<List<ImmutablePeriod>> response) {
                if(response.isSuccessful()) {
                    try {
                        DatabaseHandler db = new DatabaseHandler(mContext);
                        long now = new Date().getTime();
                        for(ImmutablePeriod period : response.body()) {
                            db.addPeriod(period, now);
                        }
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences
                                (mContext);
                        boolean notify = sharedPref.getBoolean(mContext.getString(
                                R.string.pref_key_notify_timetable_changed),
                                true);

                        notify = db.purgeOldPeriods() == 1 && notify;

                        // Show a notification since our timetable has changed.
                        if (notify) {
                            NotificationCompat.Builder mBuilder =
                                    (NotificationCompat.Builder) new NotificationCompat.Builder(mContext)
                                            .setSmallIcon(R.drawable.ic_stat_human)
                                            .setLargeIcon(BitmapFactory.decodeResource(
                                                    mContext.getResources(),
                                                    R.mipmap.ic_launcher))
                                            .setAutoCancel(true)
                                            .setPriority(Notification.PRIORITY_LOW)
                                            .setCategory(Notification.CATEGORY_RECOMMENDATION)
                                            .setContentTitle(mContext.getString(
                                                    R.string.notify_timetable_changed_title))
                                            .setContentText(mContext.getString(
                                                    R.string.notify_timetable_changed_text));

                            Intent resultIntent = new Intent(mContext, MainActivity.class);
                            resultIntent.putExtra(MainActivity.LAUNCH_FRAGMENT_EXTRA, MainActivity
                                    .Fragments.TIMETABLE.getValue());
                            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                                    0, resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                            mBuilder.setContentIntent(resultPendingIntent);
                            NotificationManager mNotificationManager =
                                    (NotificationManager) mContext.getSystemService(
                                            Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(0, mBuilder.build());
                        }
                        db.close();
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ImmutablePeriod>> call, Throwable t) {
                t.printStackTrace();
            }
        });
	}
}
