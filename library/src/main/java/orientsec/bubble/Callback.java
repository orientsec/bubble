package orientsec.bubble;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base class for {@link BubbleLayout} callbacks.
 *
 * @see BubbleLayout#addCallback(Callback)
 */
public class Callback {
    /**
     * Indicates that the Bubble was dismissed via a swipe.
     */
    public static final int DISMISS_EVENT_SWIPE = 0;
    /**
     * Indicates that the Bubble was dismissed via an action click.
     */
    public static final int DISMISS_EVENT_ACTION = 1;
    /**
     * Indicates that the Bubble was dismissed via a timeout.
     */
    public static final int DISMISS_EVENT_TIMEOUT = 2;
    /**
     * Indicates that the Bubble was dismissed via a call to {@link BubbleLayout#dismiss()}.
     */
    public static final int DISMISS_EVENT_MANUAL = 3;
    /**
     * Indicates that the Bubble was dismissed from a new Bubble being shown.
     */
    public static final int DISMISS_EVENT_CONSECUTIVE = 4;

    @IntDef({DISMISS_EVENT_SWIPE, DISMISS_EVENT_ACTION, DISMISS_EVENT_TIMEOUT,
            DISMISS_EVENT_MANUAL, DISMISS_EVENT_CONSECUTIVE})
    @Retention(RetentionPolicy.SOURCE)
    @interface DismissEvent {
    }

    /**
     * Called when the given {@link BubbleLayout} has been dismissed, either
     * through a time-out, having been manually dismissed, or an action being clicked.
     *
     * @param event The event which caused the dismissal. One of either:
     *              {@link #DISMISS_EVENT_SWIPE}, {@link #DISMISS_EVENT_ACTION},
     *              {@link #DISMISS_EVENT_TIMEOUT}, {@link #DISMISS_EVENT_MANUAL} or
     *              {@link #DISMISS_EVENT_CONSECUTIVE}.
     * @see BubbleLayout#dismiss()
     */
    public void onDismissed(@DismissEvent int event) {
        // empty
    }

    /**
     * Called when the given {@link BubbleLayout} is visible.
     *
     * @see Bubble#show()
     */
    public void onShown() {
        // empty
    }
}
