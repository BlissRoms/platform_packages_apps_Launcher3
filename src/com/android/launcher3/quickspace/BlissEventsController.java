/*
 * Copyright (C) 2026 The BlissRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.android.launcher3.quickspace;

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.view.View.OnClickListener;

import com.android.launcher3.R;
import com.android.launcher3.quickspace.receivers.QuickSpaceActionReceiver;
import com.android.launcher3.util.MediaSessionManagerHelper;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class BlissEventsController {

    private static final long ROTATION_INTERVAL_MS = 10_000L;

    private final Context mContext;
    private final Resources mResources;
    private final Handler mHandler = MAIN_EXECUTOR.getHandler();

    private String mRow3Text = "";
    private Drawable mRow3Icon = null;
    private OnClickListener mRow3Action = null;

    private int mRotationState = 0;

    private boolean mPlayingActive = false;
    private String mNowPlayingTitle = "";
    private String mNowPlayingArtist = "";

    private String mEventText = "";
    private String mEventSubtitle = "";
    private boolean mHasCalendarEvent = false;

    private ContentObserver mCalendarObserver;

    public interface OnRow3ChangedListener {
        void onRow3Changed();
    }
    private OnRow3ChangedListener mRow3Listener;

    public interface OnPagesChangedListener {
        void onPagesChanged();
    }
    private OnPagesChangedListener mPagesListener;

    private final Runnable mRotationRunnable = new Runnable() {
        @Override
        public void run() {
            mRotationState = (mRotationState == 0) ? 1 : 0;
            rebuildRow3();
            if (mRow3Listener != null) mRow3Listener.onRow3Changed();
            scheduleNextRotation();
        }
    };

    public BlissEventsController(Context context) {
        mContext = context;
        mResources = context.getResources();
    }

    public void setOnRow3ChangedListener(OnRow3ChangedListener l) {
        mRow3Listener = l;
    }

    public void setOnPagesChangedListener(OnPagesChangedListener l) {
        mPagesListener = l;
    }

    public void setMediaInfo(String title, String artist, boolean active) {
        boolean wasActive = mPlayingActive;
        mNowPlayingTitle = title != null ? title : "";
        mNowPlayingArtist = artist != null ? artist : "";
        mPlayingActive = active;
        rebuildRow3();
        startRotation();
        if (wasActive != mPlayingActive && mPagesListener != null) {
            mPagesListener.onPagesChanged();
        }
    }

    public void updateEvents() {
        MODEL_EXECUTOR.execute(this::queryNextCalendarEvent);
    }

    private void queryNextCalendarEvent() {
        long now = System.currentTimeMillis();
        long windowEnd = now + 24 * 60 * 60 * 1000L;

        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, now);
        ContentUris.appendId(builder, windowEnd);

        String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.ALL_DAY,
        };
        String selection = CalendarContract.Instances.BEGIN + " >= ? AND "
                + CalendarContract.Instances.ALL_DAY + " = 0";
        String[] selArgs = { String.valueOf(now) };

        String title = null;
        String subtitle = null;
        try {
            ContentResolver cr = mContext.getContentResolver();
            Cursor cursor = cr.query(builder.build(), projection, selection, selArgs,
                    CalendarContract.Instances.BEGIN + " ASC");
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    title = cursor.getString(0);
                    long begin = cursor.getLong(1);
                    String location = cursor.getString(3);
                    subtitle = buildSubtitle(begin, location);
                }
                cursor.close();
            }
        } catch (Exception e) {
            // ignored
        }

        final String finalTitle = title;
        final String finalSubtitle = subtitle;
        MAIN_EXECUTOR.execute(() -> {
            boolean wasActive = mHasCalendarEvent;
            mHasCalendarEvent = finalTitle != null && !finalTitle.isEmpty();
            mEventText = mHasCalendarEvent ? finalTitle : "";
            mEventSubtitle = finalSubtitle != null ? finalSubtitle : "";
            rebuildRow3();
            startRotation();
            if (wasActive != mHasCalendarEvent && mPagesListener != null) {
                mPagesListener.onPagesChanged();
            }
        });
    }

    private String buildSubtitle(long beginMs, String location) {
        long diffMs = beginMs - System.currentTimeMillis();
        String timeStr;
        if (diffMs <= 0) {
            timeStr = mContext.getString(R.string.qs_event_now);
        } else {
            long minutes = diffMs / 60_000;
            if (minutes < 60) {
                timeStr = mContext.getResources().getQuantityString(
                        R.plurals.qs_event_in_minutes, (int) minutes, (int) minutes);
            } else {
                long hours = minutes / 60;
                timeStr = mContext.getResources().getQuantityString(
                        R.plurals.qs_event_in_hours, (int) hours, (int) hours);
            }
        }
        if (location != null && !location.isEmpty()) {
            return timeStr + " · " + location;
        }
        return timeStr;
    }

    private void rebuildRow3() {
        boolean bothActive = mPlayingActive && mHasCalendarEvent;

        if (bothActive) {
            if (mRotationState == 0) {
                buildNowPlayingRow3();
            } else {
                buildCalendarRow3();
            }
        } else if (mPlayingActive) {
            buildNowPlayingRow3();
        } else if (mHasCalendarEvent) {
            buildCalendarRow3();
        } else {
            buildPsaRow3();
        }
    }

    private void buildNowPlayingRow3() {
        mRow3Text = mNowPlayingTitle.isEmpty()
                ? mResources.getString(R.string.qe_now_playing_unknown_artist)
                : mNowPlayingTitle + " · " + (mNowPlayingArtist.isEmpty()
                        ? mResources.getString(R.string.qe_now_playing_unknown_artist)
                        : mNowPlayingArtist);
        mRow3Icon = MediaSessionManagerHelper.getInstance(mContext).getMediaAppIcon();
        mRow3Action = v -> MediaSessionManagerHelper.getInstance(mContext).launchMediaApp();
    }

    private void buildCalendarRow3() {
        mRow3Text = mEventText != null ? mEventText : "";
        mRow3Icon = mResources.getDrawable(R.drawable.ic_schedule, mContext.getTheme());
        mRow3Action = QuickSpaceActionReceiver.getCalendarAction();
    }

    private void buildPsaRow3() {
        String[] psa = mResources.getStringArray(R.array.quickspace_psa_random);
        if (psa.length == 0) {
            mRow3Text = "";
            mRow3Icon = null;
            mRow3Action = null;
            return;
        }
        mRow3Text = psa[ThreadLocalRandom.current().nextInt(psa.length)];
        mRow3Icon = null;
        mRow3Action = null;
    }

    private void scheduleNextRotation() {
        if (mPlayingActive && mHasCalendarEvent) {
            mHandler.postDelayed(mRotationRunnable, ROTATION_INTERVAL_MS);
        }
    }

    public void onResume() {
        registerCalendarObserver();
        updateEvents();
        rebuildRow3();
        startRotation();
    }

    public void onPause() {
        stopRotation();
        unregisterCalendarObserver();
    }

    public void onDestroy() {
        stopRotation();
        unregisterCalendarObserver();
        mRow3Listener = null;
        mPagesListener = null;
    }

    private void registerCalendarObserver() {
        if (mCalendarObserver != null) return;
        mCalendarObserver = new ContentObserver(MAIN_EXECUTOR.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateEvents();
            }
        };
        try {
            mContext.getContentResolver().registerContentObserver(
                    CalendarContract.Instances.CONTENT_URI, true, mCalendarObserver);
        } catch (Exception e) {
            // ignored
        }
    }

    private void unregisterCalendarObserver() {
        if (mCalendarObserver == null) return;
        try {
            mContext.getContentResolver().unregisterContentObserver(mCalendarObserver);
        } catch (Exception ignored) {}
        mCalendarObserver = null;
    }

    private void startRotation() {
        stopRotation();
        if (mPlayingActive && mHasCalendarEvent) {
            mHandler.postDelayed(mRotationRunnable, ROTATION_INTERVAL_MS);
        }
    }

    private void stopRotation() {
        mHandler.removeCallbacks(mRotationRunnable);
    }

    public String getDateText() {
        String skeleton = mContext.getString(R.string.quickspace_date_format);
        DateFormat fmt = DateFormat.getInstanceForSkeleton(skeleton, Locale.getDefault());
        fmt.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
        return fmt.format(System.currentTimeMillis());
    }

    public String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour <= 11)  return mResources.getString(R.string.quickspace_grt_morning);
        if (hour >= 12 && hour <= 15) return mResources.getString(R.string.quickspace_grt_afternoon);
        if (hour >= 16 && hour <= 20) return mResources.getString(R.string.quickspace_grt_evening);
        return mResources.getString(R.string.quickspace_grt_night);
    }

    public String getRow3Text()        { return mRow3Text; }
    public Drawable getRow3Icon()      { return mRow3Icon; }
    public OnClickListener getRow3Action() { return mRow3Action; }

    public String getPsaText() {
        String[] psa = mResources.getStringArray(R.array.quickspace_psa_random);
        if (psa.length == 0) return "";
        return psa[ThreadLocalRandom.current().nextInt(psa.length)];
    }

    public boolean isPlayingActive()      { return mPlayingActive; }
    public String getNowPlayingTitle()    { return mNowPlayingTitle; }
    public String getNowPlayingArtist()   { return mNowPlayingArtist; }
    public boolean isCalendarEventActive(){ return mHasCalendarEvent; }
    public String getEventTitle()         { return mEventText; }
    public String getEventSubtitle()      { return mEventSubtitle; }

    public String getNextAlarmText() {
        android.app.AlarmManager am =
                (android.app.AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return null;
        android.app.AlarmManager.AlarmClockInfo next = am.getNextAlarmClock();
        if (next == null) return null;
        return android.text.format.DateFormat.getTimeFormat(mContext)
                .format(new java.util.Date(next.getTriggerTime()));
    }
}
