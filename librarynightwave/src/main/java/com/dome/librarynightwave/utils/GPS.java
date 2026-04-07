package com.dome.librarynightwave.utils;

public class GPS {
    public static String convert(double value) {
        StringBuilder sb = new StringBuilder(20);
        sb.append((int) value);
        sb.append("/1,");
        value = (value - (int) value) * 60;
        sb.append((int) value);
        sb.append("/1,");
        value = (value - (int) value) * 60000;
        sb.append((int) value);
        sb.append("/1000");
        return sb.toString();
    }

    public static String latitudeRef(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    public static String longitudeRef(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }
}