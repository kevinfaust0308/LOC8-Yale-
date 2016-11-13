package com.example.kevinfaust.loc8;

import java.util.List;
import java.util.Map;

/**
 * Created by Kevin Faust on 11/12/2016.
 */

public class User {

    private String name, email, pictureUrl, latitude, longitude, statusMsg;
    private boolean online;
    private Map<String, String> friends;

    public User() {
    }

    public User(String name, String email, String pictureUrl, String latitude, String longitude, String statusMsg, boolean online, Map<String, String> friends) {
        this.name = name;
        this.email = email;
        this.pictureUrl = pictureUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.statusMsg = statusMsg;
        this.online = online;
        this.friends = friends;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public Map<String, String> getFriends() {
        return friends;
    }

    public void setFriends(Map<String, String> friends) {
        this.friends = friends;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
