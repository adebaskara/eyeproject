package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.Video;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class FdActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "OCVSample::Activity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0;
	private static final int TM_SQDIFF = 0;
	private static final int TM_SQDIFF_NORMED = 1;
	private static final int TM_CCOEFF = 2;
	private static final int TM_CCOEFF_NORMED = 3;
	private static final int TM_CCORR = 4;
	private static final int TM_CCORR_NORMED = 5;
	

	private int learn_frames = 0;
	private Mat templateR;
	private Mat templateL;
	int method = 0;

	private MenuItem mItemFace50;
	private MenuItem mItemFace40;
	private MenuItem mItemFace30;
	private MenuItem mItemFace20;
	private MenuItem mItemType;

	private Mat mRgba;
	private Mat mGray;
	// matrix for zooming
	private Mat mZoomWindow;
	private Mat mZoomWindow2;
	private Mat mZoomTemplateL, mZoomROIL, mZoomTemplateR, mZoomROIR;

	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private CascadeClassifier mCascadeEL;
	private CascadeClassifier mCascadeER;
	
	
	private int mDetectorType = JAVA_DETECTOR;
	private String[] mDetectorName;

	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;

	double xCenter = -1;
	double yCenter = -1;
	
	private int conditionR = 1; 
	private int conditionL = 1;
	private int counter = 0;
	private long startTime = 0L;
	private TextView status;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");


				try {
					// load cascade file from application resources
					InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,"lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					// --------------------------------- load right eye classificator -----------------------------------
					InputStream iser = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
					File cascadeDirER = getDir("cascadeER", Context.MODE_PRIVATE);
					File cascadeFileER = new File(cascadeDirER, "haarcascade_eye_right.xml");
					FileOutputStream oser = new FileOutputStream(cascadeFileER);

					byte[] bufferER = new byte[4096];
					int bytesReadER;
					while ((bytesReadER = iser.read(bufferER)) != -1) {
						oser.write(bufferER, 0, bytesReadER);
					}
					iser.close();
					oser.close();
					
					// --------------------------------- load left eye classificator -----------------------------------
					InputStream isel = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
					File cascadeDirEL = getDir("cascadeER", Context.MODE_PRIVATE);
					File cascadeFileEL = new File(cascadeDirEL, "haarcascade_eye_left.xml");
					FileOutputStream osel = new FileOutputStream(cascadeFileEL);

					byte[] bufferEL = new byte[4096];
					int bytesReadEL;
					while ((bytesReadEL = isel.read(bufferEL)) != -1) {
						osel.write(bufferEL, 0, bytesReadEL);
					}
					isel.close();
					osel.close();

					mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
					
					mCascadeER = new CascadeClassifier(cascadeFileER.getAbsolutePath());
					if (mCascadeER.empty()) {
						Log.e(TAG, "Failed to load right cascade classifier");
						mCascadeER = null;
					} else
						Log.i(TAG, "Loaded right cascade classifier from " + cascadeFileER.getAbsolutePath());
					
					mCascadeEL = new CascadeClassifier(cascadeFileEL.getAbsolutePath());
					if (mCascadeEL.empty()) {
						Log.e(TAG, "Failed to load left cascade classifier");
						mCascadeEL = null;
					} else
						Log.i(TAG, "Loaded left cascade classifier from " + cascadeFileEL.getAbsolutePath());

					cascadeDir.delete();
					cascadeDirER.delete();
					cascadeDirEL.delete();
					cascadeFileER.delete();
					cascadeFileEL.delete();

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
				mOpenCvCameraView.setCameraIndex(1);
				mOpenCvCameraView.enableFpsMeter();
				mOpenCvCameraView.enableView();

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public FdActivity() {
		mDetectorName = new String[2];
		mDetectorName[JAVA_DETECTOR] = "Java";

		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.face_detect_surface_view);

		status = (TextView) findViewById(R.id.status);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
		

		method = 3;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
		mZoomWindow.release();
		mZoomWindow2.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();

		if (mAbsoluteFaceSize == 0) {
			int height = mGray.rows();
			if (Math.round(height * mRelativeFaceSize) > 0) {
				mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
			}
		}
		
		if (mZoomWindow == null || mZoomWindow2 == null)
	        CreateAuxiliaryMats();

		MatOfRect faces = new MatOfRect();

		if (mJavaDetector != null)
			mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
			xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
			yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
			Point center = new Point(xCenter, yCenter);

			Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

			Core.putText(mRgba, "[" + center.x + "," + center.y + "]", new Point(center.x + 20, center.y + 20), Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));

			Rect r = facesArray[i];
			// compute the eye area by spliting
			Rect eyearea_right = new Rect((int) (r.x + r.width / 7.5), (int) (r.y + (r.height / 4.0)), (int)(r.width - 2 * r.width / 7.5) / 2, (int) (r.height / 5.0));
			Rect eyearea_left = new Rect((int) (r.x + r.width / 7.5) + (int)(r.width - 2 * r.width / 7.5) / 2, (int) (r.y + (r.height / 4.0)), (int)(r.width - 2 * r.width / 7.5) / 2, (int) (r.height / 5.0));
			// draw the area - mGray is working grayscale mat, if you want to see area in rgb preview, change mGray to mRgba
			Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(), new Scalar(255, 0, 0, 255), 2);
			Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(), new Scalar(255, 0, 0, 255), 2);

			if (learn_frames < 5) {
				templateR = get_template(mCascadeER, eyearea_right, 26);
				templateL = get_template(mCascadeEL, eyearea_left, 26);
				learn_frames++;
			} else {
				// Learning finished, use the new templates for template matching
				 match_eye(eyearea_right, templateR, "right"); 
				 match_eye(eyearea_left, templateL, "left");
			}
			
			
			// cut eye areas and put them to zoom windows
			Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
			Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
			
			if(conditionL==-1 && conditionR==-1){
				Core.circle(mRgba, new Point(200,200), 50, new Scalar(255, 0, 0, 255), -3);
				runOnUiThread(new Runnable() {
				    public void run() {
				    	status.setText("Close");
				    }
				});
				
				counter++;
				if(counter==1){
					startTime = SystemClock.uptimeMillis();
				}
				if(counter>=2){
					long timeMillis = SystemClock.uptimeMillis() - startTime;
					if(timeMillis<=300 && counter==2){
						Core.circle(mRgba, new Point(200,400), 40, new Scalar(0, 0, 255, 255), -3);
						runOnUiThread(new Runnable() {
						    public void run() {
						    	Toast.makeText(getApplicationContext(), "Triggered", 100).show();
						    }
						});
					} 
					counter = 0;
				}
			}
			else {
				runOnUiThread(new Runnable() {
				    public void run() {
				    	status.setText("Open");
				    }
				});
				Core.circle(mRgba, new Point(200,200), 50, new Scalar(0, 255, 0, 255), -3);
			}
			conditionL = 1;
			conditionR = 1;
			/*Mat gray = new Mat();
			Mat bw = new Mat();
			mRgba.copyTo(gray);
			mRgba.copyTo(bw);
			Imgproc.cvtColor(mZoomWindow2, gray, Imgproc.COLOR_RGBA2GRAY);
			Imgproc.adaptiveThreshold(gray, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, 2);
			Imgproc.threshold(gray, bw, 80, 255, Imgproc.THRESH_BINARY);
			Imgproc.erode(bw, bw, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2,2)));
			Imgproc.dilate(bw, bw, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5)));
			Imgproc.cvtColor(bw, mZoomWindow2, Imgproc.COLOR_GRAY2RGBA);
			
			
			Imgproc.cvtColor(mZoomWindow, gray, Imgproc.COLOR_RGBA2GRAY);
			Imgproc.adaptiveThreshold(gray, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, 2);
			Imgproc.threshold(gray, bw, 80, 255, Imgproc.THRESH_BINARY);
			Imgproc.erode(bw, bw, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2,2))); 
			Imgproc.dilate(bw, bw, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));
			Imgproc.cvtColor(bw, mZoomWindow, Imgproc.COLOR_GRAY2RGBA);*/
			
		}
		
		return mRgba;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemFace50 = menu.add("Face size 50%");
		mItemFace40 = menu.add("Face size 40%");
		mItemFace30 = menu.add("Face size 30%");
		mItemFace20 = menu.add("Face size 20%");
		mItemType = menu.add(mDetectorName[mDetectorType]);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == mItemFace50)
			setMinFaceSize(0.5f);
		else if (item == mItemFace40)
			setMinFaceSize(0.4f);
		else if (item == mItemFace30)
			setMinFaceSize(0.3f);
		else if (item == mItemFace20)
			setMinFaceSize(0.2f);
		else if (item == mItemType) {
			int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
			item.setTitle(mDetectorName[tmpDetectorType]);
		}
		return true;
	}

	private void setMinFaceSize(float faceSize) {
		mRelativeFaceSize = faceSize;
		mAbsoluteFaceSize = 0;
	}


	private void CreateAuxiliaryMats() {
		if (mGray.empty())
			return;

		int rows = mGray.rows();
		int cols = mGray.cols();

		if (mZoomWindow == null) {
			mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2 + cols / 10, cols);
			mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2 + cols / 10, cols);
			/*mZoomTemplateR = mRgba.submat(0, 150, 0, 150);
			mZoomROIR = mRgba.submat(150, 250, 0, 150);
			mZoomTemplateL = mRgba.submat(0, 150, 170, 320);
			mZoomROIL = mRgba.submat(150, 250, 170, 320);*/
		}
	}

	private void match_eye(Rect area, Mat mTemplate, String eye) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return ;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		
		Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF_NORMED);
		
		
		
		/*if(eye.equals("left")){
			Mat tem = new Mat();
			mRgba.copyTo(tem);
			Imgproc.cvtColor(mTemplate, tem, Imgproc.COLOR_GRAY2RGBA);
			Imgproc.resize(tem, mZoomTemplateL, mZoomTemplateL.size());
			Mat ro = new Mat();
			mRgba.copyTo(ro);
			Imgproc.cvtColor(mROI, ro, Imgproc.COLOR_GRAY2RGBA);
			Imgproc.resize(ro, mZoomROIL, mZoomROIL.size());
			tem.release();
			ro.release();
		} else {
			Mat tem = new Mat();
			mRgba.copyTo(tem);
			Imgproc.cvtColor(mTemplate, tem, Imgproc.COLOR_GRAY2RGBA);
			Imgproc.resize(tem, mZoomTemplateR, mZoomTemplateR.size());
			Mat ro = new Mat();
			mRgba.copyTo(ro);
			Imgproc.cvtColor(mROI, ro, Imgproc.COLOR_GRAY2RGBA);
			Imgproc.resize(ro, mZoomROIR, mZoomROIR.size());
			tem.release();
			ro.release();
		}*/
		/*int col = mTemplate.cols();
		int row = mTemplate.rows();
		double corr = 0;
		double denom = 0;
		int u = 0;
		int v = 0;
		double meanT = Core.mean(mTemplate).val[0];
		for(int x=0; x<mROI.cols(); x++)
		{
			for(int y=0; y<mROI.rows(); y++)
			{
				if(x + col > mROI.cols()) u = mROI.cols();
				else u = x + col;
				if(y + row > mROI.rows()) v = mROI.rows();
				else v = y + row;
				double meanI = Core.mean(mROI.submat(y, v, x, u)).val[0];
			}
		}*/
		
		
		
		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		// there is difference in matching methods - best match is max/min value
		matchLoc = mmres.maxLoc;
		
		double cmax = mmres.maxVal;
		double cmin = mmres.minVal;
		Log.i("corr", String.valueOf(cmax) + ", "+ String.valueOf(cmin));
		
		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x, matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 255, 255)); // deteksi pupil mata
		//Rect rec = new Rect(matchLoc_tx,matchLoc_ty);
		if(cmax <= 0.8){
			int y = 0;
			if(eye.equals("left")){
				y = 70;
				conditionL = -1;
			}
			else{
				y = 120;
				conditionR = -1;
			}
			Core.putText(mRgba, eye + "Closed", new Point(350, y), Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255, 0, 0, 255));
		} else {
			int y = 0;
			if(eye.equals("left")){
				y = 70;
				conditionL = 1;
			}
			else{
				y = 120;
				conditionR = -1;
			}
			Core.putText(mRgba, eye + "Open", new Point(350, y), Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0, 255, 0, 255));
		}
		
		

	}

	private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = mGray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
						| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length;) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_only_rectangle = new Rect((int) e.tl().x,
					(int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			mROI = mGray.submat(eye_only_rectangle);
			Mat vyrez = mRgba.submat(eye_only_rectangle);
			
			
			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye_template = new Rect((int) iris.x - (size / 2) - 5, (int) iris.y
					- (size / 2), size, size);
			Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
					new Scalar(255, 0, 0, 255), 2);
			template = (mGray.submat(eye_template)).clone();
			return template;
		}
		return template;
	}
	
	public void onRecreateClick(View v)
    {
    	learn_frames = 0;
    }
	
	


}
