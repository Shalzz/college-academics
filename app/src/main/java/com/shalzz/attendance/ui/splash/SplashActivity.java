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

package com.shalzz.attendance.ui.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.ui.base.BaseActivity;
import com.shalzz.attendance.ui.login.AuthenticatorActivity;
import com.shalzz.attendance.ui.main.MainActivity;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.preference.PreferenceManager;
import timber.log.Timber;

public class SplashActivity extends BaseActivity {

    @Inject
    PreferencesHelper mPreferencesHelper;

    @Inject
    @Named("app")
    FirebaseAnalytics mTracker;

    @Inject
    SplashPresenter mPresenter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        Bugsnag.setContext("SplashActivity");
        mPreferencesHelper.upgradePrefsIfNecessary(this);

        mPresenter.getToken(getString(R.string.onedu_gcmSenderId));
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean optIn = sharedPref.getBoolean(getString(R.string.pref_key_ga_opt_in), true);
        mTracker.setAnalyticsCollectionEnabled(optIn);
        Timber.i("Opted In to Google Analytics: %s", optIn);

        // Set all default values once for this application
        try {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        } catch (ClassCastException e) {
            Bugsnag.notify(e, Severity.INFO);
            PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        }

        boolean loggedin = mPreferencesHelper.getLoginStatus();

        Intent intent;
        if (!loggedin)
            intent = new Intent(SplashActivity.this, AuthenticatorActivity.class);
        else
            intent = new Intent(SplashActivity.this, MainActivity.class);

        startActivity(intent);
        finish();
    }
}
