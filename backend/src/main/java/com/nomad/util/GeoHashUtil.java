package com.nomad.util;

import ch.hsr.geohash.GeoHash;

public class GeoHashUtil {

    public static String encode(double lat, double lng, int precision) {
        return GeoHash.geoHashStringWithCharacterPrecision(lat, lng, precision);
    }

    public static String generateCacheKey(double lat, double lng, String category) {
        String geohash = encode(lat, lng, 5);
        return geohash + ":" + (category != null ? category : "all");
    }
}
