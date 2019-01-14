package com.codetoarttech.demo.mapboxlayers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DiscoverApiResponse {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("data")
    private List<UserData> discoveredUserList;

    public DiscoverApiResponse() {
        discoveredUserList = new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<UserData> getDiscoveredUserList() {
        return discoveredUserList;
    }

    @Override
    public String toString() {
        return "DiscoverApiResponse{" +
                "discoveredUserList=" + discoveredUserList +
                '}';
    }
}