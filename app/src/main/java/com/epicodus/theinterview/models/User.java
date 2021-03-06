package com.epicodus.theinterview.models;

import org.parceler.Parcel;

/**
 * Created by joannaanderson on 12/19/16.
 */

@Parcel
public class User {
    String uId;
    String pushId;

    public User(){}

    public User(String uId){
        this.uId = uId;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }
}
