/*
 * Copyright © 2014 Jerzy Chalupski
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.widget.fab;

import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;

public class TouchDelegateGroup extends TouchDelegate {
    private static final Rect USELESS_HACKY_RECT = new Rect();
    private final Collection<TouchDelegate> mTouchDelegates = new ArrayList<>();
    @Nullable private TouchDelegate mCurrentTouchDelegate;
    private boolean mEnabled;

    public TouchDelegateGroup(final View uselessHackyView) {
        super(USELESS_HACKY_RECT, uselessHackyView);
    }

    public void addTouchDelegate(final TouchDelegate touchDelegate) {
        mTouchDelegates.add(touchDelegate);
    }

    public void removeTouchDelegate(final TouchDelegate touchDelegate) {
        mTouchDelegates.remove(touchDelegate);
        if (mCurrentTouchDelegate == touchDelegate) {
            mCurrentTouchDelegate = null;
        }
    }

    public void clearTouchDelegates() {
        mTouchDelegates.clear();
        mCurrentTouchDelegate = null;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!mEnabled)
            return false;

        TouchDelegate delegate = null;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (final TouchDelegate touchDelegate : mTouchDelegates) {
                    if (touchDelegate.onTouchEvent(event)) {
                        mCurrentTouchDelegate = touchDelegate;
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                delegate = mCurrentTouchDelegate;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                delegate = mCurrentTouchDelegate;
                mCurrentTouchDelegate = null;
                break;
        }

        return delegate != null && delegate.onTouchEvent(event);
    }

    public void setEnabled(final boolean enabled) {
        mEnabled = enabled;
    }
}
