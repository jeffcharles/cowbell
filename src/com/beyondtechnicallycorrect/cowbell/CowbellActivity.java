package com.beyondtechnicallycorrect.cowbell;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.ImageView;

/**
 * Activity for ringing a cowbell
 */
public class CowbellActivity extends Activity implements SensorEventListener {
	
	private final int TIPPING_POINT_IN_DEGREES = 30;
	
	private ImageView mCowbellImage;
	private RotationPosition mCurrentRotation;
	private Animation mRotateToLeft;
	private Animation mRotateFromLeft;
	private Animation mRotateToRight;
	private Animation mRotateFromRight;
	private Animation mRotateFromLeftToRight;
	private Animation mRotateFromRightToLeft;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagneticField;
	
	private float[] mGravity;
	private float[] mGeomagneticVector;
	
	private MediaPlayer mCowbellSound;
	private final Object mCowbellSoundLock = new Object();
	
	private Normalizer mNormalizer;
	
	/**
	 * Called when the activity is first created
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final int CENTRE_DEGREE = 0;
        mCowbellImage = (ImageView)this.findViewById(R.id.cowbell);
        mCurrentRotation = RotationPosition.CENTRE;
        mRotateToLeft =
        		new CowbellRotateAnimation(
        				CENTRE_DEGREE,
        				TIPPING_POINT_IN_DEGREES
        			);
        mRotateFromLeft =
        		new CowbellRotateAnimation(
        				TIPPING_POINT_IN_DEGREES,
        				CENTRE_DEGREE
        			);
        mRotateToRight =
        		new CowbellRotateAnimation(
        				CENTRE_DEGREE,
        				-TIPPING_POINT_IN_DEGREES
        			);
        mRotateFromRight =
        		new CowbellRotateAnimation(
        				-TIPPING_POINT_IN_DEGREES,
        				CENTRE_DEGREE
        			);
        mRotateFromLeftToRight =
        		new CowbellRotateAnimation(
        				TIPPING_POINT_IN_DEGREES,
        				-TIPPING_POINT_IN_DEGREES
        			);
        mRotateFromRightToLeft =
        		new CowbellRotateAnimation(
        				-TIPPING_POINT_IN_DEGREES,
        				TIPPING_POINT_IN_DEGREES
        			);
        
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
		// Need to sync to avoid race condition with playing cowbell sound
		synchronized (mCowbellSoundLock) {
			mCowbellSound.release();
			mCowbellSound = null;
		}
	}

	/**
	 * Called when the activity appears in the foreground
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		mNormalizer = new Normalizer();
		
		mGravity = null;
		mGeomagneticVector = null;
		
		mSensorManager.registerListener(
				this,
				mAccelerometer,
				SensorManager.SENSOR_DELAY_GAME
			);
		mSensorManager.registerListener(
				this,
				mMagneticField,
				SensorManager.SENSOR_DELAY_GAME
			);
		
		mCowbellSound = MediaPlayer.create(this, R.raw.cowbell);
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
		
		populateSensorMembers(event);
		
		if(mGravity == null || mGeomagneticVector == null) {
			return;
		}
		
		OrientationResult result = tryGetOrientationResult();
		if(!result.succeeded) {
			return;
		}
		float normalizedRoll = result.normalizedOrientation;
		Log.d("CowbellActivity", "Roll: " + normalizedRoll);
		
		processOrientationChange(normalizedRoll);		
	}
	
	private void populateSensorMembers(SensorEvent event) {
		if(event.sensor == mAccelerometer) {
			mGravity = event.values;
		}
		if(event.sensor == mMagneticField) {
			mGeomagneticVector = event.values;
		}
	}
	
	private class OrientationResult {
		public boolean succeeded;
		public float normalizedOrientation;
	}
	
	private OrientationResult tryGetOrientationResult() {
		
		OrientationResult result = new OrientationResult();
		
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
			result.succeeded = false;
			return result;
		}
		
		final int ORIENTATION_CAPACITY = 3;
		float[] orientation = new float[ORIENTATION_CAPACITY];
		SensorManager.getOrientation(rRotationMatrix, orientation);
		
		final int ROLL_INDEX = 2;
		final float DEGREES_PER_RADIAN = 57.2957795f;
		float roll = orientation[ROLL_INDEX] * DEGREES_PER_RADIAN;
		mNormalizer.AddValue(roll);
		float normalizedRoll = mNormalizer.getNormalizedValue();
		
		result.succeeded = true;
		result.normalizedOrientation = normalizedRoll;
		return result;
	}
	
	private void processOrientationChange(float normalizedRoll) {
		
		final int LEFT_DEGREE = -TIPPING_POINT_IN_DEGREES;
		final int RIGHT_DEGREE = TIPPING_POINT_IN_DEGREES;
		
		boolean playCowbellSound = false;
		
		if(LEFT_DEGREE < normalizedRoll && normalizedRoll < RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.LEFT) {
			mCurrentRotation = RotationPosition.CENTRE;
			mCowbellImage.startAnimation(mRotateFromLeft);
		}
		
		if(LEFT_DEGREE < normalizedRoll && normalizedRoll < RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.RIGHT) {
			mCurrentRotation = RotationPosition.CENTRE;
			mCowbellImage.startAnimation(mRotateFromRight);
		}
		
		if(normalizedRoll < LEFT_DEGREE &&
				mCurrentRotation == RotationPosition.RIGHT) {
			mCurrentRotation = RotationPosition.LEFT;
			mCowbellImage.startAnimation(mRotateFromRightToLeft);
			playCowbellSound = true;
		}
		
		if(normalizedRoll > RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.LEFT) {
			mCurrentRotation = RotationPosition.RIGHT;
			mCowbellImage.startAnimation(mRotateFromLeftToRight);
			playCowbellSound = true;
		}
		
		if(normalizedRoll < LEFT_DEGREE &&
				mCurrentRotation == RotationPosition.CENTRE) {
			mCurrentRotation = RotationPosition.LEFT;
			mCowbellImage.startAnimation(mRotateToLeft);
			playCowbellSound = true;
		}
		
		if(normalizedRoll > RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.CENTRE) {
			mCurrentRotation = RotationPosition.RIGHT;
			mCowbellImage.startAnimation(mRotateToRight);
			playCowbellSound = true;
		}
		
		if(playCowbellSound) {
			// Need to sync to prevent accessing a released cowbell sound
			synchronized (mCowbellSoundLock) {
				if(mCowbellSound != null) {
					if(mCowbellSound.isPlaying()) {
						mCowbellSound.seekTo(0);
						
					}
					mCowbellSound.start();
				}
			}
		}
	}
    
}