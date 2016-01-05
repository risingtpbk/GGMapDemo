package com.example.framgianguyenkeninh.mapdemo.Item;

import java.util.ArrayList;

/**
 * Created by FRAMGIA\nguyen.ke.ninh on 05/01/2016.
 */
public class DirectionPointGson {
    private ArrayList<Route> routes;

    public ArrayList<Route> getRoutes() {
        return this.routes;
    }

    public class Route {
        private ArrayList<Leg> legs;
        private Polyline overviewPolyline;

        public ArrayList<Leg> getLegs() {
            return this.legs;
        }

        public Polyline getOverviewPolyline() {
            return this.overviewPolyline;
        }
    }

    public class Leg {
        private String startAddress;
        private String endAddress;
        private LocationItem startLocation;
        private LocationItem endLocation;
        private ArrayList<Step> steps;

        public String getStartAddress() {
            return this.startAddress;
        }

        public String getEndAddress() {
            return this.endAddress;
        }

        public LocationItem getStartLocation() {
            return this.startLocation;
        }

        public LocationItem getEndLocation() {
            return this.endLocation;
        }

        public ArrayList<Step> getSteps() {
            return this.steps;
        }
    }

    public class Step {
        private LocationItem startLocation;
        private LocationItem endLocation;
        private String htmlInstructions;

        public LocationItem getStartLocation() {
            return this.startLocation;
        }

        public LocationItem getEndLocation() {
            return this.endLocation;
        }

        public String getHtmlInstructions() {
            return this.htmlInstructions;
        }

    }

    public class Polyline {
        private String points;

        public String getPoints() {
            return this.points;
        }
    }
}
