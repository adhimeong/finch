package com.bourke.finch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.drawable.Drawable;

import android.net.Uri;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import com.bourke.finch.common.Constants;
import com.bourke.finch.common.FinchTwitterFactory;
import com.bourke.finch.common.TwitterTask;
import com.bourke.finch.common.TwitterTaskCallback;
import com.bourke.finch.common.TwitterTaskParams;
import com.bourke.finch.lazylist.LazyAdapter;
import com.bourke.finch.provider.FinchProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import twitter4j.auth.AccessToken;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import twitter4j.Paging;

import twitter4j.ProfileImage;

import twitter4j.ResponseList;

import twitter4j.Status;

import twitter4j.Twitter;

import twitter4j.TwitterException;

import twitter4j.TwitterResponse;

import twitter4j.User;

public class HomePageFragment extends SherlockFragment
        implements OnScrollListener {

    private static final String TAG = "Finch/HomePageFragment";

    private ListView mMainList;
    private LazyAdapter mMainListAdapter;

    private Twitter mTwitter;

    private AccessToken mAccessToken;

    private SharedPreferences mPrefs;

    private ResponseList<TwitterResponse> mHomeTimeline;

    private ActionMode mMode;

    private Context mContext;

    private int mHomeTimelinePage = 1;

    private boolean mLoadingPage = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mContext = getSherlockActivity().getApplicationContext();
        mPrefs = getSherlockActivity().getSharedPreferences(
                "twitterPrefs", Context.MODE_PRIVATE);
        mTwitter = FinchTwitterFactory.getInstance(mContext).getTwitter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        RelativeLayout layout = (RelativeLayout)inflater
            .inflate(R.layout.standard_list_fragment, container, false);

        initMainList(layout);

        return layout;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount,
            int totalCount) {

        if (totalCount <= 0 || mLoadingPage)
            return;

        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if (loadMore) {
            mLoadingPage = true;
            loadNextPage();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView v, int s) {
    }

    @Override
    public void onPause() {
        super.onPause();
        //savePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHomeTimeline(new Paging(1));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshHomeTimeline(new Paging(1));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initMainList(ViewGroup layout) {
        mMainList = (ListView)layout.findViewById(R.id.list);
        mMainListAdapter = new LazyAdapter(getSherlockActivity());
        mMainList.setAdapter(mMainListAdapter);
        mMainList.setOnScrollListener(this);
        mMainList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                    int position, long id) {
                Intent profileActivity = new Intent(
                    HomePageFragment.this.getSherlockActivity(),
                    ProfileActivity.class);
                String screenName = (
                    (Status)mHomeTimeline.get(position)).getUser()
                        .getScreenName();
                profileActivity.setData(Uri.parse(FinchProvider.CONTENT_URI +
                        "/" + screenName));
                HomePageFragment.this.startActivity(profileActivity);
            }
        });
        mMainList.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mMode = getSherlockActivity().startActionMode(
                    new ActionModeTweet());
                return true;
            }
        });
    }

    private void loadNextPage() {
        TwitterTaskCallback pullUpRefreshCallback =
                new TwitterTaskCallback<TwitterTaskParams,
                                        TwitterException>() {
            public void onSuccess(TwitterTaskParams payload) {
                /* Append responses to list adapter */
                mHomeTimeline = (ResponseList<TwitterResponse>)
                    payload.result;
                mMainListAdapter.appendResponses((ResponseList)
                        mHomeTimeline);
                mMainListAdapter.notifyDataSetChanged();
                mLoadingPage = false;
            }
            public void onFailure(TwitterException e) {
                e.printStackTrace();
            }
        };
        Paging paging = new Paging(++mHomeTimelinePage);
        Log.d(TAG, "Fetching page " + mHomeTimelinePage);
        TwitterTaskParams getTimelineParams =
             new TwitterTaskParams(TwitterTask.GET_HOME_TIMELINE,
                 new Object[] { getSherlockActivity(), mMainListAdapter,
                     mMainList, paging});
        new TwitterTask(getTimelineParams, pullUpRefreshCallback,
            mTwitter).execute();
    }

    private void savePosition() {
        /* Save currently displayed tweets */
        try {
            FileOutputStream fos = mContext.openFileOutput(
                    Constants.PREF_HOMETIMELINE_PAGE, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(mHomeTimeline);
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* Save page position */
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(Constants.PREF_HOMETIMELINE_POS,
                mMainList.getFirstVisiblePosition());
        editor.commit();
    }

    public void refreshHomeTimeline(Paging page) {
        TwitterTaskParams getTimelineParams =
            new TwitterTaskParams(TwitterTask.GET_HOME_TIMELINE,
                    new Object[] {getSherlockActivity(), mMainListAdapter,
                        mMainList, page});
        TwitterTaskCallback mHomeListCallback = new TwitterTaskCallback
                <TwitterTaskParams, TwitterException>() {
            public void onSuccess(TwitterTaskParams payload) {
                mHomeTimeline = (ResponseList<TwitterResponse>)payload.result;
                mMainListAdapter.prependResponses((ResponseList)mHomeTimeline);
                mMainListAdapter.notifyDataSetChanged();
            }
            public void onFailure(TwitterException e) {
                e.printStackTrace();
            }
        };
        new TwitterTask(getTimelineParams, mHomeListCallback,
                mTwitter).execute();
    }

    private void showUserInActionbar() {
        /* Set up callback to set user's profile image to actionbar */
        final TwitterTaskCallback<TwitterTaskParams, TwitterException>
            profileImageCallback =  new TwitterTaskCallback<TwitterTaskParams,
                                                    TwitterException>() {

            public void onSuccess(TwitterTaskParams payload) {
                Drawable profileImage = (Drawable)payload.result;
                ImageView homeIcon = (ImageView)HomePageFragment.this
                    .getSherlockActivity().findViewById(android.R.id.home);
                int abHeight = ((FinchActivity)HomePageFragment
                        .this.getSherlockActivity()).getSupportActionBar()
                        .getHeight();
                try {
                    homeIcon.setLayoutParams(new FrameLayout.LayoutParams(
                                abHeight, abHeight));
                    homeIcon.setPadding(0, 10, 10, 10);
                    homeIcon.setImageDrawable(profileImage);
                } catch (NullPointerException e) {
                    //TODO: Problem on <3.0, need to test further. Hopefully
                    //ABS 4.0 might fix this.
                    Log.e(TAG, "Could not get reference to home icon");
                    e.printStackTrace();
                }
            }
            public void onFailure(TwitterException e) {
                e.printStackTrace();
            }
        };

        TwitterTaskCallback<TwitterTaskParams, TwitterException>
            showUserCallback =  new TwitterTaskCallback<TwitterTaskParams,
                                                    TwitterException>() {
            public void onSuccess(TwitterTaskParams payload) {
                String screenName = ((User)payload.result).getScreenName();
                ((FinchActivity)HomePageFragment.this.getSherlockActivity()).
                    getSupportActionBar().setSubtitle(screenName);
                SharedPreferences prefs =
                    HomePageFragment.this.getSherlockActivity()
                    .getSharedPreferences("twitterPrefs",
                            Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.PREF_SCREEN_NAME, screenName);
                editor.commit();

                /* Now we have screenName, start another thread to get the
                 * profile image */
                TwitterTaskParams showProfileImageParams =
                    new TwitterTaskParams(TwitterTask.GET_PROFILE_IMAGE,
                        new Object[] {getSherlockActivity(), screenName,
                            ProfileImage.NORMAL});
                new TwitterTask(showProfileImageParams, profileImageCallback,
                        mTwitter).execute();
            }
            public void onFailure(TwitterException e) {
                e.printStackTrace();
            }
        };

        /* Set actionbar subtitle to user's username, and home icon to user's
         * profile image */
        TwitterTaskParams showUserParams = new TwitterTaskParams(
                TwitterTask.SHOW_USER,
                new Object[] {getSherlockActivity(),
                    ((FinchActivity)getSherlockActivity()).getAccessToken()
                .getUserId()});
        new TwitterTask(showUserParams, showUserCallback,
                mTwitter).execute();
    }

    private final class ActionModeTweet implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            /* Used to put dark icons on light action bar */
            //boolean isLight = (Constants.THEME == Constants.THEME_LIGHT);
            boolean isLight = false;

            menu.add("Reply")
                .setIcon(isLight ? R.drawable.social_reply_light
                        : R.drawable.social_reply_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add("Re-tweet")
                .setIcon(isLight ? R.drawable.av_repeat_light
                        : R.drawable.av_repeat_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add("Favorite")
                .setIcon(isLight ? R.drawable.rating_not_important_light
                        : R.drawable.rating_not_important_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            menu.add("Share")
                .setIcon(isLight ? R.drawable.ic_action_share_light
                        : R.drawable.ic_action_share_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }
}
