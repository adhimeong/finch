package com.bourke.finch.lazylist;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.graphics.Typeface;

import android.net.Uri;

import android.text.util.Linkify;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bourke.finch.common.Constants;
import com.bourke.finch.common.PrettyDate;
import com.bourke.finch.ProfileActivity;
import com.bourke.finch.provider.FinchProvider;
import com.bourke.finch.R;

import java.util.Date;
import java.util.regex.Pattern;

import twitter4j.ResponseList;

import twitter4j.Status;

import twitter4j.TwitterResponse;

import twitter4j.User;
import java.util.ArrayList;

public class LazyAdapter extends BaseAdapter {

    private static final String TAG = "Finch/LazyAdapter";

    private Activity activity;

    private ResponseList<TwitterResponse> mResponses;

    private static LayoutInflater inflater = null;

    public ImageLoader imageLoader;

    private Pattern screenNameMatcher = Pattern.compile("@\\w+");

    private Typeface mTypeface;

    private View mLastSelectedView;

    /* Statuses recently marked favorite that should be updated on the next
     * call to getView */
    private ArrayList<Long> mFavQueue = new ArrayList<Long>();

    public LazyAdapter(Activity a) {
        activity = a;
        inflater = (LayoutInflater)activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mTypeface = Typeface.createFromAsset(a.getAssets(),
                Constants.ROBOTO_REGULAR);
        imageLoader = new ImageLoader(activity);
    }

    public View getView(final int position, View convertView,
            ViewGroup parent) {
        /* Init view holder */
        View vi = convertView;
        ViewHolder holder;
        if (convertView == null) {
            vi = inflater.inflate(R.layout.main_row, null);
            holder = initViewHolder(vi, position);
        } else {
            holder = (ViewHolder)vi.getTag();
        }

        /* Populate the view based on entity type */
        if (mResponses != null) {
            TwitterResponse currentEntity = mResponses.get(position);
            if (currentEntity instanceof User) {
                showUserView(holder, (User)currentEntity);
            } else if (currentEntity instanceof Status) {
                showStatusView(holder, (Status)currentEntity);
            } else {
                Log.e(TAG, "Unknown TwitterResponse type in getView");
            }
        }

        return vi;
    }

    private void showStatusView(ViewHolder holder, Status currentEntity) {
        /* Get the entity text */
        String text = ((Status)currentEntity).getText();
        holder.text_tweet.setText(text);
        holder.text_tweet.setTypeface(mTypeface);
        Linkify.addLinks(holder.text_tweet, Linkify.ALL);
        Linkify.addLinks(holder.text_tweet, screenNameMatcher,
                 Constants.SCREEN_NAME_URI.toString() + "/");

        /* Show star if status is favorited */
        long entityId = currentEntity.getId();
        if (mFavQueue.contains(entityId) || currentEntity.isFavorited()) {
            holder.imageFavStar.setVisibility(View.VISIBLE);
        } else {
            holder.imageFavStar.setVisibility(View.GONE);
        }

        /* Set the tweet time Textview */
        Date createdAt = ((Status)currentEntity).getCreatedAt();
        holder.text_time.setText(new PrettyDate(createdAt).toString());
        holder.text_time.setTypeface(mTypeface);

        /* Set the screen name TextView */
        String screenName = currentEntity.getUser().getScreenName();
        holder.text_screenname.setText("@"+screenName);
        holder.text_screenname.setTypeface(mTypeface);

        /* Set the profile image ImageView */
        imageLoader.displayImage(screenName, holder.image_profile);
    }

    private void showUserView(ViewHolder holder, User currentEntity) {
        /* Get the entity text. (If the user is protected, the status may be
         * null, so account for that) */
        String text = "";
        if (currentEntity.getStatus() == null) {
            // TODO: add to strings.xml
            text = "You need to follow this user to see their status.";
        } else {
            text = ((User)currentEntity).getStatus().getText();
        }
        holder.text_tweet.setText(text);
        holder.text_tweet.setTypeface(mTypeface);
        Linkify.addLinks(holder.text_tweet, Linkify.ALL);
        Linkify.addLinks(holder.text_tweet, screenNameMatcher,
                 Constants.SCREEN_NAME_URI.toString() + "/");

        /* Set the tweet time Textview */
        Date createdAt = new Date();
        if (currentEntity.getStatus() != null) {
            createdAt = ((User)currentEntity).getStatus()
                .getCreatedAt();
        }
        holder.text_time.setText(new PrettyDate(createdAt).toString());
        holder.text_time.setTypeface(mTypeface);

        /* Set the screen name TextView */
        String screenName = currentEntity.getScreenName();
        holder.text_screenname.setText("@"+screenName);
        holder.text_screenname.setTypeface(mTypeface);

        /* Set the profile image ImageView */
        imageLoader.displayImage(screenName, holder.image_profile);
    }

    private ViewHolder initViewHolder(View vi, final int position) {
        ViewHolder holder = new ViewHolder();
        holder.imageProfile = (ImageView)vi.findViewById(
                R.id.image_profile);
        holder.text_tweet = (TextView)vi.findViewById(R.id.text_tweet);
        holder.imageFavStar = (ImageView)vi.findViewById(
                R.id.image_fav_star);
        holder.text_time = (TextView)vi.findViewById(R.id.text_time);
        holder.text_screenname = (TextView)vi.findViewById(
                R.id.text_screenname);
        holder.image_profile = (ImageView)vi.findViewById(
                R.id.image_profile);

        vi.setTag(holder);

        holder.imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileActivity = new Intent(activity,
                    ProfileActivity.class);
                String screenName = ((Status)mResponses.get(position))
                    .getUser().getScreenName();
                profileActivity.setData(Uri.parse(
                        FinchProvider.CONTENT_URI + "/" + screenName));
                activity.startActivity(profileActivity);
            }
        });
        vi.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                view.setBackgroundResource(
                    android.R.color.holo_blue_light);
                mLastSelectedView = view;
                return false;
            }
        });

        return holder;
    }

    public int getCount() {
        int count = 0;
        if (mResponses != null) {
            count = mResponses.size();
        }
        return count;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public void prependResponses(ResponseList<TwitterResponse> data) {
        if (mResponses != null) {
            data.addAll(mResponses);
        }
        mResponses = data;
    }

    public void appendResponses(ResponseList<TwitterResponse> data) {
        if (mResponses != null) {
            mResponses.addAll(data);
        } else {
            mResponses = data;
        }
    }

    public void clearResponses() {
        mResponses.clear();
    }

    public ResponseList<TwitterResponse> getResponses() {
        return mResponses;
    }

    public void showFavStatus(Status statusToUpdate) {
        mFavQueue.add(statusToUpdate.getId());
    }

    public void unselectLastView() {
        if (mLastSelectedView != null) {
            mLastSelectedView.setBackgroundResource(
                    android.R.color.background_light);
        } else {
            Log.e(TAG, "mLastSelectedView == null");
        }
    }

    static class ViewHolder {
        ImageView imageProfile;
        TextView text_tweet;
        ImageView imageFavStar;
        TextView text_time;
        TextView text_screenname;
        ImageView image_profile;
    }

}
