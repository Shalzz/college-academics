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

package com.shalzz.attendance.ui.login

import android.content.Context
import com.shalzz.attendance.R
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.remote.RetrofitException
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.injection.ConfigPersistent
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.ui.splash.SplashPresenter
import com.shalzz.attendance.utils.NetworkUtil
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@ConfigPersistent
class LoginPresenter @Inject
internal constructor(private val mDataManager: DataManager,
                     private val mPreferenceHelper: PreferencesHelper,
                     @param:ApplicationContext private val mContext: Context,
                     private val mSplashPresenter: SplashPresenter = SplashPresenter(mPreferenceHelper)
    ) : BasePresenter<LoginMvpView>() {

    private var mDisposable: Disposable? = null

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: LoginMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mDisposable)
    }

    fun login(username: String) {
        checkViewAttached()
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            Timber.i("Sync canceled, connection not available")
            mvpView.showError(mContext.getString(R.string.no_internet))
            return
        }

        val onError = { error : Throwable ->
            if (error !is RetrofitException) {
                Timber.e(error)
            }
            else if (isViewAttached) {
                if (error.kind == RetrofitException.Kind.HTTP) {
                    mvpView.showError(error.message)
                } else {
                    mvpView.showError(error.message)
                    Timber.e(error)
                }
            }
        }

        mvpView.showProgressDialog()
        RxUtil.dispose(mDisposable)
        Timber.d("Calling sync user on: %s ", Thread.currentThread().id)
        val auth = "Bearer $username"
        if (mPreferenceHelper.token == null) {
            mSplashPresenter.getToken(mContext.getString(R.string.onedu_gcmSenderId))
        }
        mDisposable = mDataManager.sendRegID(token=mPreferenceHelper.token!!, auth=auth)
                .doOnNext { result-> Timber.d("Sent token to server successfully: %b",
                        result) }
                .flatMap { mDataManager.syncUser(auth) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ user -> mvpView.showMainActivity(user) }, onError)
    }
}