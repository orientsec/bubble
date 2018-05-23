/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package orientsec.bubble;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import orientsec.bubble.library.R;


/**
 * Snackbars provide lightweight feedback about an operation. They show a brief message at the
 * bottom of the screen on mobile and lower left on larger devices. Snackbars appear above all other
 * elements on screen and only one can be displayed at a time.
 * <p>
 * They automatically disappear after a timeout or after user interaction elsewhere on the screen,
 * particularly after interactions that summon a new surface or activity. Snackbars can be swiped
 * off screen.
 * <p>
 * Snackbars can contain an action which is set via
 * {@link #setAction(View.OnClickListener)}.
 * <p>
 * To be notified when a snackbar has been shown or dismissed, you can provide a {@link Callback}
 * via {@link BubbleLayout#addCallback(orientsec.bubble.Callback)}.</p>
 */
public final class Bubble {

    private final BubbleLayout mView;
    private final ViewGroup parent;

    @Nullable
    private Callback mCallback;

    private Bubble(ViewGroup parent, BubbleLayout bubbleLayout) {
        this.parent = parent;
        mView = bubbleLayout;
    }

    /**
     * Make a Bubble to display a message
     * <p>
     * <p>Bubble will try and find a parent view to hold Bubble's view from the value given
     * to {@code view}. Bubble will walk up the view tree trying to find a suitable parent,
     * which is defined as a {@link CoordinatorLayout} or the window decor's content view,
     * whichever comes first.
     *
     * @param activity The Activity to show this bubble.
     * @param duration How long to display the message.  Either {@link Constant#LENGTH_SHORT} or {@link
     *                 Constant#LENGTH_LONG}
     */
    @NonNull
    public static Bubble make(@NonNull Activity activity,
                              @Constant.Duration int duration) {
        final ViewGroup parent = findRootView(activity);
        if (parent == null) {
            throw new IllegalArgumentException("No suitable parent found from the given view. "
                    + "Please provide a valid view.");
        }

        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final BubbleLayout bubbleLayout =
                (BubbleLayout) inflater.inflate(R.layout.bubble_layout_content, parent, false);
        final Bubble bubble = new Bubble(parent, bubbleLayout);
        bubbleLayout.findViewById(R.id.btn_close).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bubbleLayout.dismiss();
                    }
                }
        );
        bubbleLayout.setDuration(duration);
        return bubble;
    }

    private static ViewGroup findRootView(Activity activity) {
        ViewGroup fallback = null;
        Window window = activity.getWindow();
        if (window != null) {
            View view = window.getDecorView();
            if (view instanceof FrameLayout) {
                fallback = (ViewGroup) view;
            }
        }
        // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
        return fallback;
    }

    /**
     * Update the title in this {@link Bubble}.
     *
     * @param message The new title for this {@link Bubble}.
     */
    @NonNull
    public Bubble setTitle(@NonNull CharSequence message) {
        final TextView tv = mView.findViewById(R.id.tv_title);
        tv.setText(message);
        return this;
    }

    /**
     * Update the title in this {@link Bubble}.
     *
     * @param resId The new title for this {@link Bubble}.
     */
    @NonNull
    public Bubble setTitle(@StringRes int resId) {
        return setTitle(mView.getContext().getText(resId));
    }

    /**
     * Update the content in this {@link Bubble}.
     *
     * @param message The new content for this {@link Bubble}.
     */
    @NonNull
    public Bubble setContent(@NonNull CharSequence message) {
        final TextView tv = mView.findViewById(R.id.tv_content);
        tv.setText(message);
        return this;
    }

    /**
     * Update the content in this {@link Bubble}.
     *
     * @param resId The new content for this {@link Bubble}.
     */
    @NonNull
    public Bubble setContent(@StringRes int resId) {
        return setContent(mView.getContext().getText(resId));
    }

    /**
     * Update the content in this {@link Bubble}.
     *
     * @param drawable The new content for this {@link Bubble}.
     */
    @NonNull
    public Bubble setIcon(@NonNull Drawable drawable) {
        final ImageView iv = mView.findViewById(R.id.iv_icon);
        iv.setImageDrawable(drawable);
        return this;
    }

    /**
     * Update the content in this {@link Bubble}.
     *
     * @param resId The new content for this {@link Bubble}.
     */
    @NonNull
    public Bubble setIcon(@DrawableRes int resId) {
        final ImageView iv = mView.findViewById(R.id.iv_icon);
        iv.setImageResource(resId);
        return this;
    }


    /**
     * Set the action to be displayed in this {@link Bubble}.
     *
     * @param listener callback to be invoked when the action is clicked
     */
    @NonNull
    public Bubble setAction(final View.OnClickListener listener) {
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                // Now dismiss the Bubble
                mView.dispatchDismiss(Callback.DISMISS_EVENT_ACTION);
            }
        });
        return this;
    }

    /**
     * Set a callback to be called when this the visibility of this {@link Bubble}
     * changes. Note that this method is deprecated
     * and you should use {@link BubbleLayout#addCallback(Callback)} to add a callback and
     * {@link BubbleLayout#removeCallback(Callback)} to remove a registered callback.
     *
     * @param callback Callback to notify when transient bottom bar events occur.
     * @see Callback
     * @see BubbleLayout#addCallback(Callback)
     * @see BubbleLayout#removeCallback(Callback)
     * @deprecated Use {@link BubbleLayout#addCallback(Callback)}
     */
    @Deprecated
    @NonNull
    public Bubble setCallback(Callback callback) {
        // The logic in this method emulates what we had before support for multiple
        // registered callbacks.
        if (mCallback != null) {
            mView.removeCallback(mCallback);
        }
        if (callback != null) {
            mView.addCallback(callback);
        }
        // Update the deprecated field so that we can remove the passed callback the next
        // time we're called
        mCallback = callback;
        return this;
    }

    /**
     * Show the {@link BubbleLayout}.
     */
    public void show() {
        mView.show(parent);
    }

    /**
     * Dismiss the {@link BubbleLayout}.
     */
    public void dismiss() {
        mView.dismiss();
    }
}

