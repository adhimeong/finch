package com.bourke.finch;

import android.app.Activity;

import android.content.Context;

import android.os.Bundle;
import android.os.Parcelable;

import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.Window;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bourke.finch.lazylist.LazyAdapter;

import java.util.ArrayList;

public class FinchActivity extends FragmentActivity
        implements ActionBar.OnNavigationListener {

    private static final int NUM_ITEMS = 2;

    public static final int HOME_PAGE = 0;
    public static final int MESSAGES_PAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout and theme */
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTheme(FinchApplication.THEME_LIGHT);
        setContentView(R.layout.main);

        /* Set up actionbar navigation spinner */
        ArrayAdapter<CharSequence> list =
            ArrayAdapter.createFromResource(this, R.array.locations,
                R.layout.spinner_title);
        list.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        getSupportActionBar().setNavigationMode(
                ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(list, this);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        FinchPagerAdapter adapter = new FinchPagerAdapter(
                getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        viewPager.setOnPageChangeListener(new OnPageChangeListener() {

        @Override
        public void onPageSelected(int page) {
            //TODO: update nav spinner
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
        });
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return true;
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Write")
            .setIcon(R.drawable.ic_action_edit)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS |
                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add("Refresh")
            .setIcon(R.drawable.ic_action_refresh)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS |
                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add("Search")
            .setIcon(R.drawable.ic_action_search)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS |
                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }
    */

    public static class FinchPagerAdapter extends FragmentPagerAdapter {

        public FinchPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case HOME_PAGE:
                    return new HomePageFragment();
                case MESSAGES_PAGE:
                    return new MessagesFragment();
            }
            return null;
        }

    }
}