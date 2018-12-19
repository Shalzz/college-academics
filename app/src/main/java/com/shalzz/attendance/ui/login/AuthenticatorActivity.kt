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

package com.shalzz.attendance.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.sync.AccountAuthenticatorActivity
import com.shalzz.attendance.sync.MyAccountManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class AuthenticatorActivity: AccountAuthenticatorActivity(), OTPFragment.OnFragmentInteractionListener {

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    private lateinit var mAccountManager: AccountManager
    private var mAuthTokenType: String? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(icicle: Bundle?) {
        if (icicle == null) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(icicle)
        activityComponent().inject(this)
        setContentView(R.layout.activity_login)

        mAccountManager = AccountManager.get(this)
        mAuthTokenType = MyAccountManager.AUTHTOKEN_TYPE_READ_ONLY
    }

    override fun onFragmentInteraction(authToken: String, phone: String) {
        val data = Bundle()
        data.putString(AccountManager.KEY_ACCOUNT_NAME, phone)
        data.putString(AccountManager.KEY_ACCOUNT_TYPE, MyAccountManager.ACCOUNT_TYPE)
        data.putString(AccountManager.KEY_AUTHTOKEN, authToken)
        val res = Intent()
        res.putExtras(data)

        finishLogin(res)
    }

    private fun finishLogin(intent: Intent) {
        Timber.d( "> finishLogin")

        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val account = Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Timber.d("> finishLogin > addAccountExplicitly")
            val authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)
            val authtokenType = mAuthTokenType

            // Creating the account on the device and setting the auth regId we got
            // (Not setting the auth regId will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, null, null)
            mAccountManager.setAuthToken(account, authtokenType, authtoken)

            // Set up sync
            MyAccountManager.addPeriodicSync(this, account)

        }

        setAccountAuthenticatorResult(intent.extras!!)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        var ARG_ACCOUNT_TYPE: String = "ACCOUNT_TYPE"
        var ARG_AUTH_TYPE: String = "AUTH_TYPE"
        var ARG_IS_ADDING_NEW_ACCOUNT: String = "IS_ADDING_ACCOUNT"
    }
}