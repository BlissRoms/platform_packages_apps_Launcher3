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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.quickspace.QuickspaceController.OnDataListener;
import com.android.launcher3.quickspace.receivers.QuickSpaceActionReceiver;
import com.android.launcher3.util.Themes;

public class BlissSpaceView extends FrameLayout implements OnDataListener {

    private static final String TAG = "Launcher3:BlissSpaceView";

    private static final Interpolator ANIMATE_IN  = new DecelerateInterpolator();
    private static final Interpolator ANIMATE_OUT = new AccelerateInterpolator();

    private QuickspaceController mController;
    private BlissEventsController mEventsController;
    private boolean mFinishedInflate;
    private boolean mListenerRegistered;

    // Views
    private TextView  mDateView;
    private ImageView mWeatherIcon;
    private TextView  mWeatherText;
    private View      mDotSeparator;
    private TextView  mGreetingText;
    private ImageView mRow3Icon;
    private TextView  mRow3Text;

    public BlissSpaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!LauncherPrefs.SHOW_QUICKSPACE.get(context)) return;
        mController = new QuickspaceController(context);
        mEventsController = new BlissEventsController(context);
        mEventsController.setOnRow3ChangedListener(this::bindRow3);
        setClipChildren(false);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (mController == null) return;
        mDateView     = findViewById(R.id.bliss_date);
        mWeatherIcon  = findViewById(R.id.bliss_weather_icon);
        mWeatherText  = findViewById(R.id.bliss_weather_text);
        mDotSeparator = findViewById(R.id.bliss_row2_dot);
        mGreetingText = findViewById(R.id.bliss_greeting_text);
        mRow3Icon     = findViewById(R.id.bliss_row3_icon);
        mRow3Text     = findViewById(R.id.bliss_row3_text);

        // Tint the dot separator to workspace text color (shape drawable can't use theme attrs)
        ColorStateList dotTint = ColorStateList.valueOf(
                Themes.getAttrColor(getContext(), R.attr.workspaceTextColor));
        mDotSeparator.setBackgroundTintList(dotTint);

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
    public void onDataUpdated() {
        if (mController == null || mDateView == null) return;

        // Bridge Now Playing state from QuickspaceController's QuickEventsController
        // into BlissEventsController so the rotation logic has accurate NP data.
        QuickEventsController events = mController.getEventController();
        mEventsController.setMediaInfo(
                events.getTitle(),
                events.getActionTitle(),
                events.isNowPlaying());

        // Row 1: date
        mDateView.setText(mEventsController.getDateText());
        mDateView.setOnClickListener(QuickSpaceActionReceiver.getCalendarAction());

        // Row 2: weather + greeting
        boolean weatherAvailable = mController.isWeatherAvailable();
        String weatherTemp = mController.getWeatherTemp();
        Drawable weatherIcon = mController.getWeatherIcon();

        if (weatherAvailable && weatherTemp != null && !weatherTemp.isEmpty()) {
            mWeatherText.setText(weatherTemp);
            mWeatherText.setVisibility(View.VISIBLE);
            mWeatherText.setOnClickListener(QuickSpaceActionReceiver.getWeatherAction());
            if (weatherIcon != null) {
                mWeatherIcon.setImageDrawable(weatherIcon);
                mWeatherIcon.setVisibility(View.VISIBLE);
                mWeatherIcon.setOnClickListener(QuickSpaceActionReceiver.getWeatherAction());
            } else {
                mWeatherIcon.setVisibility(View.GONE);
            }
            mDotSeparator.setVisibility(View.VISIBLE);
        } else {
            mWeatherIcon.setVisibility(View.GONE);
            mWeatherText.setVisibility(View.GONE);
            mDotSeparator.setVisibility(View.GONE);
        }

        mGreetingText.setText(mEventsController.getGreeting());

        // Row 3
        bindRow3();
    }

    private void bindRow3() {
        if (mRow3Text == null) return;
        String text = mEventsController.getRow3Text();
        Drawable icon = mEventsController.getRow3Icon();

        mRow3Text.setText(text != null ? text : "");
        mRow3Text.setOnClickListener(mEventsController.getRow3Action());

        if (icon != null) {
            mRow3Icon.setImageDrawable(icon);
            mRow3Icon.setOnClickListener(mEventsController.getRow3Action());
            if (mRow3Icon.getVisibility() != View.VISIBLE) animateIn(mRow3Icon);
        } else {
            if (mRow3Icon.getVisibility() == View.VISIBLE) animateOut(mRow3Icon);
        }
    }

    private void animateIn(View view) {
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(view.getHeight() / 2f);
        view.animate().alpha(1f).translationY(0f).setDuration(300)
                .setInterpolator(ANIMATE_IN).start();
    }

    private void animateOut(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        view.animate().cancel();
        view.animate().alpha(0f).translationY(view.getHeight() / 2f).setDuration(400)
                .setInterpolator(ANIMATE_OUT)
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
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
        mDateView = mWeatherText = mGreetingText = mRow3Text = null;
        mWeatherIcon = mRow3Icon = null;
        mDotSeparator = null;
    }

    @Override
    public void setPadding(int l, int t, int r, int b) {
        super.setPadding(0, 0, 0, 0);
    }
}
