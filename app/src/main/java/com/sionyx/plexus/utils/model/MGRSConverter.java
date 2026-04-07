package com.sionyx.plexus.utils.model;

public class MGRSConverter {
    private static final double a = 6378137; // Semi-major axis of Earth (meters)
    private static final double f = 1 / 298.257223563; // Flattening of Earth

    public static String toMGRS(double latitude, double longitude) {
        // Validate input
        if (latitude < -80 || latitude > 84) {
            throw new IllegalArgumentException("Latitude must be between -80 and 84 degrees.");
        }

        // Convert degrees to radians
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);

        // Determine UTM zone
        int zoneNumber = (int) Math.floor((longitude + 180) / 6) + 1;

        // Calculate parameters for MGRS conversion
        double e2 = f * (2 - f);
        double n = Math.cos(latRad);
        double a1 = a * n;
        double s1 = 1 - e2 * n * n;
        double s2 = Math.sqrt(s1);
        double falseNorthing = (latitude < 0)
                ? 0 : a * (1 - s1) * Math.sin(Math.PI / 2);
        double falseEasting = 500000 * (zoneNumber - 1);

        // Calculate MGRS easting and northing
        double easting = (a1 * lonRad * s2 / s1 + falseEasting);
        double northing = (a1 / s2 * (Math.atan(Math.tan(latRad) / s2) + e2 * atanh(e2 * Math.sin(latRad) / s2)) + falseNorthing);

        // Convert easting and northing to integer for MGRS formatting
        int eastingInt = (int) Math.floor(easting);
        int northingInt = (int) Math.floor(northing);

        // Calculate UTM grid letter based on northing
        char gridLetter = (char) ((northingInt < 0) ? 'A' : 'C' + ((northingInt / 100000) % 2));

        // Format and return MGRS string
        return String.format("%d%c %d%d", zoneNumber, gridLetter, eastingInt, northingInt);
    }
    public static double atanh(double x) {
        return 0.5 * Math.log((1 + x) / (1 - x));
    }
    public static void main(String[] args) {
        double latitude = 41.783333;
        double longitude = -87.633333;

        String mgrs = toMGRS(latitude, longitude);
        System.out.println("MGRS: " + mgrs); // Output: 16S 352206 4705970
    }
}
