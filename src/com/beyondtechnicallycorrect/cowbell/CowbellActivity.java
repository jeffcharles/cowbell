package com.beyondtechnicallycorrect.cowbell;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity for ringing a cowbell
 */
public class CowbellActivity extends Activity {
    
	/**
	 * Called when the activity is first created
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}