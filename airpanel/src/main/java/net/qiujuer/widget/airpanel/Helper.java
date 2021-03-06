package net.qiujuer.widget.airpanel;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author qiujuer Email:qiujuer@live.cn
 * @version 1.0.0
 */

final class Helper implements Contract.Helper {
    private Contract.Helper mTarget;
    private AirPanel.Listener mListenerTmp;
    private View mView;
    private AirAttribute mAttribute;

    Helper(View view, AirAttribute attribute) {
        this.mView = view;
        this.mAttribute = attribute;
    }

    @Override
    public void openPanel() {
        if (mTarget != null)
            mTarget.openPanel();
    }

    @Override
    public void closePanel() {
        if (mTarget != null)
            mTarget.closePanel();
    }

    @Override
    public boolean isOpen() {
        return mTarget != null && mTarget.isOpen();
    }

    @Override
    public void setPanelListener(AirPanel.Listener listener) {
        if (mTarget == null)
            this.mListenerTmp = listener;
        else mTarget.setPanelListener(listener);
    }

    @Override
    public void adjustPanelHeight(int heightMeasureSpec) {
        if (mTarget != null)
            mTarget.adjustPanelHeight(heightMeasureSpec);
    }

    @Override
    public void requestHideSoftKeyboard() {
        if (mTarget != null)
            mTarget.requestHideSoftKeyboard();
    }

    @Override
    public int calculateHeightMeasureSpec(int heightMeasureSpec) {
        return mTarget == null ? heightMeasureSpec : mTarget.calculateHeightMeasureSpec(heightMeasureSpec);
    }

    @Override
    public void setup(Activity activity) {
        if (mView.getId() == R.id.airPanelSubLayout) {
            mTarget = new Sub(mView, mAttribute);
        } else {
            View childPanel = mView.findViewById(R.id.airPanelSubLayout);
            if (childPanel == null) {
                throw new RuntimeException("AirPanel child can't null. You must set child id:airPanelLayout");
                //throw new RuntimeException(String.format("AirPanel child(%s) must implements: AirPanel and Contract", childPanel));
            }
            mTarget = new Boss((Contract.Panel) childPanel);
        }
        mTarget.setPanelListener(mListenerTmp);
        mTarget.setup(activity);

        // Clear it
        mListenerTmp = null;
        mView = null;
        mAttribute = null;
    }

    private static class Boss implements Contract.Helper {
        private final AtomicBoolean mShowPanelIntention = new AtomicBoolean(false);
        private final Rect mLastFrame = new Rect();
        private final Contract.Panel mSubPanel;
        private View mRootView;
        private AirPanel.Listener mListener;
        private int mDisplayHeight;

        private Boss(Contract.Panel subPanel) {
            this.mSubPanel = subPanel;
        }

        @Override
        public void openPanel() {
            if (isOpenSoftKeyboard()) {
                mShowPanelIntention.set(true);
                if (mListener != null)
                    mListener.requestHideSoftKeyboard();
            } else {
                mSubPanel.openPanel();
            }
        }

        @Override
        public void closePanel() {
            mSubPanel.closePanel();
        }

        @Override
        public boolean isOpen() {
            return mSubPanel.isOpen();
        }

        @Override
        public int calculateHeightMeasureSpec(int heightMeasureSpec) {
            // Only update frame values
            updateFrameSize();

            // Con't change it
            return heightMeasureSpec;
        }

        private void updateFrameSize() {
            if (mRootView != null) {
                Rect frame = new Rect();
                mRootView.getWindowVisibleDisplayFrame(frame);

                int bottomChangeSize = 0;
                if (mLastFrame.bottom > 0) {
                    bottomChangeSize = frame.bottom - mLastFrame.bottom;
                }

                mLastFrame.set(frame);
                Util.log("updateFrameSize frame:%s bottomChangeSize:%s", mLastFrame, bottomChangeSize);

                // In the end, we should check the soft keyboard next action
                checkSoftKeyboardAction(bottomChangeSize);
            }
        }

        private void checkSoftKeyboardAction(int bottomChangeSize) {
            if (bottomChangeSize > 0 && !isOpenSoftKeyboard()) {
                // Adjust SubPanelHeight
                mSubPanel.adjustPanelHeight(bottomChangeSize);

                // If want to show panel, we need call it
                if (mShowPanelIntention.getAndSet(false))
                    openPanel();
            } else if (bottomChangeSize < 0) {
                closePanel();
            }
        }

        @Override
        public void setup(Activity activity) {
            mRootView = activity.getWindow().getDecorView();
            /*
            mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Util.log("onGlobalLayout");
                }
            });
            */

            // Get DisplayHeight
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            mDisplayHeight = point.y;

            Util.log("setup mDisplayHeight:%s point:%s", mDisplayHeight, point.toString());
        }

        @Override
        public void adjustPanelHeight(int height) {
            // None do
        }

        private boolean isOpenSoftKeyboard() {
            return mLastFrame.bottom != 0 && mLastFrame.bottom != mDisplayHeight;
        }

        @Override
        public void requestHideSoftKeyboard() {
            if (mListener != null)
                mListener.requestHideSoftKeyboard();
        }

        @Override
        public void setPanelListener(AirPanel.Listener listener) {
            this.mListener = listener;
        }
    }

    private static class Sub implements Contract.Helper {
        private final View mView;
        private int mPanelHeight;
        private AirAttribute mAttribute;

        private Sub(View view, AirAttribute attribute) {
            mView = view;
            mAttribute = attribute;
            mPanelHeight = Util.getDefaultPanelHeight(mView.getContext(), mAttribute);
        }

        @Override
        public void openPanel() {
            mView.setVisibility(View.VISIBLE);
        }

        @Override
        public void closePanel() {
            mView.setVisibility(View.GONE);
        }

        @Override
        public boolean isOpen() {
            return mView.getVisibility() != View.GONE;
        }

        @Override
        public int calculateHeightMeasureSpec(int heightMeasureSpec) {
            int specMode = View.MeasureSpec.getMode(heightMeasureSpec);
            int specSize = View.MeasureSpec.getSize(heightMeasureSpec);

            if (mPanelHeight > 0) {
                int newSpecMode = View.MeasureSpec.EXACTLY;
                int newSpecSize = mPanelHeight;
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(newSpecSize, newSpecMode);
                Util.log("calculateHeightMeasureSpec:oldMode:%s, oldSize:%s newMode:%s, newSize:%s",
                        specMode, specSize, newSpecMode, newSpecSize);
            } else {
                Util.log("calculateHeightMeasureSpec:oldMode:%s, size:%s", specMode, specSize);
            }
            return heightMeasureSpec;
        }

        @Override
        public void setup(Activity activity) {
            // None do
        }

        @Override
        public void adjustPanelHeight(int height) {
            height = Math.min(height, mAttribute.panelMaxHeight);
            height = Math.max(height, mAttribute.panelMinHeight);
            if (height != mPanelHeight) {
                mPanelHeight = height;
                Util.updateLocalPanelHeight(mView.getContext(), height);
            }
        }

        @Override
        public void requestHideSoftKeyboard() {
            // None do
        }

        @Override
        public void setPanelListener(AirPanel.Listener listener) {
            // None do
        }
    }
}
