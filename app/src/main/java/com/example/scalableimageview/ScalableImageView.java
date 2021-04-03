package com.example.scalableimageview;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
//import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

public class ScalableImageView extends View {

	private final Paint mPaint = new Paint();
	//private final Paint mPointPaint = new Paint();

	private Bitmap mBitmap;
	private ScaleGestureDetector mScaleDetector;
	private GestureDetectorCompat mGestureDetector;
	private final OverScroller mOverScroller = new OverScroller(getContext());

	private int mImageWidth;
	private int mImageHeight;

	private float mScaleFactor = 1.f;
	private float mMinScale = 1.f;
	private float mMaxScale = 2.5f;

	private boolean mLoaded = false;
	private int mMaxViewWidth;
	private int mMaxViewHeight;

	//private float pointX;
	//private float pointY;
	public ScalableImageView(Context context) {
		super(context);
	}

	public ScalableImageView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		TypedArray array =  context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScalableImageView, 0,0);
		setImageResource(context, array.getResourceId(R.styleable.ScalableImageView_src, R.drawable.page));
		mMaxScale = array.getFloat(R.styleable.ScalableImageView_max_scale, mMaxScale);

		mScaleDetector = new ScaleGestureDetector(context, mScaleGestureListener);
		mGestureDetector = new GestureDetectorCompat(getContext(), new GestureListener());
		mImageWidth = mBitmap.getWidth();
		mImageHeight = mBitmap.getHeight();
		//mPointPaint.setColor(Color.BLUE);
	}

	public void setImageResource(Context context, int id){
		mBitmap = BitmapFactory.decodeResource(context.getResources(), id);
	}

	public void setImageBitmap(Bitmap bitmap){
		mBitmap = bitmap;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {


		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		if (!mLoaded) {
			mScaleFactor = (float) widthSize / (float) mImageWidth;
			mMinScale = mScaleFactor;
			mMaxScale = mMinScale * mMaxScale;
			mMaxViewWidth = widthSize;
			mMaxViewHeight = heightSize;
			mLoaded = true;
		}

		int desiredWidth = (int) (mImageWidth * mScaleFactor);
		int desiredHeight = (int) (mImageHeight * mScaleFactor);


		int width;
		int height;


		if (widthMode == MeasureSpec.EXACTLY)
			width = widthSize;

		else if (widthMode == MeasureSpec.AT_MOST)
			width = Math.min(desiredWidth, widthSize);

		else
			width = desiredWidth;


		if (heightMode == MeasureSpec.EXACTLY)
			height = heightSize;

		else if (heightMode == MeasureSpec.AT_MOST)
			height = Math.min(desiredHeight, heightSize);

		else
			height = desiredHeight;

		setMeasuredDimension(width, height);


	}

	@Override
	protected void onDraw(Canvas canvas) {

		if (mOverScroller.computeScrollOffset()) {
			scrollTo(mOverScroller.getCurrX(), mOverScroller.getCurrY());
		}
		canvas.scale(mScaleFactor, mScaleFactor);
		canvas.drawBitmap(mBitmap, 0, 0, mPaint);
		//canvas.scale(1 / mScaleFactor, 1 / mScaleFactor);
		//canvas.drawCircle(pointX * mScaleFactor, pointY * mScaleFactor, 35, mPointPaint);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		mScaleDetector.onTouchEvent(event);
		mGestureDetector.onTouchEvent(event);

		return true;
	}


	private void scaleTo(int fromX, int fromY, int toX, int toY, float scale) {
		@SuppressLint("Recycle") ValueAnimator animator = ValueAnimator.ofFloat(mScaleFactor, scale);
		animator.setDuration(300);
		int diffX = toX - fromX;
		int diffY = toY - fromY;
		animator.addUpdateListener(animation -> {
			scrollTo((int) (fromX + diffX * animation.getAnimatedFraction()),
					(int) (fromY + diffY * animation.getAnimatedFraction()));
			mScaleFactor = (float) animation.getAnimatedValue();
			adjust();
		});
		animator.start();

	}

	private void adjust() {
		getLayoutParams().width = (int) Math.min(mMaxViewWidth, mImageWidth * mScaleFactor);
		getLayoutParams().height = (int) Math.min(mMaxViewHeight, mImageHeight * mScaleFactor);
		setLayoutParams(getLayoutParams());
		scrollTo((int) Math.min(Math.max(0, getScrollX()), mImageWidth * mScaleFactor - getWidth()),
				(int) Math.min(Math.max(0, getScrollY()), Math.max(0, mImageHeight * mScaleFactor - getHeight())));
		invalidate();
	}

	private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
			= new ScaleGestureDetector.SimpleOnScaleGestureListener() {
		float startX;
		float startY;
		float startCenterX;
		float startCenterY;
		float startScaleFactor;

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			startX = (getScrollX()) / mScaleFactor;
			startY = (getScrollY()) / mScaleFactor;
			startCenterX = (getScrollX() + detector.getFocusX()) / mScaleFactor;
			startCenterY = (getScrollY() + detector.getFocusY()) / mScaleFactor;
			startScaleFactor = mScaleFactor;
			//pointX = startCenterX;
			//pointY = startCenterY;
			//Log.e("startY", String.valueOf(startY));
			//Log.e("startCenterY", String.valueOf(startCenterY));
			//Log.e("detector.getFocusY()", String.valueOf(detector.getFocusY()));

			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();
			mScaleFactor = Math.max(mMinScale, Math.min(mScaleFactor, mMaxScale));
			//TODO Improve Scaling
			float curScaleChanges = (mScaleFactor - startScaleFactor) / (mMaxScale );
			//Log.e("curScaleChanges", String.valueOf(curScaleChanges));
			float curX = startX * (1 - curScaleChanges) + startCenterX * curScaleChanges;
			float curY = startY * (1 - curScaleChanges) + startCenterY * curScaleChanges;
			//Log.e("curY", String.valueOf(curY));
			scrollTo((int) (curX * mScaleFactor),
					(int) (curY * mScaleFactor));
			adjust();
			return true;
		}
	};

	private class GestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

		@Override
		public boolean onDown(MotionEvent e) {
			if (!mOverScroller.isFinished())
				mOverScroller.forceFinished(true);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			scrollBy((int) Math.min(mImageWidth * mScaleFactor - getWidth() - getScrollX(), Math.max(-getScrollX(), distanceX)),
					(int) Math.min(Math.max(0, mImageHeight * mScaleFactor - getHeight() - getScrollY()), Math.max(-getScrollY(), distanceY))
			);
			invalidate();
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			int max_width = (int) (mImageWidth * mScaleFactor - getWidth());
			int max_height = (int) (int) (mImageHeight * mScaleFactor - getHeight());
			mOverScroller.fling(getScrollX(), getScrollY(),
					(int) (-velocityX), (int) (-velocityY),
					0, max_width, 0, max_height);
			postInvalidate();
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			float x = (getScrollX() + event.getRawX() - getWidth() / 2.f) / mScaleFactor;
			float y = (getScrollY() + event.getRawY() - getHeight() / 2.f) / mScaleFactor;
			float finalScale;
			if (mScaleFactor == mMinScale) {
				finalScale = mMaxScale ;
				x *= finalScale;
				y *= finalScale;
				x += getWidth() / 2.f * finalScale;
				y += getHeight() / 2.f * finalScale;
			} else {
				finalScale = mMinScale;
				x -= event.getRawX() / mScaleFactor;
				y -= event.getRawY() / mScaleFactor;
				x *= finalScale;
				y *= finalScale;
				x -= getWidth() / 2.f * finalScale;
				y -= getHeight() / 2.f * finalScale;
			}
			scaleTo(getScrollX(), getScrollY(), (int) x, (int) y, finalScale);
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}


		@Override
		public void onLongPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}
	}

}
