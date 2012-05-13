package com.beyondtechnicallycorrect.cowbell;

import android.view.animation.RotateAnimation;

public class CowbellRotateAnimation extends RotateAnimation {
	
	public CowbellRotateAnimation(float fromDegrees, float toDegrees) {
		super(fromDegrees, toDegrees);
		this.setFillAfter(true);
		final int DURATION = 50;
		this.setDuration(DURATION);
	}

}
