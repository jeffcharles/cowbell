package com.beyondtechnicallycorrect.cowbell;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Activity for ringing a cowbell
 */
public class CowbellActivity extends Activity implements SensorEventListener {
    
	private ImageView mCowbellImage;
	private boolean mRotatedLeft;
	private Animation mRotateToLeft;
	private Animation mRotateFromLeft;
	private boolean mRotatedRight;
	private Animation mRotateToRight;
	private Animation mRotateFromRight;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagneticField;
	
	private float[] mGravity;
	private float[] mGeomagneticVector;
	
	/**
	 * Called when the activity is first created
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mCowbellImage = (ImageView)this.findViewById(R.id.cowbell);
        mRotatedLeft = false;
        mRotateToLeft = AnimationUtils.loadAnimation(this, R.anim.rotate_left);
        mRotateFromLeft = AnimationUtils.loadAnimation(this, R.anim.rotate_from_left);
        mRotatedRight = false;
        mRotateToRight = AnimationUtils.loadAnimation(this, R.anim.rotate_right);
        mRotateFromRight = AnimationUtils.loadAnimation(this, R.anim.rotate_from_right);
        
        mSensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE);
		mAccelerometer =
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagneticField =
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    /**
     * Called when the activity leaves the foreground
     */
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	/**
	 * Called when the activity appears in the foreground
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(
				this,
				mAccelerometer,
				SensorManager.SENSOR_DELAY_UI
			);
		mSensorManager.registerListener(
				this,
				mMagneticField,
				SensorManager.SENSOR_DELAY_UI
			);
	}

	/**
	 * Called when one of the sensor's accuracy changes
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do nothing
	}

	/**
	 * Called when a sensor event occurs
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor == mAccelerometer) {
			mGravity = event.values;
		}
		if(event.sensor == mMagneticField) {
			mGeomagneticVector = event.values;
		}
		
		if(mGravity == null || mGeomagneticVector == null) {
			return;
		}
		
		final int R_CAPACITY = 9;
		float[] rRotationMatrix = new float[R_CAPACITY];
		float[] iRotationMatrix = null;
		boolean gotResult =
				SensorManager.getRotationMatrix(
						rRotationMatrix, // out variable
						iRotationMatrix, // out variable
						mGravity,
						mGeomagneticVector
					);
		if(!gotResult) {
			return;
		}
		
		final int ORIENTATION_CAPACITY = 3;
		float[] orientation = new float[ORIENTATION_CAPACITY];
		SensorManager.getOrientation(rRotationMatrix, orientation);
		
		final int ROLL_INDEX = 2;
		final float DEGREES_PER_RADIAN = 57.2957795f;
		float roll = orientation[ROLL_INDEX] * DEGREES_PER_RADIAN;
		Log.d("CowbellActivity", "Roll: " + roll);
		
		final int LEFT_DEGREE = -30;
		final int RIGHT_DEGREE = 30;
		
		if(roll > LEFT_DEGREE && mRotatedLeft) {
			mRotatedLeft = false;
			mCowbellImage.startAnimation(mRotateFromLeft);
		}
		
		if(roll < RIGHT_DEGREE && mRotatedRight) {
			mRotatedRight = false;
			mCowbellImage.startAnimation(mRotateFromRight);
		}
		
		if(roll < LEFT_DEGREE && !mRotatedLeft) {
			mRotatedLeft = true;
			mCowbellImage.startAnimation(mRotateToLeft);
		}
		
		if(roll > RIGHT_DEGREE && !mRotatedRight) {
			mRotatedRight = true;
			mCowbellImage.startAnimation(mRotateToRight);
		}
		
	}
    
    
}