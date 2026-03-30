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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.quickspace.QuickspaceController.OnDataListener;
import com.android.launcher3.quickspace.receivers.QuickSpaceActionReceiver;
import com.android.launcher3.util.BluetoothBatteryHelper;
import com.android.launcher3.util.MediaSessionManagerHelper;
import com.android.launcher3.util.Themes;

import java.util.ArrayList;
import java.util.List;

public class BlissSpaceView extends FrameLayout implements OnDataListener {

    private static final String TAG = "Launcher3:BlissSpaceView";

    private static final Interpolator ANIMATE_IN  = new DecelerateInterpolator();
    private static final Interpolator ANIMATE_OUT = new AccelerateInterpolator();
    private static final long PAGE_ANIM_DURATION = 180;

    private static final int PAGE_BLISS    = 0;
    private static final int PAGE_WEATHER  = 1;
    private static final int PAGE_CALENDAR = 2;
    private static final int PAGE_MUSIC    = 3;
    private static final int PAGE_BLUETOOTH = 4;

    private QuickspaceController mController;
    private BlissEventsController mEventsController;
    private boolean mFinishedInflate;
    private boolean mListenerRegistered;

    private View      mContentContainer;
    private View      mSharedRows;
    private TextView  mDateView;
    private View      mDotSeparator;
    private TextView  mAlarmText;
    private ImageView mWeatherIcon;
    private TextView  mWeatherText;
    private TextView  mGreetingText;
    private ImageView mRow3Icon;
    private TextView  mRow3Text;
    private ColorStateList mDotTint;

    private ImageView mAlarmIcon;

    private View      mMusicContainer;
    private ImageView mMusicAppIcon;
    private TextView  mMusicTitle;
    private TextView  mMusicArtist;
    private View      mMusicControls;
    private ImageView mMusicPrev;
    private ImageView mMusicPlayPause;
    private ImageView mMusicNext;
    private ImageView mAlbumArt;

    private LinearLayout mDotsContainer;
    private LinearLayout mBtCompact;
    private LinearLayout mBtContainer;

    private final List<Integer> mPages = new ArrayList<>();
    private int mCurrentPage = 0;
    private GestureDetector mGestureDetector;

    private boolean mInterceptionDisallowed = false;

    public BlissSpaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!LauncherPrefs.SHOW_QUICKSPACE.get(context)) return;
        mController = new QuickspaceController(context);
        mEventsController = new BlissEventsController(context);
        mEventsController.setOnRow3ChangedListener(this::onRow3Changed);
        mEventsController.setOnPagesChangedListener(this::onEventsPageDataChanged);
        setClipChildren(false);
        mGestureDetector = new GestureDetector(context, new PageGestureListener());
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (mController == null) return;

        mContentContainer = findViewById(R.id.bliss_quickspace_content);
        mSharedRows   = findViewById(R.id.bliss_shared_rows);
        mDateView     = findViewById(R.id.bliss_date);
        mDotSeparator = findViewById(R.id.bliss_row2_dot);
        mAlarmIcon    = findViewById(R.id.bliss_alarm_icon);
        mAlarmText    = findViewById(R.id.bliss_alarm_text);
        mWeatherIcon  = findViewById(R.id.bliss_weather_icon);
        mWeatherText  = findViewById(R.id.bliss_weather_text);
        mGreetingText = findViewById(R.id.bliss_greeting_text);
        mRow3Icon     = findViewById(R.id.bliss_row3_icon);
        mRow3Text     = findViewById(R.id.bliss_row3_text);

        mMusicContainer  = findViewById(R.id.bliss_music_container);
        mMusicAppIcon    = findViewById(R.id.bliss_music_app_icon);
        mMusicTitle      = findViewById(R.id.bliss_music_title);
        mMusicArtist     = findViewById(R.id.bliss_music_artist);
        mMusicControls   = findViewById(R.id.bliss_music_controls);
        mMusicPrev       = findViewById(R.id.bliss_music_prev);
        mMusicPlayPause  = findViewById(R.id.bliss_music_play_pause);
        mMusicNext       = findViewById(R.id.bliss_music_next);
        mAlbumArt        = findViewById(R.id.bliss_music_album_art);
        mAlbumArt.setClipToOutline(true);
        mBtCompact   = findViewById(R.id.bliss_bt_compact);
        mBtContainer = findViewById(R.id.bliss_bt_container);

        mDotTint = ColorStateList.valueOf(
                Themes.getAttrColor(getContext(), R.attr.workspaceTextColor));
        mDotSeparator.setBackgroundTintList(mDotTint);
        mAlarmIcon.setImageTintList(mDotTint);
        mMusicPrev.setImageTintList(mDotTint);
        mMusicPlayPause.setImageTintList(mDotTint);
        mMusicNext.setImageTintList(mDotTint);

        mDotsContainer = new LinearLayout(getContext());
        mDotsContainer.setOrientation(LinearLayout.HORIZONTAL);
        int dotsPad = (int) (getResources().getDisplayMetrics().density * 6);
        FrameLayout.LayoutParams dotsLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.END | android.view.Gravity.BOTTOM);
        dotsLp.setMarginEnd(dotsPad);
        dotsLp.bottomMargin = dotsPad;
        addView(mDotsContainer, dotsLp);

        mFinishedInflate = true;
        if (isAttachedToWindow() && !mListenerRegistered) {
            mController.addListener(this);
            mListenerRegistered = true;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mController != null && mFinishedInflate && !mListenerRegistered) {
            mListenerRegistered = true;
            mController.addListener(this);
        }
        if (mEventsController != null) mEventsController.onResume();
    }

    @Override
    public void onDetachedFromWindow() {
        if (mController != null) {
            mController.removeListener(this);
            mListenerRegistered = false;
        }
        if (mEventsController != null) mEventsController.onPause();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mPages.size() > 1) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                mInterceptionDisallowed = false;
            }
            mGestureDetector.onTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mPages.size() > 1) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                mInterceptionDisallowed = false;
            } else if (action == MotionEvent.ACTION_MOVE && !mInterceptionDisallowed) {
                getParent().requestDisallowInterceptTouchEvent(true);
                mInterceptionDisallowed = true;
            } else if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                if (mInterceptionDisallowed) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    mInterceptionDisallowed = false;
                }
            }
            mGestureDetector.onTouchEvent(ev);
            return true;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void onDataUpdated() {
        if (mController == null || mDateView == null) return;
        MediaSessionManagerHelper msm = MediaSessionManagerHelper.getInstance(getContext());
        boolean sessionActive = msm != null && msm.isMediaSessionActive();
        MediaMetadata meta = sessionActive ? msm.getCurrentMediaMetadata() : null;
        String mediaTitle  = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_TITLE) : null;
        String mediaArtist = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_ARTIST) : null;
        if (mediaArtist == null || mediaArtist.isEmpty()) {
            mediaArtist = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) : null;
        }
        mEventsController.setMediaInfo(mediaTitle, mediaArtist, sessionActive);
        rebuildPages();
        bindCurrentPage();
    }

    private void onRow3Changed() {
        if (mCurrentPage == PAGE_BLISS) bindBlissPage();
    }

    private void onEventsPageDataChanged() {
        if (mDateView == null) return;
        int previousPage = mCurrentPage;
        rebuildPages();
        if (mCurrentPage != previousPage) bindCurrentPage();
        else updateDots();
    }

    private void rebuildPages() {
        int previousPageType = mPages.isEmpty() ? PAGE_BLISS : mPages.get(mCurrentPage);

        mPages.clear();
        mPages.add(PAGE_BLISS);
        if (mController.isWeatherAvailable()) mPages.add(PAGE_WEATHER);
        mPages.add(PAGE_CALENDAR);
        mPages.add(PAGE_MUSIC);
        if (mController.isBluetoothAvailable()) mPages.add(PAGE_BLUETOOTH);

        int newIndex = mPages.indexOf(previousPageType);
        mCurrentPage = newIndex >= 0 ? newIndex : 0;

        rebuildDots();
    }

    private void rebuildDots() {
        if (mDotsContainer == null) return;
        mDotsContainer.removeAllViews();
        if (mPages.size() <= 1) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int gap = (int) (getResources().getDisplayMetrics().density * 4);
        for (int i = 0; i < mPages.size(); i++) {
            View dot = inflater.inflate(R.layout.qs_dot_indicator, mDotsContainer, false);
            if (i > 0) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
                lp.setMarginStart(gap);
                dot.setLayoutParams(lp);
            }
            mDotsContainer.addView(dot);
        }
        updateDots();
    }

    private void updateDots() {
        if (mDotsContainer == null) return;
        float density = getResources().getDisplayMetrics().density;
        int activeW  = (int) (14 * density);
        int inactiveW = (int) (4 * density);
        int dotH = (int) (4 * density);
        for (int i = 0; i < mDotsContainer.getChildCount(); i++) {
            View dot = mDotsContainer.getChildAt(i);
            boolean selected = (i == mCurrentPage);
            dot.setSelected(selected);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = selected ? activeW : inactiveW;
            lp.height = dotH;
            dot.setLayoutParams(lp);
        }
    }

    private void bindCurrentPage() {
        switch (mPages.get(mCurrentPage)) {
            case PAGE_WEATHER:    bindWeatherPage();    break;
            case PAGE_CALENDAR:   bindCalendarPage();   break;
            case PAGE_MUSIC:      bindMusicPage();      break;
            case PAGE_BLUETOOTH:  bindBluetoothPage();  break;
            default:              bindBlissPage();      break;
        }
        updateDots();
    }

    private void navigateTo(int newPage) {
        if (mPages.isEmpty()) return;
        newPage = ((newPage % mPages.size()) + mPages.size()) % mPages.size();
        if (newPage == mCurrentPage) return;
        mCurrentPage = newPage;
        mContentContainer.animate().cancel();
        mContentContainer.animate()
                .alpha(0f)
                .setDuration(PAGE_ANIM_DURATION)
                .setInterpolator(ANIMATE_OUT)
                .withEndAction(() -> {
                    bindCurrentPage();
                    mContentContainer.animate()
                            .alpha(1f)
                            .setDuration(PAGE_ANIM_DURATION)
                            .setInterpolator(ANIMATE_IN)
                            .start();
                })
                .start();
    }

    private void bindBlissPage() {
        showContentRows(0);
        mDateView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 26f);
        mDateView.setText(mEventsController.getDateText());
        mDateView.setOnClickListener(QuickSpaceActionReceiver.getCalendarAction());

        String alarm = mEventsController.getNextAlarmText();
        if (alarm != null) {
            mAlarmText.setText(alarm);
            mAlarmText.setVisibility(View.VISIBLE);
            mAlarmIcon.setVisibility(View.VISIBLE);
            mDotSeparator.setVisibility(View.VISIBLE);
        } else {
            mAlarmText.setVisibility(View.GONE);
            mAlarmIcon.setVisibility(View.GONE);
            mDotSeparator.setVisibility(View.GONE);
        }

        mWeatherIcon.setVisibility(View.GONE);
        mWeatherText.setVisibility(View.GONE);

        mGreetingText.setText(mEventsController.getGreeting());
        mGreetingText.setVisibility(View.VISIBLE);
        mGreetingText.setOnClickListener(null);

        // Bluetooth compact indicator
        if (mBtCompact != null) {
            mBtCompact.removeAllViews();
            if (mController.isBluetoothAvailable()) {
                java.util.List<BluetoothBatteryHelper.BtDeviceInfo> devices =
                        mController.getBluetoothDevices();
                int count = Math.min(devices.size(), 3);
                float density = getResources().getDisplayMetrics().density;
                int iconSize = (int) (16 * density);
                for (int i = 0; i < count; i++) {
                    BluetoothBatteryHelper.BtDeviceInfo info = devices.get(i);
                    LinearLayout item = new LinearLayout(getContext());
                    item.setOrientation(LinearLayout.HORIZONTAL);
                    item.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    if (i > 0) {
                        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        itemLp.setMarginStart((int) (12 * density));
                        item.setLayoutParams(itemLp);
                    }

                    ImageView icon = new ImageView(getContext());
                    icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
                    icon.setImageResource(getBluetoothDeviceIcon(info.deviceType));
                    icon.setImageTintList(mDotTint);
                    item.addView(icon);

                    TextView text = new TextView(getContext());
                    text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f);
                    text.setTextColor(mDotTint);
                    LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    textLp.setMarginStart((int) (3 * density));
                    text.setLayoutParams(textLp);
                    text.setText(info.batteryLevel >= 0
                            ? info.batteryLevel + "%"
                            : getContext().getString(R.string.bt_device_connected));
                    item.addView(text);

                    mBtCompact.addView(item);
                }
                mBtCompact.setVisibility(View.VISIBLE);
            } else {
                mBtCompact.setVisibility(View.GONE);
            }
        }

        mRow3Icon.setVisibility(View.GONE);
        if (LauncherPrefs.SHOW_QUICKSPACE_PSONALITY.get(getContext())) {
            mRow3Text.setText(mEventsController.getPsaText());
            mRow3Text.setVisibility(View.VISIBLE);
        } else {
            mRow3Text.setVisibility(View.GONE);
        }
        mRow3Text.setOnClickListener(null);
    }

    private void bindWeatherPage() {
        showContentRows(0);
        if (mBtCompact != null) mBtCompact.setVisibility(View.GONE);
        String temp = mController.getWeatherTempOnly();
        String city = mController.getWeatherCity();
        Drawable icon = mController.getWeatherIcon();

        mDateView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f);
        mDateView.setText(temp != null ? temp : "");
        mDateView.setOnClickListener(QuickSpaceActionReceiver.getWeatherAction());

        if (icon != null) {
            mWeatherIcon.setImageDrawable(icon);
            mWeatherIcon.setVisibility(View.VISIBLE);
        } else {
            mWeatherIcon.setVisibility(View.GONE);
        }
        boolean showCity = LauncherPrefs.SHOW_QUICKSPACE_WEATHER_CITY.get(getContext());
        if (showCity && city != null && !city.isEmpty()) {
            mWeatherText.setText(city);
            mWeatherText.setVisibility(View.VISIBLE);
        } else {
            mWeatherText.setVisibility(View.GONE);
        }
        mWeatherText.setOnClickListener(QuickSpaceActionReceiver.getWeatherAction());
        mDotSeparator.setVisibility(View.GONE);
        mAlarmIcon.setVisibility(View.GONE);
        mAlarmText.setVisibility(View.GONE);
        mGreetingText.setVisibility(View.GONE);
        mRow3Icon.setVisibility(View.GONE);
        mRow3Text.setText("");
        mRow3Text.setOnClickListener(QuickSpaceActionReceiver.getWeatherAction());
    }

    private void bindCalendarPage() {
        showContentRows(0);
        if (mBtCompact != null) mBtCompact.setVisibility(View.GONE);
        String title    = mEventsController.getEventTitle();
        String subtitle = mEventsController.getEventSubtitle();
        boolean hasEvent = title != null && !title.isEmpty();

        mDateView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f);
        mDateView.setText(hasEvent ? title
                : getContext().getString(R.string.qs_no_upcoming_events));
        mDateView.setOnClickListener(QuickSpaceActionReceiver.getCalendarAction());

        mWeatherIcon.setImageResource(R.drawable.ic_qs_calendar);
        mWeatherIcon.setVisibility(View.VISIBLE);
        mWeatherIcon.setOnClickListener(QuickSpaceActionReceiver.getCalendarAction());

        if (hasEvent && subtitle != null && !subtitle.isEmpty()) {
            mWeatherText.setText(subtitle);
            mWeatherText.setVisibility(View.VISIBLE);
            mWeatherText.setOnClickListener(QuickSpaceActionReceiver.getCalendarAction());
        } else {
            mWeatherText.setVisibility(View.GONE);
        }
        mDotSeparator.setVisibility(View.GONE);
        mAlarmIcon.setVisibility(View.GONE);
        mAlarmText.setVisibility(View.GONE);
        mGreetingText.setVisibility(View.GONE);
        mRow3Icon.setVisibility(View.GONE);
        mRow3Text.setText("");
        mRow3Text.setOnClickListener(QuickSpaceActionReceiver.getCalendarAction());
    }

    private void showContentRows(int mode) {
        // mode 0 = shared rows, 1 = music, 2 = bluetooth
        if (mSharedRows != null) mSharedRows.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        if (mMusicContainer != null) mMusicContainer.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        if (mBtContainer != null) mBtContainer.setVisibility(mode == 2 ? View.VISIBLE : View.GONE);
    }

    private void bindMusicPage() {
        showContentRows(1);

        MediaSessionManagerHelper msm = MediaSessionManagerHelper.getInstance(getContext());
        boolean sessionActive = msm != null && msm.isMediaSessionActive();

        MediaMetadata meta = sessionActive ? msm.getCurrentMediaMetadata() : null;

        String title  = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_TITLE)  : null;
        String artist = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_ARTIST) : null;
        if (artist == null || artist.isEmpty()) {
            artist = meta != null ? meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) : null;
        }

        boolean hasInfo = sessionActive && (title != null && !title.isEmpty());

        if (hasInfo) {
            mMusicTitle.setText(title);
            mMusicTitle.setOnClickListener(v -> { if (msm != null) msm.launchMediaApp(); });

            Drawable appIcon = msm != null ? msm.getMediaAppIcon() : null;
            if (appIcon != null) {
                mMusicAppIcon.setImageDrawable(appIcon);
                mMusicAppIcon.setVisibility(View.VISIBLE);
            } else {
                mMusicAppIcon.setImageResource(R.drawable.ic_qs_music);
                mMusicAppIcon.setVisibility(View.VISIBLE);
            }

            if (artist != null && !artist.isEmpty()) {
                mMusicArtist.setText(artist);
                mMusicArtist.setVisibility(View.VISIBLE);
            } else {
                mMusicArtist.setVisibility(View.GONE);
            }

            mMusicControls.setVisibility(View.VISIBLE);
            mMusicPrev.setOnClickListener(v -> { if (msm != null) msm.prevSong(); });
            mMusicNext.setOnClickListener(v -> { if (msm != null) msm.nextSong(); });
            boolean playing = msm != null && msm.isMediaPlaying();
            mMusicPlayPause.setImageResource(playing ? R.drawable.ic_qs_pause : R.drawable.ic_qs_play);
            mMusicPlayPause.setOnClickListener(v -> {
                if (msm != null) {
                    msm.toggleMediaPlaybackState();
                    mMusicPlayPause.setImageResource(msm.isMediaPlaying()
                            ? R.drawable.ic_qs_pause : R.drawable.ic_qs_play);
                }
            });

            loadAlbumArt(msm);
        } else {
            mMusicAppIcon.setImageResource(R.drawable.ic_qs_music);
            mMusicAppIcon.setVisibility(View.VISIBLE);
            mMusicTitle.setText(getContext().getString(R.string.qs_no_media_playing));
            mMusicTitle.setOnClickListener(null);
            mMusicArtist.setVisibility(View.GONE);
            mMusicControls.setVisibility(View.GONE);
            mAlbumArt.setVisibility(View.GONE);
        }
    }

    private void loadAlbumArt(MediaSessionManagerHelper msm) {
        if (mAlbumArt == null) return;
        Bitmap art = msm != null ? msm.getAlbumArt() : null;
        if (art != null) {
            mAlbumArt.setImageBitmap(art);
            mAlbumArt.setVisibility(View.VISIBLE);
        } else {
            mAlbumArt.setVisibility(View.GONE);
        }
    }

    private void bindBluetoothPage() {
        showContentRows(2);

        if (mBtContainer == null) return;
        mBtContainer.removeAllViews();

        java.util.List<BluetoothBatteryHelper.BtDeviceInfo> devices =
                mController.getBluetoothDevices();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (BluetoothBatteryHelper.BtDeviceInfo info : devices) {
            View row = inflater.inflate(R.layout.quickspace_bt_device_row, mBtContainer, false);

            ImageView icon = row.findViewById(R.id.bt_device_icon);
            icon.setImageResource(getBluetoothDeviceIcon(info.deviceType));
            icon.setImageTintList(mDotTint);

            TextView name = row.findViewById(R.id.bt_device_name);
            name.setText(info.name);

            View dot = row.findViewById(R.id.bt_device_dot);
            dot.setBackgroundTintList(mDotTint);

            TextView battery = row.findViewById(R.id.bt_device_battery);
            battery.setText(info.batteryLevel >= 0
                    ? info.batteryLevel + "%"
                    : getContext().getString(R.string.bt_device_connected));

            mBtContainer.addView(row);
        }

        mBtContainer.setOnClickListener(v -> {
            try {
                getContext().startActivity(
                        new android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            } catch (Exception ignored) {}
        });
    }

    private int getBluetoothDeviceIcon(int deviceType) {
        switch (deviceType) {
            case BluetoothBatteryHelper.TYPE_WATCH:
                return R.drawable.ic_qs_bt_watch;
            case BluetoothBatteryHelper.TYPE_SPEAKER:
                return R.drawable.ic_qs_bt_speaker;
            default:
                return R.drawable.ic_qs_bt_headphone;
        }
    }

    private class PageGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 60;
        private static final int SWIPE_MIN_VELOCITY = 150;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
            if (e1 == null || e2 == null) return false;
            float dX = e2.getX() - e1.getX();
            float dY = e2.getY() - e1.getY();
            if (Math.abs(dX) > Math.abs(dY) * 1.2f
                    && Math.abs(dX) > SWIPE_MIN_DISTANCE
                    && Math.abs(vX) > SWIPE_MIN_VELOCITY) {
                navigateTo(dX < 0 ? mCurrentPage + 1 : mCurrentPage - 1);
                return true;
            }
            return false;
        }
    }

    public void onPause() {
        if (mController != null) mController.onPause();
        if (mEventsController != null) mEventsController.onPause();
    }

    public void onResume() {
        if (mController != null && mListenerRegistered) mController.onResume();
        if (mEventsController != null) mEventsController.onResume();
    }

    public void onDestroy() {
        if (mEventsController != null) {
            mEventsController.onDestroy();
            mEventsController = null;
        }
        if (mController != null) {
            mController.onDestroy();
            mController = null;
        }
        mDateView = mAlarmText = mWeatherText = mGreetingText = mRow3Text = mMusicTitle = mMusicArtist = null;
        mWeatherIcon = mAlarmIcon = mRow3Icon = mMusicAppIcon = mMusicPrev = mMusicPlayPause = mMusicNext = mAlbumArt = null;
        mDotSeparator = mMusicContainer = mMusicControls = mSharedRows = null;
        mBtCompact = mBtContainer = null;
    }

    @Override
    public void setPadding(int l, int t, int r, int b) {
        super.setPadding(0, 0, 0, 0);
    }
}
