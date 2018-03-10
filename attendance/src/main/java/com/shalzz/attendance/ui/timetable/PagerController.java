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

package com.shalzz.attendance.ui.timetable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.malinskiy.materialicons.IconDrawable;
import com.malinskiy.materialicons.Iconify;
import com.shalzz.attendance.data.local.DbOpenHelper;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.Miscellaneous;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import timber.log.Timber;

public class PagerController {

    private TimeTablePagerFragment mView;
    public TimeTablePagerAdapter mAdapter;
    private DbOpenHelper db;
    private Resources mResources;
    private Date mToday = new Date();
    private final DataAPI api;

    @Inject
    public PagerController(@Singleton Context context,
                           TimeTablePagerFragment view,
                           FragmentManager fm,
                           DataAPI api) {
        mResources = context.getResources();
        mView = view;
        db = new DbOpenHelper(context);
        mAdapter = new TimeTablePagerAdapter(fm, context);
        mView.mViewPager.setAdapter(mAdapter);
        this.api = api;
    }

    public void setDate(Date date) {
        mAdapter.setDate(date);
        mView.updateTitle(-1);
        scrollToDate(date);
    }

    public void setToday() {
        setDate(mToday);
    }

    public void scrollToDate(Date date) {
        int pos = mAdapter.getPositionForDate(date);
        mView.mViewPager.setCurrentItem(pos, true);
    }

    public Date getDateForPosition(int position) {
        return mAdapter.getDateForPosition(position);
    }

    public void updatePeriods() {
        Call<List<Period>> call = api.getTimetable();
        call.enqueue(new Callback<List<Period>>() {
            @Override
            public void onResponse(Call<List<Period>> call,
                                   retrofit2.Response<List<Period>> response) {
                done();
                toggleEmptyViewVisibility(false);

                List<Period> periods = response.body();

                long now = new Date().getTime();
                for (Period period : periods) {
                    db.addPeriod(period, now);
                }

                if (db.purgeOldPeriods() == 1) {
                    Timber.i("Purging Periods...");
                }
                db.close();

                // Don't update the view, if there isn't one.
                if(mView == null || mView.mViewPager == null)
                    return;

                // TODO: use an event bus or RxJava to update fragment contents
                setToday();
                mView.updateTitle(-1);

                // Update the drawer header
                ((MainActivity) mView.getActivity()).updateLastSync();
            }

            @Override
            public void onFailure(Call<List<Period>> call, Throwable t) {
                if(mView == null || mView.getActivity() == null)
                    return;

                RetrofitException error = (RetrofitException) t;
                if(db.getSubjectCount() > 0 &&
                        (error.getKind() == RetrofitException.Kind.NETWORK ||
                                error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE)) {
                    View view = mView.getActivity().findViewById(android.R.id.content);
                    Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction("Retry", v -> updatePeriods())
                            .show();
                }
                else if (error.getKind() == RetrofitException.Kind.NETWORK) {
                        Drawable emptyDrawable = new IconDrawable(mView.getContext(),
                                Iconify.IconValue.zmdi_wifi_off)
                                .colorRes(android.R.color.darker_gray);
                        mView.mEmptyView.ImageView.setImageDrawable(emptyDrawable);
                        mView.mEmptyView.TitleTextView.setText(R.string.no_connection_title);
                        mView.mEmptyView.ContentTextView.setText(R.string.no_connection_content);
                        mView.mEmptyView.Button.setOnClickListener( v -> updatePeriods());
                        mView.mEmptyView.Button.setVisibility(View.VISIBLE);

                        toggleEmptyViewVisibility(true);
                }
                else if (error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE) {
                    Drawable emptyDrawable = new IconDrawable(mView.getContext(),
                            Iconify.IconValue.zmdi_cloud_off)
                            .colorRes(android.R.color.darker_gray);
                    mView.mEmptyView.ImageView.setImageDrawable(emptyDrawable);
                    mView.mEmptyView.TitleTextView.setText(R.string.no_data_title);
                    mView.mEmptyView.ContentTextView.setText(R.string.no_data_content);
                    mView.mEmptyView.Button.setVisibility(View.GONE);

                    toggleEmptyViewVisibility(true);

                    // Update the drawer header
                    ((MainActivity) mView.getActivity()).updateLastSync();
                }
                else if (error.getKind() == RetrofitException.Kind.HTTP) {
                    showError(error.getMessage());
                }
                else {
                    String msg = mResources.getString(R.string.unexpected_error);
                    showError(msg);
                    Timber.e(t, msg);
                }
                done();
            }
        });
    }

    private void toggleEmptyViewVisibility(boolean show) {
        if(mView == null || mView.mViewPager == null || mView.mEmptyView == null)
            return;
        if(show) {
            mView.emptyView.setVisibility(View.VISIBLE);
            mView.mViewPager.setVisibility(View.GONE);
        } else {
            mView.emptyView.setVisibility(View.GONE);
            mView.mViewPager.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        if(mView == null || mView.getActivity() == null)
            return;
        View view = mView.getActivity().findViewById(android.R.id.content);
        if(view != null)
            Miscellaneous.showSnackBar(view, message);
    }

    public void done() {
        if(mView.mProgress != null) {
            mView.mProgress.setVisibility(View.GONE);
        }
        if(mView.mSwipeRefreshLayout != null) {
            mView.mSwipeRefreshLayout.setRefreshing(false);
        }
        if(mView.mViewPager != null) {
            mView.mViewPager.setVisibility(View.VISIBLE);
        }
    }
}
