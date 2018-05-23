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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Manages {@link Bubble}s.
 */
class BubbleManager {

    static final int MSG_TIMEOUT = 0;

    private static final int SHORT_DURATION_MS = 1500;
    private static final int LONG_DURATION_MS = 2750;

    private static BubbleManager sBubbleManager;

    static BubbleManager getInstance() {
        if (sBubbleManager == null) {
            sBubbleManager = new BubbleManager();
        }
        return sBubbleManager;
    }

    private final Object mLock;
    private final Handler mHandler;

    private BubbleRecord mCurrentBubble;
    private BubbleRecord mNextBubble;

    private BubbleManager() {
        mLock = new Object();
        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case MSG_TIMEOUT:
                        handleTimeout((BubbleRecord) message.obj);
                        return true;
                }
                return false;
            }
        });
    }

    interface Callback {
        void show();

        void dismiss(int event);
    }

    public void show(int duration, Callback callback) {
        synchronized (mLock) {
            if (isCurrentBubbleLocked(callback)) {
                // Means that the callback is already in the queue. We'll just update the duration
                mCurrentBubble.duration = duration;

                // If this is the Bubble currently being shown, call re-schedule it's
                // timeout
                mHandler.removeCallbacksAndMessages(mCurrentBubble);
                scheduleTimeoutLocked(mCurrentBubble);
                return;
            } else if (isNextBubbleLocked(callback)) {
                // We'll just update the duration
                mNextBubble.duration = duration;
            } else {
                // Else, we need to create a new record and queue it
                mNextBubble = new BubbleRecord(duration, callback);
            }

            if (mCurrentBubble != null && cancelBubbleLocked(mCurrentBubble,
                    orientsec.bubble.Callback.DISMISS_EVENT_CONSECUTIVE)) {
                // If we currently have a Bubble, try and cancel it and wait in line
                return;
            } else {
                // Clear out the current Bubble
                mCurrentBubble = null;
                // Otherwise, just show it now
                showNextBubbleLocked();
            }
        }
    }

    public void dismiss(Callback callback, int event) {
        synchronized (mLock) {
            if (isCurrentBubbleLocked(callback)) {
                cancelBubbleLocked(mCurrentBubble, event);
            } else if (isNextBubbleLocked(callback)) {
                cancelBubbleLocked(mNextBubble, event);
            }
        }
    }

    /**
     * Should be called when a Bubble is no longer displayed. This is after any exit
     * animation has finished.
     */
    public void onDismissed(Callback callback) {
        synchronized (mLock) {
            if (isCurrentBubbleLocked(callback)) {
                // If the callback is from a Bubble currently show, remove it and show a new one
                mCurrentBubble = null;
                if (mNextBubble != null) {
                    showNextBubbleLocked();
                }
            }
        }
    }

    /**
     * Should be called when a Bubble is being shown. This is after any entrance animation has
     * finished.
     */
    public void onShown(Callback callback) {
        synchronized (mLock) {
            if (isCurrentBubbleLocked(callback)) {
                scheduleTimeoutLocked(mCurrentBubble);
            }
        }
    }

    public void pauseTimeout(Callback callback) {
        synchronized (mLock) {
            if (isCurrentBubbleLocked(callback) && !mCurrentBubble.paused) {
                mCurrentBubble.paused = true;
                mHandler.removeCallbacksAndMessages(mCurrentBubble);
            }
        }
    }

    public void restoreTimeoutIfPaused(Callback callback) {
        synchronized (mLock) {
            if (isCurrentBubbleLocked(callback) && mCurrentBubble.paused) {
                mCurrentBubble.paused = false;
                scheduleTimeoutLocked(mCurrentBubble);
            }
        }
    }

    public boolean isCurrent(Callback callback) {
        synchronized (mLock) {
            return isCurrentBubbleLocked(callback);
        }
    }

    public boolean isCurrentOrNext(Callback callback) {
        synchronized (mLock) {
            return isCurrentBubbleLocked(callback) || isNextBubbleLocked(callback);
        }
    }

    private static class BubbleRecord {
        final WeakReference<Callback> callback;
        int duration;
        boolean paused;

        BubbleRecord(int duration, Callback callback) {
            this.callback = new WeakReference<>(callback);
            this.duration = duration;
        }

        boolean isBubble(Callback callback) {
            return callback != null && this.callback.get() == callback;
        }
    }

    private void showNextBubbleLocked() {
        if (mNextBubble != null) {
            mCurrentBubble = mNextBubble;
            mNextBubble = null;

            final Callback callback = mCurrentBubble.callback.get();
            if (callback != null) {
                callback.show();
            } else {
                // The callback doesn't exist any more, clear out the Bubble
                mCurrentBubble = null;
            }
        }
    }

    private boolean cancelBubbleLocked(BubbleRecord record, int event) {
        final Callback callback = record.callback.get();
        if (callback != null) {
            // Make sure we remove any timeouts for the BubbleRecord
            mHandler.removeCallbacksAndMessages(record);
            callback.dismiss(event);
            return true;
        }
        return false;
    }

    private boolean isCurrentBubbleLocked(Callback callback) {
        return mCurrentBubble != null && mCurrentBubble.isBubble(callback);
    }

    private boolean isNextBubbleLocked(Callback callback) {
        return mNextBubble != null && mNextBubble.isBubble(callback);
    }

    private void scheduleTimeoutLocked(BubbleRecord r) {
        if (r.duration == Constant.LENGTH_INDEFINITE) {
            // If we're set to indefinite, we don't want to set a timeout
            return;
        }

        int durationMs = LONG_DURATION_MS;
        if (r.duration > 0) {
            durationMs = r.duration;
        } else if (r.duration == Constant.LENGTH_SHORT) {
            durationMs = SHORT_DURATION_MS;
        }
        mHandler.removeCallbacksAndMessages(r);
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_TIMEOUT, r), durationMs);
    }

    void handleTimeout(BubbleRecord record) {
        synchronized (mLock) {
            if (mCurrentBubble == record || mNextBubble == record) {
                cancelBubbleLocked(record, orientsec.bubble.Callback.DISMISS_EVENT_TIMEOUT);
            }
        }
    }

}
