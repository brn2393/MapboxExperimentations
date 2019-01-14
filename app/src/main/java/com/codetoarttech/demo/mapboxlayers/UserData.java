package com.codetoarttech.demo.mapboxlayers;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

public class UserData implements Parcelable {
    public static final Creator<UserData> CREATOR = new Creator<UserData>() {
        @Override
        public UserData createFromParcel(Parcel source) {
            return new UserData(source);
        }

        @Override
        public UserData[] newArray(int size) {
            return new UserData[size];
        }
    };
    @JsonProperty("id")
    private int id;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("phone")
    private String phoneNumber;
    @JsonProperty("lat")
    private String lat;
    @JsonProperty("lng")
    private String lng;
    @JsonProperty("profile_images")
    private List<ImageData> profileImageList;

    public UserData() {
        profileImageList = new ArrayList<>();
    }

    protected UserData(Parcel in) {
        this.id = in.readInt();
        this.firstName = in.readString();
        this.lastName = in.readString();
        this.phoneNumber = in.readString();
        this.lat = in.readString();
        this.lng = in.readString();
        this.profileImageList = in.createTypedArrayList(ImageData.CREATOR);
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public LatLng getLatLng() {
        try {
            return new LatLng(Double.valueOf(lat), Double.valueOf(lng));
        } catch (Exception e) {
            return null;
        }
    }

    public ImageData getSelectedProfileImage() {
        ImageData result = null;
        if (!isProfileImageListEmpty()) {
            for (ImageData imageData : profileImageList) {
                if (imageData != null && imageData.isSelected()) {
                    result = imageData;
                    break;
                }
            }
        }
        return result;
    }

    public boolean isProfileImageListEmpty() {
        return profileImageList == null || profileImageList.size() == 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.firstName);
        dest.writeString(this.lastName);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.lat);
        dest.writeString(this.lng);
        dest.writeTypedList(this.profileImageList);
    }

    @Override
    public String toString() {
        return "UserData{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", lat='" + lat + '\'' +
                ", lng='" + lng + '\'' +
                ", profileImageList=" + profileImageList +
                '}';
    }
}