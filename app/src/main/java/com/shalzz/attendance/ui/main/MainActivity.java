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

package com.shalzz.attendance.ui.main;

import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient.BillingResponse;
import com.bugsnag.android.Bugsnag;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.R;
import com.shalzz.attendance.billing.BillingManager;
import com.shalzz.attendance.billing.BillingProvider;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.ui.attendance.AttendanceListFragment;
import com.shalzz.attendance.ui.base.BaseActivity;
import com.shalzz.attendance.ui.login.LoginActivity;
import com.shalzz.attendance.ui.settings.SettingsFragment;
import com.shalzz.attendance.ui.timetable.TimeTablePagerFragment;
import com.shalzz.attendance.wrapper.MySyncManager;

import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindArray;
import butterknife.BindBool;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class MainActivity extends BaseActivity implements MainMvpView, BillingProvider {

    /**
     * To prevent saving the drawer position when logging out.
     */
    public static boolean LOGGED_OUT = false;

    /**
     * Remember the position of the selected item.
     */
    public static final String PREFERENCE_ACTIVATED_FRAGMENT = "ACTIVATED_FRAGMENT2.2";

    public static final String FRAGMENT_TAG = "MainActivity.FRAGMENT";

    public static final String LAUNCH_FRAGMENT_EXTRA = BuildConfig.APPLICATION_ID +
            ".MainActivity.LAUNCH_FRAGMENT";

    private static final String PREVIOUS_FRAGMENT_TAG = "MainActivity.PREVIOUS_FRAGMENT";

    /**
     * Reference to fragment positions
     */
    public enum Fragments {
        ATTENDANCE(1),
        TIMETABLE(2),
        SETTINGS(3);

        private final int value;

        Fragments(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }


    /**
     * Null on tablets
     */
    @Nullable @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.list_slidermenu)
    NavigationView mNavigationView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    /**
     * Drawer lock state. True for tablets, false otherwise .
     */
    @BindBool(R.bool.tablet_layout)
    boolean isTabletLayout;

    @BindArray(R.array.drawer_array)
    String[] mNavTitles;

    @Inject
    DataManager mDataManager;

    @Inject
    MainPresenter mMainPresenter;

    @Inject
    OkHttpClient httpClient;

    public boolean mPopSettingsBackStack =  false;

    private int mCurrentSelectedPosition = Fragments.ATTENDANCE.getValue();
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerHeaderViewHolder DrawerheaderVH;

    private FragmentManager mFragmentManager;
    private Fragment fragment = null;
    // Our custom poor-man's back stack which has only one entry at maximum.
    private Fragment mPreviousFragment;
    private BillingManager mBillingManager;

    public static class DrawerHeaderViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.drawer_header_name) TextView tv_name;
        @BindView(R.id.drawer_header_course) TextView tv_course;
        @BindView(R.id.last_refreshed) TextView last_refresh;

        public DrawerHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);
        ButterKnife.bind(this);
        activityComponent().inject(this);
	    Bugsnag.setContext("MainActivity");
        mMainPresenter.attachView(this);

        mFragmentManager = getSupportFragmentManager();
        DrawerheaderVH = new DrawerHeaderViewHolder(mNavigationView.getHeaderView(0));
        mBillingManager = new BillingManager(this, mDataManager,
                mMainPresenter.getUpdateListener());
        setSupportActionBar(mToolbar);

        // Set the list's click listener
        mNavigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener());

        initDrawer();
        init(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showcaseView();
        // Note: We query purchases in onResume() to handle purchases completed while the activity
        // is inactive. For example, this can happen if the activity is destroyed during the
        // purchase flow. This ensures that when the activity is resumed it reflects the user's
        // current purchases.
        if (mBillingManager != null
                && mBillingManager.getBillingClientResponseCode() == BillingResponse.OK) {
            mBillingManager.queryPurchases();
        }
    }

    /**
     * Initialise a fragment
     **/
    public void init(Bundle bundle) {

        // Select either the default item (Fragments.ATTENDANCE) or the last selected item.
        mCurrentSelectedPosition = reloadCurrentFragment();

        // Recycle fragment
        if(bundle != null) {
            fragment =  mFragmentManager.findFragmentByTag(FRAGMENT_TAG);
            mPreviousFragment = mFragmentManager.getFragment(bundle, PREVIOUS_FRAGMENT_TAG);
            Timber.d("current fag found: %s", fragment);
            Timber.d("previous fag found: %s", mPreviousFragment);
            selectItem(mCurrentSelectedPosition);
            showFragment(fragment);
        } else {

            if (getIntent().hasExtra(LAUNCH_FRAGMENT_EXTRA)) {
                mCurrentSelectedPosition = getIntent().getIntExtra(LAUNCH_FRAGMENT_EXTRA,
                        Fragments.ATTENDANCE.getValue());
            } else if (getIntent().getAction() != null &&
                    getIntent().getAction().equals(Intent.ACTION_MANAGE_NETWORK_USAGE)) {
                mCurrentSelectedPosition = Fragments.SETTINGS.getValue();
                Timber.i("MANAGE_NETWORK_USAGE intent received");
            }
            displayView(mCurrentSelectedPosition);
        }

        mMainPresenter.loadUser();
    }

    private void initDrawer() {
        if(mDrawerLayout == null)
            return;

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> {
            int drawerLockMode = mDrawerLayout.getDrawerLockMode(GravityCompat.START);
            // check if drawer is shown as up
            if(drawerLockMode == DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                onBackPressed();
            } else if (mDrawerLayout.isDrawerVisible(GravityCompat.START)
                    && (drawerLockMode != DrawerLayout.LOCK_MODE_LOCKED_OPEN)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark,
                    getTheme()));
        } else {
            //noinspection deprecation
            mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(
                    R.color.primary_dark));
        }
    }

    void showcaseView() {

        if(isTabletLayout) {
            if(fragment instanceof AttendanceListFragment) {
                ((AttendanceListFragment) fragment).showcaseView();
            }
            return;
        }

        Target homeTarget = () -> {
            // Get approximate position of home icon's center
            int actionBarSize = mToolbar.getHeight();
            int x = actionBarSize / 2;
            int y = actionBarSize / 2;
            return new Point(x, y);
        };

        final ShowcaseView sv = new ShowcaseView.Builder(this)
                .setTarget(homeTarget)
                .setStyle(R.style.ShowcaseTheme)
                .singleShot(1111)
                .setContentTitle(getString(R.string.sv_main_activity_title))
                .setContentText(getString(R.string.sv_main_activity_content))
                .build();

        sv.overrideButtonClick(v -> {
            if(mDrawerLayout != null)
                mDrawerLayout.closeDrawer(mNavigationView);
            sv.hide();
            if(fragment instanceof AttendanceListFragment) {
                ((AttendanceListFragment) fragment).showcaseView();
            }
        });
    }

    public void setDrawerAsUp(boolean enabled) {
        if(mDrawerLayout == null)
            return ;

        float start = enabled ? 0f : 1f ;
        float end = enabled ? 1f : 0f ;
        mDrawerLayout.setDrawerLockMode(enabled ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                DrawerLayout.LOCK_MODE_UNLOCKED);

        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.addUpdateListener(valueAnimator -> {
            float slideOffset = (Float) valueAnimator.getAnimatedValue();
            mDrawerToggle.onDrawerSlide(mDrawerLayout, slideOffset);
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(300);
        anim.start();
    }

    private class NavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            displayView(menuItem.getOrder());
            return false;
        }
    }

    void displayView(int position) {
        ActionBar actionBar = getSupportActionBar();
        // update the main content by replacing fragments
        switch (position) {
            case 0:
                return;
            case 1:
                fragment = new AttendanceListFragment();
                mPreviousFragment = null; // GC
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(false);
                }
                break;
            case 2:
                fragment = new TimeTablePagerFragment();
                mPreviousFragment = null; // GC
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(false);
                }
                break;
            case 3:
                fragment = new SettingsFragment();
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                break;
            default:
                break;
        }

        if (fragment != null) {
            selectItem(position);
            showFragment(fragment);
        } else {
            Timber.e("Error in creating fragment");
        }
    }

    /**
     * Update selected item and title, then close the drawer
     * @param position the item to highlight
     */
    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        mNavigationView.getMenu().getItem(position-1).setChecked(true);
        setTitle(mNavTitles[position-1]);
        if(mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mNavigationView))
            mDrawerLayout.closeDrawer(mNavigationView);
    }

    /**
     * Push the installed fragment into our custom back stack (or optionally
     * {@link FragmentTransaction#remove} it) and {@link FragmentTransaction#add} {@code fragment}.
     *
     * @param fragment {@link Fragment} to be added.
     *
     */
    private void showFragment(Fragment fragment) {
        final FragmentTransaction ft = mFragmentManager.beginTransaction();
        final Fragment installed = getInstalledFragment();

        // return if the fragment is already installed
        if(isAttendanceListInstalled() && fragment instanceof AttendanceListFragment ||
                isTimeTablePagerInstalled() && fragment instanceof TimeTablePagerFragment ||
                isSettingsInstalled() && fragment instanceof SettingsFragment) {
            return;
        }

        if (mPreviousFragment != null) {
            Timber.d("showFragment: destroying previous fragment %s",
                    mPreviousFragment.getClass().getSimpleName());
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.remove(mPreviousFragment);
            mPreviousFragment = null;
        }

        // Remove the current fragment and push it into the backstack.
        if (installed != null) {
            mPreviousFragment = installed;
            ft.detach(mPreviousFragment);
        }

        // Show the new one
        ft.add(R.id.frame_container,fragment,FRAGMENT_TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // called by the activity on tablets,
        // as we do not set a onClick listener
        // on the toolbar navigation icon
        // while on a tablet
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            mMainPresenter.logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // close drawer if it is open
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        }
        else if (shouldPopFromBackStack()) {
            if(mPopSettingsBackStack) {
                Timber.i("Back: Popping from internal back stack");
                mPopSettingsBackStack = false;
                mFragmentManager.popBackStackImmediate();
                setDrawerAsUp(false);
            } else {
                Timber.i("Back: Popping from custom back stack");
                // Custom back stack
                popFromBackStack();
                ActionBar actionBar = getSupportActionBar();
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(false);
                }
            }
        }
        else {
            ActivityCompat.finishAfterTransition(this);
            Timber.i("Back: App closed");
        }
    }

    /**
     * @return true if we should pop from our custom back stack.
     */
    private boolean shouldPopFromBackStack() {

        if (mPreviousFragment == null) {
            return false; // Nothing in the back stack
        }
        final Fragment installed = getInstalledFragment();
        if (installed == null) {
            // If no fragment is installed right now, do nothing.
            return false;
        }
        // Okay now we have 2 fragments; the one in the back stack and the one that's currently
        // installed.
        return !(installed instanceof AttendanceListFragment ||
                installed instanceof TimeTablePagerFragment);

    }

    /**
     * Pop from our custom back stack.
     */
    private void popFromBackStack() {
        if (mPreviousFragment == null) {
            return;
        }
        final FragmentTransaction ft = mFragmentManager.beginTransaction();
        final Fragment installed = getInstalledFragment();
        int position = Fragments.ATTENDANCE.getValue() ;
        Timber.i("backstack: [pop] %s -> %s", installed.getClass().getSimpleName(),
                mPreviousFragment.getClass().getSimpleName());

        ft.remove(installed);
        ft.attach(mPreviousFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        // redraw fragment
        if (mPreviousFragment instanceof AttendanceListFragment) {
            position = Fragments.ATTENDANCE.getValue();
        } else if (mPreviousFragment instanceof TimeTablePagerFragment) {
            position = Fragments.TIMETABLE.getValue();
            //((TimeTablePagerFragment) mPreviousFragment).updateFragmentsData();
        }
        selectItem(position);
        mPreviousFragment = null;
    }

    @SuppressWarnings("CommitPrefEdits")
    private void persistCurrentFragment() {
        if(!LOGGED_OUT) {
            SharedPreferences.Editor editor = getSharedPreferences("SETTINGS", 0).edit();
            mCurrentSelectedPosition = mCurrentSelectedPosition == Fragments.SETTINGS.getValue() ?
                    Fragments.ATTENDANCE.getValue() : mCurrentSelectedPosition;
            editor.putInt(PREFERENCE_ACTIVATED_FRAGMENT, mCurrentSelectedPosition).commit();
        }
    }

    private int reloadCurrentFragment() {
        SharedPreferences settings = getSharedPreferences("SETTINGS", 0);
        return settings.getInt(PREFERENCE_ACTIVATED_FRAGMENT, Fragments.ATTENDANCE.getValue());
    }

    @Override
    public void setTitle(CharSequence title) {
        mToolbar.setTitle(title);
        mToolbar.setSubtitle("");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(mDrawerToggle != null)
            mDrawerToggle.syncState();

        // Toolbar#setTitle is called by the system on onCreate and
        // again over here which sets the activity label
        // as the title.
        // So we need to call setTitle again as well
        // to show the correct title.
        setTitle(mNavTitles[mCurrentSelectedPosition-1]);
    }

    /**
     * @return currently installed {@link Fragment} (1-pane has only one at most), or null if none
     *         exists.
     */
    private Fragment getInstalledFragment() {
        return mFragmentManager.findFragmentByTag(FRAGMENT_TAG);
    }

    private boolean isAttendanceListInstalled() {
        return getInstalledFragment() instanceof AttendanceListFragment;
    }

    private boolean isTimeTablePagerInstalled() {
        return getInstalledFragment() instanceof TimeTablePagerFragment;
    }

    private boolean isSettingsInstalled() {
        return getInstalledFragment() instanceof SettingsFragment;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // for orientation changes, etc.
        if (mPreviousFragment != null) {
            mFragmentManager.putFragment(outState, PREVIOUS_FRAGMENT_TAG, mPreviousFragment);
            Timber.d("previous fag saved: %s", mPreviousFragment.getClass().getSimpleName());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause() {
        persistCurrentFragment();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if(mDrawerLayout != null)
            mDrawerLayout.removeDrawerListener(mDrawerToggle);
        if (mBillingManager != null) {
            mBillingManager.destroy();
        }
        mMainPresenter.detachView();
        super.onDestroy();
    }

    /****** BillingProvider interface implementations*****/

    @Override
    public BillingManager getBillingManager() {
        return mBillingManager;
    }

    @Override
    public boolean isProKeyPurchased() {
        return mMainPresenter.isProKeyPurchased();
    }

    /******* MVP View methods implementation *****/

    @Override
    public void updateUserDetails(User user) {
        if (user.name() != null && !user.name().isEmpty())
            DrawerheaderVH.tv_name.setText(user.name());
        if (user.course() != null && !user.course().isEmpty())
            DrawerheaderVH.tv_course.setText(user.course());
    }

    @Override
    public void logout() {
        // Remove Sync Account
        MySyncManager.removeSyncAccount(this);

        // Invalidate the complete network cache
        try {
            httpClient.cache().evictAll();
        } catch (IOException e) {
            Timber.e(e);
        }

        // Cancel a notification if it is shown.
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0 /* timetable changed notification id */);

        // Destroy current activity and start doLogin Activity
        Intent ourIntent = new Intent(this, LoginActivity.class);
        startActivity(ourIntent);
        finish();
    }
}
