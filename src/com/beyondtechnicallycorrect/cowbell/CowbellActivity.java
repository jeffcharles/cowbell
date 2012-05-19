package com.beyondtechnicallycorrect.cowbell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.widget.ImageView;

/**
 * Activity for ringing a cowbell
 */
public class CowbellActivity extends Activity implements SensorEventListener {
	
	private final int DIALOG_ATTRIBUTION_ID = 0;
	
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
	private boolean mListenForAccelerometer;
	private boolean mListenForMagneticField;
	
	private float[] mGravity;
	private float[] mGeomagneticVector;
	
	private MediaPlayer mCowbellSound;
	private final Object mCowbellSoundLock = new Object();
	
	private Normalizer mNormalizer;
	
	private boolean mProcessingOrientationChange;
	
	/**
	 * Called when the activity is first created
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mProcessingOrientationChange = false;
        
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
	 * Called when menu button pressed (pre-3.0) or when activity
	 * created (post-3.0)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		this.getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Called when an options menu item is selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
			case R.id.attribution:
				this.showDialog(DIALOG_ATTRIBUTION_ID);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Called when a dialog is created
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
			case DIALOG_ATTRIBUTION_ID:
				Dialog dialog = createAttributionDialog();
				return dialog;
			default:
				return super.onCreateDialog(id);
		}
	}
	
	private Dialog createAttributionDialog() {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
			.setMessage(R.string.attribution_text)
			.setCancelable(true);
		AlertDialog dialog = builder.create();
		return dialog;
	}

	/**
	 * Called when one of the sensor's accuracy changes
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
		boolean sensorAccurate =
				accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ||
				accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
		if(sensor == mAccelerometer) {
			mListenForAccelerometer = sensorAccurate;
		}
		if(sensor == mMagneticField) {
			mListenForMagneticField = sensorAccurate;
		}
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
		
		// If an orientation change is already in progress,
		// then don't process this reading
		if(!mProcessingOrientationChange) {
			mProcessingOrientationChange = true;
			processOrientationChange(normalizedRoll);
			mProcessingOrientationChange = false;
		}
	}
	
	private void populateSensorMembers(SensorEvent event) {
		
		if(event.sensor == mAccelerometer && mListenForAccelerometer) {
			mGravity = event.values;
		}
		if(event.sensor == mMagneticField && mListenForMagneticField) {
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
		boolean gotResult =
				SensorManager.getRotationMatrix(
						rRotationMatrix, // out variable
						null, // out variable
						mGravity,
						mGeomagneticVector
					);
		if(!gotResult) {
			result.succeeded = false;
			return result;
		}
		
		final int ORIENTATION_CAPACITY = 3;
		float[] orientation = new float[ORIENTATION_CAPACITY];
		SensorManager.getOrientation(
				rRotationMatrix,
				orientation // out variable
			);
		
		final int ROLL_INDEX = 2;
		final float DEGREES_PER_RADIAN = 57.2957795f;
		float roll = orientation[ROLL_INDEX] * DEGREES_PER_RADIAN;
		mNormalizer.AddValue(roll);
		float normalizedRoll = mNormalizer.getNormalizedValue();
		
		result.succeeded = true;
		result.normalizedOrientation = normalizedRoll;
		return result;
	}
	
	private synchronized void processOrientationChange(float roll) {
		
		final int FUZZY_FACTOR = 5;
		
		final int TO_LEFT_DEGREE = -TIPPING_POINT_IN_DEGREES - FUZZY_FACTOR;
		final int FROM_LEFT_DEGREE = -TIPPING_POINT_IN_DEGREES + FUZZY_FACTOR;
		final int TO_RIGHT_DEGREE = TIPPING_POINT_IN_DEGREES + FUZZY_FACTOR;
		final int FROM_RIGHT_DEGREE = TIPPING_POINT_IN_DEGREES - FUZZY_FACTOR;
		
		boolean playCowbellSound = false;
		
		if(FROM_LEFT_DEGREE < roll && roll < TO_RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.LEFT) {
			mCurrentRotation = RotationPosition.CENTRE;
			mCowbellImage.startAnimation(mRotateFromLeft);
		}
		
		if(TO_LEFT_DEGREE < roll && roll < FROM_RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.RIGHT) {
			mCurrentRotation = RotationPosition.CENTRE;
			mCowbellImage.startAnimation(mRotateFromRight);
		}
		
		if(roll < TO_LEFT_DEGREE &&
				mCurrentRotation == RotationPosition.RIGHT) {
			mCurrentRotation = RotationPosition.LEFT;
			mCowbellImage.startAnimation(mRotateFromRightToLeft);
			playCowbellSound = true;
		}
		
		if(roll > TO_RIGHT_DEGREE &&
				mCurrentRotation == RotationPosition.LEFT) {
			mCurrentRotation = RotationPosition.RIGHT;
			mCowbellImage.startAnimation(mRotateFromLeftToRight);
			playCowbellSound = true;
		}
		
		if(roll < TO_LEFT_DEGREE &&
				mCurrentRotation == RotationPosition.CENTRE) {
			mCurrentRotation = RotationPosition.LEFT;
			mCowbellImage.startAnimation(mRotateToLeft);
			playCowbellSound = true;
		}
		
		if(roll > TO_RIGHT_DEGREE &&
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