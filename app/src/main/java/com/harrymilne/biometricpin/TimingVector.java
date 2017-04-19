package com.harrymilne.biometricpin;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by Harry on 16/04/2017.
 */

public class TimingVector {
    private Timing[] _timings;
    private int _user_id;

    public TimingVector(Timing[] timings) {
        this._user_id = timings[0].get_user_id();
        this._timings = timings;
        Log.i("biometric_pin", Arrays.toString(timings));
    }

    public Timing[] get_timings() {
        return this._timings;
    }

    public int get_user_id() {
        return _user_id;
    }

    public float get_interval(int idx){
        if (idx < this._timings.length) {
            return this._timings[idx].get_interval();
        } else {
            return -1;
        }
    }

    public double diff(TimingVector timing) {
        double diff = 0;
        for (int i = 0; i < this._timings.length; i++) {
            diff = diff + Math.pow(this.get_interval(i) - timing.get_interval(i), 2);
        }
        return Math.sqrt(diff);
    }
}
