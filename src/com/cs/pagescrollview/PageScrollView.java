package com.cs.pagescrollview;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * 分页滑动的ScrollView
 * @author chenshi
 *
 */
public class PageScrollView extends ScrollView{
    private static final String TAG = PageScrollView.class.getSimpleName();
    private static final boolean ENABLE_LEFT_RIGHT_SLOP = false;
    private static final int PAGING_TOUCH_SLOP = 96;
    private final int mPagingTouchSlop;
    private boolean mDisallowInterceptTouch;
    private GestureDetector mDetector;
    private Paint mPaint;
    private float mTrackWidth, mThumbWidth, mRadius;
    private int mThumbColor, mTrackColor;
    private final Rect mTempRect = new Rect();
    private boolean mFixLastPageHeight;
    /**
     * 滚动条最小高度
     */
    private final static int M_SCROLL_MIN_HEIGHT = 20;

    public PageScrollView(Context context) {
        this(context, null);
    }
    
    public PageScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.scrollViewStyle);
    }
    
    public PageScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.ScrollViewStyle);
    }
    
    @SuppressLint("NewApi")
    public PageScrollView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPagingTouchSlop = PAGING_TOUCH_SLOP;
        mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                    float velocityX, float velocityY) {
                if (checkStartScroll(e1, e2, velocityX, velocityY))
                    return true;
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });
        init(context, attrs, defStyleAttr,  defStyleRes);
    }
    
    private boolean canScroll() {
        View child = getChildAt(0);
        if (child != null) {
            int childHeight = child.getHeight();
            return getHeight() < childHeight + getPaddingTop() + getPaddingBottom();
        }
        return false;
    }
    
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PageScrollView, defStyleAttr, defStyleRes);
        mRadius = a.getDimension(R.styleable.PageScrollView_radius, 0);
        mThumbColor = a.getColor(R.styleable.PageScrollView_thumbColor, Color.BLACK);
        mTrackColor = a.getColor(R.styleable.PageScrollView_trackColor, Color.WHITE);
        mTrackWidth = a.getDimension(R.styleable.PageScrollView_trackWidth, 1);
        mThumbWidth = a.getDimension(R.styleable.PageScrollView_thumbWidth, 0);
        mFixLastPageHeight = a.getBoolean(R.styleable.PageScrollView_fixLastPageHeight, true);
        a.recycle();
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollbarFadingEnabled(false);
        setAnimationCacheEnabled(false);
        setSmoothScrollingEnabled(false);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mDetector.onTouchEvent(ev);
        return true;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canScroll()) {
            drawScrollBars(canvas);
        }
    }
    
    private void drawScrollBars(Canvas canvas) {
        mPaint.setStyle(Style.FILL);
        mPaint.setColor(mTrackColor);
        if (mThumbWidth <= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mThumbWidth = getScrollBarSize();
            }else {
                mThumbWidth = mTrackWidth;
            }
        }
        float left = getWidth() - getPaddingRight() - mThumbWidth / 2 - mTrackWidth / 2;
        float right = left + mTrackWidth / 2;
        canvas.drawRect(left, getPaddingTop(), right, getTotalHeight() - getPaddingBottom(), mPaint);
        mPaint.setColor(mThumbColor);
        canvas.drawRoundRect(getProgressRect(mThumbWidth), mRadius, mRadius, mPaint);
    }
    
    private float getOneCutWidth() {
        return (getHeight() - getPaddingTop() - getPaddingBottom()) * 1.0f / getTotalPage();
    }

    private RectF getProgressRect(float thumbSize) {
        float oneCutWidth = getOneCutWidth();
        float left = getWidth()- getPaddingRight() - thumbSize;
        float right = left + thumbSize;
        float top =  oneCutWidth * (getCurPage() - 1) + getScrollY();
        float bottom = top + (oneCutWidth < M_SCROLL_MIN_HEIGHT ? M_SCROLL_MIN_HEIGHT : oneCutWidth);
        return new RectF(left, top, right, bottom);
    }

    private boolean checkStartScroll(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean startScroll = false;
        float slope = 0;
        final float dx = e2.getX() - e1.getX();
        final float dy = e2.getY() - e1.getY();
        if (Math.abs(dx) > mPagingTouchSlop) {
            startScroll = ENABLE_LEFT_RIGHT_SLOP;
        }
        if (Math.abs(dy) > mPagingTouchSlop) {
            startScroll = true;
        }
        if (startScroll) {
            disallowParentAndChildInterceptTouchEvent(true);
        }
        if (startScroll) {
            if (dx != 0) {
                slope = Math.abs(dy / dx);
            } else {
                slope = 2; //一直以dy做主导
            }
            if (slope >= 1) {
                if (dy < 0) {
                    nextPage();
                }else {
                    prePage();
                }
            }else {
                if (ENABLE_LEFT_RIGHT_SLOP) {
                    if (dx < 0) {
                        nextPage();
                    }else {
                        prePage();
                    }
                }
            }
        }
        Log.d(TAG, String.format("startScroll:%b,slope:%s,dy:%s,dx:%s", startScroll, slope, dy, dx));
        return startScroll;
    }
    
    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     *                 if topFocus is true, or at the bottom of the bounds if topFocus is
     *                 false
     * @param top      the top offset of the bounds in which a focusable must be
     *                 found
     * @param bottom   the bottom offset of the bounds in which a focusable must
     *                 be found
     * @return the next focusable component in the bounds or null if none can
     *         be found
     */
    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = (top < viewTop) &&
                        (viewBottom < bottom);

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            (topFocus && viewTop < focusCandidate.getTop()) ||
                                    (!topFocus && viewBottom > focusCandidate
                                            .getBottom());

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }
    
    /**
     * <p>Scrolls the view to make the area defined by <code>top</code> and
     * <code>bottom</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this ScrollView.</p>
     *
     * @param direction the scroll direction: {@link android.view.View#FOCUS_UP}
     *                  to go upward, {@link android.view.View#FOCUS_DOWN} to downward
     * @param top       the top offset of the new area to be made visible
     * @param bottom    the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == View.FOCUS_UP;

        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? (top - containerTop) : (bottom - containerBottom);
            doScrollY(delta);
        }

        if (newFocused != findFocus()) newFocused.requestFocus(direction);

        return handled;
    }
    
    /**
     * Smooth scroll by a Y delta
     *
     * @param delta the number of pixels to scroll by on the Y axis
     */
    private void doScrollY(int delta) {
        if (delta != 0) {
            if (isSmoothScrollingEnabled()) {
                smoothScrollBy(0, delta);
            } else {
                scrollBy(0, delta);
            }
        }
    }
    
    public boolean prePage() {
        if (canScroll()) {
            Log.d(TAG, String.format("prePage cur:%d,total:%d", getCurPage(), getTotalPage()));
            return pageScroll(View.FOCUS_UP);
        }else {
            Log.d(TAG, "can not scroll");
            return false;
        }
    }

    public boolean nextPage() {
        if (canScroll()) {
            Log.d(TAG, String.format("nextPage cur:%d,total:%d", getCurPage(), getTotalPage()));
            return pageScroll(View.FOCUS_DOWN);
        }else {
            Log.d(TAG, "can not scroll");
            return false;
        }
    }
    
    public boolean moveToPage(int page) {
        if (canScroll() && page <= getTotalPage()) {
            if (page == getCurPage())
                return false;
            boolean down = page > getCurPage();
            int height = getHeight() * (page - getCurPage());
            if (down) {
                mTempRect.top = getScrollY() + height;
                int count = getChildCount();
                if (count > 0) {
                    View view = getChildAt(count - 1);
                    if (mTempRect.top + height > view.getBottom()) {
                        mTempRect.top = view.getBottom() - height;
                    }
                }
            } else {
                mTempRect.top = getScrollY() - height;
                if (mTempRect.top < 0) {
                    mTempRect.top = 0;
                }
            }
            mTempRect.bottom = mTempRect.top + height;
            Log.d(TAG, String.format("nextPage cur:%d,total:%d", getCurPage(), getTotalPage()));
            return scrollAndFocus(down ? View.FOCUS_DOWN : View.FOCUS_UP, mTempRect.top, mTempRect.bottom);
        }else {
            Log.d(TAG, "can not scroll");
            return false;
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mFixLastPageHeight) {
            final View child = getChildAt(0);
            if (child.getVisibility() != GONE) {
                final int height = child.getMeasuredHeight();
                int total = getPage(height, getMeasuredHeight());
                child.layout(left, top, right, top + getMeasuredHeight() * total);
            }
        }
    }
    
    
    public boolean isFixLastPageHeight() {
        return mFixLastPageHeight;
    }

    public void setFixLastPageHeight(boolean fixLastPageHeight) {
        this.mFixLastPageHeight = fixLastPageHeight;
        requestLayout();
    }

    public int getTotalHeight() {
        View child = getChildAt(0);
        if (child != null) {
            return child.getHeight();
        }
        return 0;
    }
    
    public int getTotalPage() {
        return getPage(getTotalHeight(), getHeight());
    }
    
    public int getCurPage() {
        return getPage(getScrollY() + getHeight(), getHeight());
    }
    
    private final int getPage(int height, int pageHeight) {
        return height % pageHeight > 0 ? height / pageHeight + 1 : height / pageHeight;
    }
   
    
    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    public void scrollToChild(View child) {
        Rect tempRect = new Rect();
        child.getDrawingRect(tempRect);
        // Offset from child's local coordinates to ScrollView coordinates 
        offsetDescendantRectToMyCoords(child, tempRect);
        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(tempRect);
        if (scrollDelta != 0) {
            scrollBy(0, scrollDelta);
        }
    }

    private void disallowParentAndChildInterceptTouchEvent(boolean disabled) {
        if (mDisallowInterceptTouch != disabled) {
            mDisallowInterceptTouch = disabled;
            requestChildDisallowInterceptTouchEvent(disabled);
            getParent().requestDisallowInterceptTouchEvent(disabled);
        }
    }

    private void requestChildDisallowInterceptTouchEvent(boolean disabled) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null && child instanceof ViewGroup){
                ((ViewGroup)child).requestDisallowInterceptTouchEvent(disabled);
            }
        }
    }
}
