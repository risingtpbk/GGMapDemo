package com.example.framgianguyenkeninh.mapdemo.Item;

import java.util.ArrayList;

/**
 * Created by FRAMGIA\nguyen.ke.ninh on 05/01/2016.
 */
public class ReverseGeoCodeGson {
    private ArrayList<Results> results;

    public ArrayList<Results> getResults() {
        return this.results;
    }

    public class Results {
        private String formattedAddress;
        private Geometry geometry;

        public String getFormattedAddress() {
            return this.formattedAddress;
        }

        public Geometry getGeometry() {
            return this.geometry;
        }
    }

    public class Geometry {
        private LocationItem location;

        public LocationItem getLocation() {
            return this.location;
        }
    }
}
