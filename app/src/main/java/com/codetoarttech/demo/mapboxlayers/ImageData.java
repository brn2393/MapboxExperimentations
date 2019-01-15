package com.codetoarttech.demo.mapboxlayers;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageData implements Parcelable {

    public static final Creator<ImageData> CREATOR = new Creator<ImageData>() {
        @Override
        public ImageData createFromParcel(Parcel source) {
            return new ImageData(source);
        }

        @Override
        public ImageData[] newArray(int size) {
            return new ImageData[size];
        }
    };
    @JsonProperty("thumb")
    private String thumb;
    @JsonProperty("main")
    private String main;
    @JsonProperty("original")
    private String original;
    @JsonProperty("id")
    private String id;
    @JsonProperty("position")
    private String position;
    @JsonProperty("is_selected")
    private boolean isSelected;

    public ImageData() {
    }

    protected ImageData(Parcel in) {
        this.thumb = in.readString();
        this.main = in.readString();
        this.original = in.readString();
        this.id = in.readString();
        this.position = in.readString();
        this.isSelected = in.readByte() != 0;
    }

    public String getThumb() {
        if (thumb == null) return null;
        return thumb.trim();
    }

    public String getMain() {
        if (main == null) return null;
        return main.trim();
    }

    public String getOriginal() {
        if (original == null) return null;
        return original.trim();
    }

    public String getId() {
        return id;
    }

    public String getPosition() {
        return position;
    }

    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public int hashCode() {
        int result = thumb != null ? thumb.hashCode() : 0;
        result = 31 * result + (main != null ? main.hashCode() : 0);
        result = 31 * result + (original != null ? original.hashCode() : 0);
        result = 31 * result + id.hashCode();
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (isSelected ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageData imageData = (ImageData) o;

        if (isSelected != imageData.isSelected) return false;
        if (thumb != null ? !thumb.equals(imageData.thumb) : imageData.thumb != null) return false;
        if (main != null ? !main.equals(imageData.main) : imageData.main != null) return false;
        if (original != null ? !original.equals(imageData.original) : imageData.original != null)
            return false;
        if (!id.equals(imageData.id)) return false;
        return position != null ? position.equals(imageData.position) : imageData.position == null;
    }

    @Override
    public String toString() {
        return "ImageData{" +
                "thumb='" + thumb + '\'' +
                ", main='" + main + '\'' +
                ", original='" + original + '\'' +
                ", id='" + id + '\'' +
                ", position='" + position + '\'' +
                ", isSelected='" + isSelected + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.thumb);
        dest.writeString(this.main);
        dest.writeString(this.original);
        dest.writeString(this.id);
        dest.writeString(this.position);
        dest.writeByte(this.isSelected ? (byte) 1 : (byte) 0);
    }
}