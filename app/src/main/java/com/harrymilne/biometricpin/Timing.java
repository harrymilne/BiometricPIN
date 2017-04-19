package com.harrymilne.biometricpin;

/**
 * Created by Harry on 12/04/2017.
 */

public class Timing {
    private int _id;
    private int _user_id;
    private int _set_id;
    private int _idx;
    private long _interval;

    public Timing(int user_id, int timing_set_id, int idx, long interval) {
        this._user_id = user_id;
        this._set_id = timing_set_id;
        this._idx = idx;
        this._interval = interval;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int get_user_id() {
        return _user_id;
    }

    public void set_user_id(int _user_id) {
        this._user_id = _user_id;
    }

    public int get_timing_set_id() {
        return _set_id;
    }

    public void set_timing_set_id(int _timing_set_id) {
        this._set_id = _timing_set_id;
    }

    public int get_idx() {
        return _idx;
    }

    public void set_idx(int _idx) {
        this._idx = _idx;
    }

    public float get_interval() {
        return _interval;
    }

    public void set_interval(long _interval) {
        this._interval = _interval;
    }

}
