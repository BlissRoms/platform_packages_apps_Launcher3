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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.os.Handler;
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

    // Row 3 state
    private String mRow3Text = "";
    private Drawable mRow3Icon = null;
    private OnClickListener mRow3Action = null;

    // Rotation state: 0 = showing NP, 1 = showing event
    private int mRotationState = 0;

    // NowPlaying
    private boolean mPlayingActive = false;
    private String mNowPlayingTitle = "";
    private String mNowPlayingArtist = "";

    // Calendar / PSA
    private String mEventText = "";
    private boolean mHasCalendarEvent = false;

    // Listener called by BlissSpaceView to rebind row 3
    public interface OnRow3ChangedListener {
        void onRow3Changed();
    }
    private OnRow3ChangedListener mRow3Listener;

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

    public void setMediaInfo(String title, String artist, boolean active) {
        mNowPlayingTitle = title != null ? title : "";
        mNowPlayingArtist = artist != null ? artist : "";
        mPlayingActive = active;
        rebuildRow3();
        startRotation();
    }

    public void updateEvents() {
        // PSA and greeting are derived from time — no external data needed.
        // Calendar event integration is a future extension; for now treat as absent.
        mHasCalendarEvent = false;
        mEventText = "";
        rebuildRow3();
        startRotation();
    }

    private void rebuildRow3() {
        boolean bothActive = mPlayingActive && mHasCalendarEvent;

        if (bothActive) {
            // Rotation mode: state determines which to show
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
        rebuildRow3();
        startRotation();
    }

    public void onPause() {
        stopRotation();
    }

    public void onDestroy() {
        stopRotation();
        mRow3Listener = null;
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

    // ── Public API consumed by BlissSpaceView ──────────────────────────────

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
}
