package com.harrymilne.biometricpin;

/**
 * Created by Harry on 27/03/2017.
 */

public class User {

    private int _id;
    private String _name;
    private String _pin;
    private int _timing_count;

    public User(String name) {
        this._name = name;
    }

    public User(int user_id, String name, String pin, int timing_count) {
        this._id = user_id;
        this._name = name;
        this._pin = pin;
        this._timing_count = timing_count;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String get_pin() {
        return _pin;
    }

    public void set_pin(String _pin) {
        this._pin = _pin;
    }

    public String get_name() {
        return _name;
    }

    public void set_name(String _name) {
        this._name = _name;
    }

    public int get_timing_count() {
        return _timing_count;
    }

    public void set_timing_count(int _timing_count) {
        this._timing_count = _timing_count;
    }
}
