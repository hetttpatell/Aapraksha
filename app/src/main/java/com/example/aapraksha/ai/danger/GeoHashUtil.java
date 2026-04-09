package com.example.aapraksha.ai.danger;

/**
 * Lightweight GeoHash encoder for the Danger Intelligence Layer.
 * 
 * Encodes latitude/longitude into a geohash string used for zone clustering.
 * We use precision 6 (~1.2km x 0.6km cells) which is ideal for neighborhood-level
 * danger zone detection.
 * 
 * This avoids depending on the full GeoFire library (which targets Realtime Database)
 * and gives us a simple, dependency-free utility for Firestore geohashing.
 */
public class GeoHashUtil {

    /**
     * Default geohash precision for danger zone clustering.
     * Precision 6 = ~1.2km x 0.6km cells — neighborhood-level granularity.
     * 
     *  Precision | Cell width x height
     *  ---------|--------------------
     *  1        | 5,000km x 5,000km
     *  2        | 1,250km x 625km
     *  3        | 156km x 156km
     *  4        | 39.1km x 19.5km
     *  5        | 4.89km x 4.89km
     *  6        | 1.22km x 0.61km  ← WE USE THIS
     *  7        | 153m x 153m
     *  8        | 38.2m x 19.1m
     */
    public static final int DEFAULT_PRECISION = 6;

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /**
     * Encode a latitude/longitude pair into a geohash string.
     *
     * @param latitude  Latitude in degrees (-90 to 90)
     * @param longitude Longitude in degrees (-180 to 180)
     * @param precision Number of characters in the geohash (1-12)
     * @return Geohash string
     */
    public static String encode(double latitude, double longitude, int precision) {
        if (precision < 1 || precision > 12) {
            throw new IllegalArgumentException("Precision must be between 1 and 12");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        double latMin = -90.0, latMax = 90.0;
        double lonMin = -180.0, lonMax = 180.0;

        StringBuilder geohash = new StringBuilder();
        boolean isLon = true; // Start with longitude
        int bit = 0;
        int ch = 0;

        while (geohash.length() < precision) {
            double mid;
            if (isLon) {
                mid = (lonMin + lonMax) / 2;
                if (longitude >= mid) {
                    ch |= (1 << (4 - bit));
                    lonMin = mid;
                } else {
                    lonMax = mid;
                }
            } else {
                mid = (latMin + latMax) / 2;
                if (latitude >= mid) {
                    ch |= (1 << (4 - bit));
                    latMin = mid;
                } else {
                    latMax = mid;
                }
            }
            isLon = !isLon;
            bit++;

            if (bit == 5) {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    /**
     * Encode with default precision (6 characters = ~1.2km zones)
     */
    public static String encode(double latitude, double longitude) {
        return encode(latitude, longitude, DEFAULT_PRECISION);
    }

    /**
     * Decode a geohash string back to its approximate center coordinate.
     *
     * @param geohash The geohash string to decode
     * @return double array: [latitude, longitude]
     */
    public static double[] decode(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }

        double latMin = -90.0, latMax = 90.0;
        double lonMin = -180.0, lonMax = 180.0;
        boolean isLon = true;

        for (char c : geohash.toCharArray()) {
            int charIndex = BASE32.indexOf(c);
            if (charIndex < 0) {
                throw new IllegalArgumentException("Invalid geohash character: " + c);
            }

            for (int bit = 4; bit >= 0; bit--) {
                double mid;
                if (isLon) {
                    mid = (lonMin + lonMax) / 2;
                    if ((charIndex & (1 << bit)) != 0) {
                        lonMin = mid;
                    } else {
                        lonMax = mid;
                    }
                } else {
                    mid = (latMin + latMax) / 2;
                    if ((charIndex & (1 << bit)) != 0) {
                        latMin = mid;
                    } else {
                        latMax = mid;
                    }
                }
                isLon = !isLon;
            }
        }

        double latitude = (latMin + latMax) / 2;
        double longitude = (lonMin + lonMax) / 2;
        return new double[]{latitude, longitude};
    }

    /**
     * Get the neighboring geohashes for a given geohash.
     * Used for querying danger zones in adjacent cells (important for border cases).
     *
     * @param geohash The center geohash
     * @return Array of 9 geohashes (center + 8 neighbors)
     */
    public static String[] getNeighbors(String geohash) {
        double[] center = decode(geohash);
        int precision = geohash.length();

        // Calculate cell size based on precision
        double latDelta = 180.0 / Math.pow(2, (5 * precision) / 2);
        double lonDelta = 360.0 / Math.pow(2, (5 * precision + 1) / 2);

        String[] neighbors = new String[9];
        int index = 0;

        for (int dlat = -1; dlat <= 1; dlat++) {
            for (int dlon = -1; dlon <= 1; dlon++) {
                double lat = center[0] + dlat * latDelta;
                double lon = center[1] + dlon * lonDelta;

                // Clamp values
                lat = Math.max(-90, Math.min(90, lat));
                lon = Math.max(-180, Math.min(180, lon));

                neighbors[index++] = encode(lat, lon, precision);
            }
        }

        return neighbors;
    }

    /**
     * Get the time-of-day category for danger score calculation.
     * 
     * @param hour Hour of day (0-23)
     * @return Time category string
     */
    public static String getTimeOfDay(int hour) {
        if (hour >= 6 && hour < 12) return "MORNING";
        if (hour >= 12 && hour < 17) return "AFTERNOON";
        if (hour >= 17 && hour < 21) return "EVENING";
        return "NIGHT"; // 9 PM to 6 AM
    }
}
