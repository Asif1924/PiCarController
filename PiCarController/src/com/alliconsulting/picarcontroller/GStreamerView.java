package com.alliconsulting.picarcontroller;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GStreamerView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String LOG_TAG = "PiCarControllerActivity::GStreamerView";

	public final static int POSITION_UPPER_LEFT = 9;
	public final static int POSITION_UPPER_RIGHT = 3;
	public final static int POSITION_LOWER_LEFT = 12;
	public final static int POSITION_LOWER_RIGHT = 6;

	public final static int SIZE_STANDARD = 1;
	public final static int SIZE_BEST_FIT = 4;
	public final static int SIZE_FULLSCREEN = 8;

	SurfaceHolder holder;
	Context saved_context;

	private GStreamViewThread thread;
	private GStreamerInputStream mIn = null;
	private boolean showFps = false;
	private boolean mRun = false;
	private boolean surfaceDone = false;

	private boolean showDeviceResolution = false;
	private int deviceWidth = 0;
	private int deviceHeight = 0;

	private Paint overlayPaint;
	private int overlayTextColor;
	private int overlayBackgroundColor;
	private int ovlPos;
	private int resOverlayPos;
	private int dispWidth;
	private int dispHeight;
	private int displayMode;

	private boolean suspending = false;

	private Bitmap bmp = null;
	// hard-coded image size
	private static int imgWidth = 320;
	private static int imgHeight = 240;

	public class GStreamViewThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private int frameCounter = 0;
		private long start;
		private String fps = "";
		private String resolutionString = "";

		public GStreamViewThread(SurfaceHolder surfaceHolder, Context context) {
			String methodName = "GStreamViewThread";
			Log.i(LOG_TAG + ": GStreamViewThread", methodName);

			mSurfaceHolder = surfaceHolder;
		}

		private Rect destRect(int bmw, int bmh) {
			String methodName = "destRect";
			Log.i(LOG_TAG + ": GStreamViewThread", methodName);

			int tempx;
			int tempy;
			if (displayMode == GStreamerView.SIZE_STANDARD) {
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == GStreamerView.SIZE_BEST_FIT) {
				float bmasp = (float) bmw / (float) bmh;
				bmw = dispWidth;
				bmh = (int) (dispWidth / bmasp);
				if (bmh > dispHeight) {
					bmh = dispHeight;
					bmw = (int) (dispHeight * bmasp);
				}
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == GStreamerView.SIZE_FULLSCREEN)
				return new Rect(0, 0, dispWidth, dispHeight);
			return null;
		}

		public void setSurfaceSize(int width, int height) {
			String methodName = "setSurfaceSize";
			Log.i(LOG_TAG + ": GStreamViewThread", methodName);

			synchronized (mSurfaceHolder) {
				dispWidth = width;
				dispHeight = height;
			}
		}

		private Bitmap makeFpsOverlay(Paint p) {
			String methodName = "makeFpsOverlay";
			Log.i(LOG_TAG + "; GStreamViewThread", methodName);

			Rect b = new Rect();
			p.getTextBounds(fps, 0, fps.length(), b);

			// false indentation to fix forum layout
			Bitmap bm = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);

			Canvas c = new Canvas(bm);
			p.setColor(overlayBackgroundColor);
			c.drawRect(0, 0, b.width(), b.height(), p);
			p.setColor(overlayTextColor);
			c.drawText(fps, -b.left, b.bottom - b.top - p.descent(), p);
			return bm;
		}

		private Bitmap makeDeviceResolutionOverlay(Paint paintArea) {
			String methodName = "makeDeviceResolutionOverlay";
			Log.i(LOG_TAG + ": GStreamViewThread", methodName);

			Rect rectBoundingDeviceResolutionString = new Rect();
			paintArea.getTextBounds(resolutionString, 0, resolutionString.length(), rectBoundingDeviceResolutionString);

			// false indentation to fix forum layout
			Bitmap deviceResBitmap = Bitmap.createBitmap(rectBoundingDeviceResolutionString.width(),
					rectBoundingDeviceResolutionString.height(), Bitmap.Config.ARGB_8888);

			Canvas drawingCanvas = new Canvas(deviceResBitmap);
			paintArea.setColor(overlayBackgroundColor);
			drawingCanvas.drawRect(0, 0, rectBoundingDeviceResolutionString.width(),
					rectBoundingDeviceResolutionString.height(), paintArea);
			paintArea.setColor(overlayTextColor);
			drawingCanvas.drawText(
					resolutionString,
					-rectBoundingDeviceResolutionString.left,
					rectBoundingDeviceResolutionString.bottom - rectBoundingDeviceResolutionString.top
							- paintArea.descent(), paintArea);
			return deviceResBitmap;
		}

		public void run() {
			String methodName = "run";
			Log.i(LOG_TAG + ": GStreamViewThread", methodName);

			start = System.currentTimeMillis();
			PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);

			int width;
			int height;
			Paint paintInfo = new Paint();
			Bitmap ovl = null;
			Bitmap resOverlay = null;

			while (mRun) {

				Rect destRect = null;
				Canvas drawcallsCanvas = null;

				if (surfaceDone) {
					try {
						if (bmp == null) {
							bmp = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
						}
						mIn.readMjpegFrame(bmp);

						destRect = destRect(bmp.getWidth(), bmp.getHeight());

						drawcallsCanvas = mSurfaceHolder.lockCanvas();
						synchronized (mSurfaceHolder) {

							drawcallsCanvas.drawBitmap(bmp, null, destRect, paintInfo);

							if (showFps) {
								paintInfo.setXfermode(mode);
								if (ovl != null) {

									// false indentation to fix forum layout
									height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom - ovl.getHeight();
									width = ((ovlPos & 8) == 8) ? destRect.left : destRect.right - ovl.getWidth();

									drawcallsCanvas.drawBitmap(ovl, width, height, null);
								}
								paintInfo.setXfermode(null);
								frameCounter++;
								if ((System.currentTimeMillis() - start) >= 1000) {
									fps = String.valueOf(frameCounter) + "fps";
									frameCounter = 0;
									start = System.currentTimeMillis();
									if (ovl != null)
										ovl.recycle();

									ovl = makeFpsOverlay(overlayPaint);
								}
							}

							if (showDeviceResolution) {
								paintInfo.setXfermode(mode);
								if (resOverlay != null) {

									// false indentation to fix forum layout
									height = ((resOverlayPos & 1) == 1) ? destRect.top : destRect.bottom
											- resOverlay.getHeight();
									width = ((resOverlayPos & 8) == 8) ? destRect.left : destRect.right
											- resOverlay.getWidth();

									drawcallsCanvas.drawBitmap(resOverlay, width, height, null);
								}
								paintInfo.setXfermode(null);
								// frameCounter++;
								// if((System.currentTimeMillis() - start) >=
								// 1000) {
								resolutionString = "Device Resolution: " + deviceWidth + "x" + deviceHeight
										+ " pixels.";
								// frameCounter = 0;
								// start = System.currentTimeMillis();
								if (resOverlay != null)
									resOverlay.recycle();

								resOverlay = makeDeviceResolutionOverlay(overlayPaint);
								// }
							}

						}

					} catch (IOException e) {

					} finally {
						if (drawcallsCanvas != null)
							mSurfaceHolder.unlockCanvasAndPost(drawcallsCanvas);
					}
				}
			}
		}
	}

	private void init(Context context) {
		String methodName = "init";
		Log.i(LOG_TAG + "", methodName);

		// SurfaceHolder holder = getHolder();
		holder = getHolder();
		saved_context = context;
		holder.addCallback(this);
		thread = new GStreamViewThread(holder, context);
		setFocusable(true);
		overlayPaint = new Paint();
		overlayPaint.setTextAlign(Paint.Align.LEFT);
		overlayPaint.setTextSize(12);
		overlayPaint.setTypeface(Typeface.DEFAULT);
		overlayTextColor = Color.WHITE;
		overlayBackgroundColor = Color.BLACK;
		ovlPos = GStreamerView.POSITION_LOWER_RIGHT;
		resOverlayPos = GStreamerView.POSITION_UPPER_RIGHT;
		displayMode = GStreamerView.SIZE_STANDARD;
		dispWidth = getWidth();
		dispHeight = getHeight();
	}

	public void startPlayback() {
		String methodName = "startPlayback";
		Log.i(LOG_TAG + "", methodName);

		if (mIn != null) {
			mRun = true;
			thread.start();
		}
	}

	public void resumePlayback() {
		String methodName = "resumePlayback";
		Log.i(LOG_TAG + "", methodName);

		if (suspending) {
			if (mIn != null) {
				mRun = true;
				SurfaceHolder holder = getHolder();
				holder.addCallback(this);
				thread = new GStreamViewThread(holder, saved_context);
				thread.start();
			}
			suspending = false;
		}
	}

	public void stopPlayback() {
		String methodName = "stopPlayback";
		Log.i(LOG_TAG + "", methodName);

		if (mRun) {
			suspending = true;
		}
		mRun = false;
		boolean retry = true;
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	public static void setImageSize(int width, int height) {
		String methodName = "setImageSize";
		Log.i(LOG_TAG + "", methodName);

		GStreamerView.imgWidth = width;
		GStreamerView.imgHeight = height;
	}

	public void freeCameraMemory() {
		String methodName = "freeCameraMemory";
		Log.i(LOG_TAG + "", methodName);

		if (mIn != null) {
			mIn.freeCameraMemory();
		}
	}

	public GStreamerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		String methodName = "GStreamerView";
		Log.i(LOG_TAG + "", methodName);

		init(context);
	}

	public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
		String methodName = "surfaceChanged";
		Log.i(LOG_TAG + "", methodName);

		thread.setSurfaceSize(w, h);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		String methodName = "surfaceDestroyed";
		Log.i(LOG_TAG + "", methodName);

		surfaceDone = false;
		stopPlayback();
	}

	public GStreamerView(Context context) {
		super(context);

		String methodName = "GStreamerView";
    	Log.i(LOG_TAG + "", methodName );
		
		init(context);
	}

	public void surfaceCreated(SurfaceHolder holder) {
    	String methodName = "surfaceCreated";
    	Log.i(LOG_TAG + "", methodName );

		surfaceDone = true;
	}

	public void showFps(boolean b) {
    	String methodName = "showFps";
    	Log.i(LOG_TAG + "", methodName );

		showFps = b;
	}

	public void setSource(GStreamerInputStream source) {
    	String methodName = "setSource";
    	Log.i(LOG_TAG + "", methodName );

    	mIn = source;
		startPlayback();
	}

	public void setOverlayPaint(Paint p) {
    	String methodName = "setOverlayPaint";
    	Log.i(LOG_TAG + "", methodName );

		overlayPaint = p;
	}

	public void setOverlayTextColor(int c) {
    	String methodName = "setOverlayTextColor";
    	Log.i(LOG_TAG + "", methodName );

		overlayTextColor = c;
	}

	public void setOverlayBackgroundColor(int c) {
    	String methodName = "setOverlayBackgroundColor";
    	Log.i(LOG_TAG + "", methodName );

		overlayBackgroundColor = c;
	}

	public void setOverlayPosition(int p) {
    	String methodName = "setOverlayPosition";
    	Log.i(LOG_TAG + "", methodName );

		ovlPos = p;
	}

	public void setDisplayMode(int s) {
    	String methodName = "setDisplayMode";
    	Log.i(LOG_TAG + "", methodName );

		displayMode = s;
	}

	public void showDeviceResolution(boolean showOrNot, int argDWidth, int argDHeight) {
		String methodName = "showDeviceResolution";
		Log.i(LOG_TAG + "", methodName);

		showDeviceResolution = showOrNot;
		deviceWidth = argDWidth;
		deviceHeight = argDHeight;
	}
}
