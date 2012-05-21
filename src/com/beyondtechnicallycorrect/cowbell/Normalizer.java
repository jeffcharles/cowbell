package com.beyondtechnicallycorrect.cowbell;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Used for averaging out a series of input values
 */
public class Normalizer {
	
	final int MAX_LENGTH = 8;
	
	private Queue<Float> mValues;
	
	public Normalizer() {
		mValues = new ArrayBlockingQueue<Float>(MAX_LENGTH);
	}
	
	public synchronized void AddValue(float value) {
		if(mValues.size() == MAX_LENGTH) {
			mValues.remove();
		}
		mValues.add(value);
	}
	
	public float getNormalizedValue() {
		float sum = 0;
		for(float value : mValues) {
			sum += value;
		}
		float normalizedValue = sum / mValues.size();
		return normalizedValue;
	}

}
