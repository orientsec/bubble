package orientsec.bubble;

import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.animation.Interpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Constant {
    static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();

    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})
    @IntRange(from = 1)
    @Retention(RetentionPolicy.SOURCE)
    @interface Duration {
    }

    /**
     * Show the Bubble indefinitely. This means that the Bubble will be displayed from the time
     * that is {@link Bubble#show() shown} until either it is dismissed, or another Bubble is shown.
     *
     * @see BubbleLayout#setDuration
     */
    public static final int LENGTH_INDEFINITE = -2;

    /**
     * Show the Bubble for a short period of time.
     *
     * @see BubbleLayout#setDuration
     */
    public static final int LENGTH_SHORT = -1;

    /**
     * Show the Bubble for a long period of time.
     *
     * @see BubbleLayout#setDuration
     */
    public static final int LENGTH_LONG = 0;
}
