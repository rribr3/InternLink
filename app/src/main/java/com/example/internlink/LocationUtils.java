package com.example.internlink;

import com.google.android.gms.maps.model.LatLng;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple utility class for Lebanon location coordinates
 */
public class LocationUtils {

    private static final Map<String, LatLng> LEBANON_LOCATIONS = new HashMap<String, LatLng>() {{
        // Major cities
        put("beirut", new LatLng(33.8938, 35.5018));
        put("tripoli", new LatLng(34.4363, 35.8497));
        put("sidon", new LatLng(33.5563, 35.3781));
        put("tyre", new LatLng(33.2704, 35.2038));
        put("zahle", new LatLng(33.8469, 35.9019));
        put("baalbek", new LatLng(34.0058, 36.2081));
        put("jounieh", new LatLng(33.9808, 35.6178));
        put("byblos", new LatLng(34.1208, 35.6481));
        put("nabatieh", new LatLng(33.3781, 35.4839));
        put("batroun", new LatLng(34.2556, 35.6586));

        // Regions/Governorates
        put("lebanon", new LatLng(33.8547, 35.8623)); // Center of Lebanon
        put("mount lebanon", new LatLng(33.8369, 35.5444));
        put("north lebanon", new LatLng(34.4363, 35.8497));
        put("south lebanon", new LatLng(33.5563, 35.3781));
        put("bekaa", new LatLng(33.8469, 35.9019));
        put("akkar", new LatLng(34.5481, 36.0781));

        // Districts
        put("hamra", new LatLng(33.8998, 35.4850));
        put("achrafieh", new LatLng(33.8869, 35.5131));
        put("verdun", new LatLng(33.8704, 35.4838));
        put("kaslik", new LatLng(33.9700, 35.6100));
        put("antelias", new LatLng(33.9072, 35.5931));
        put("jezzine", new LatLng(33.5450, 35.5856));
        put("marjeyoun", new LatLng(33.3631, 35.5919));
        put("halba", new LatLng(34.5481, 36.0781));
        put("zgharta", new LatLng(34.3969, 35.8650));
        put("bcharre", new LatLng(34.2514, 36.0131));
    }};

    /**
     * Get coordinates from location string
     */
    public static LatLng getCoordinatesFromString(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        String locationLower = location.toLowerCase().trim();

        // Direct match first
        LatLng coordinates = LEBANON_LOCATIONS.get(locationLower);
        if (coordinates != null) {
            return coordinates;
        }

        // Partial match - check if location contains any of our known locations
        for (Map.Entry<String, LatLng> entry : LEBANON_LOCATIONS.entrySet()) {
            String knownLocation = entry.getKey();

            // Check if the input contains the known location
            if (locationLower.contains(knownLocation) || knownLocation.contains(locationLower)) {
                return entry.getValue();
            }
        }

        // If no match found, return default Lebanon center
        return LEBANON_LOCATIONS.get("lebanon");
    }

    /**
     * Get coordinates for governorate, district, city
     */
    public static LatLng getCoordinates(String governorate, String district, String city) {
        // Try city first
        if (city != null) {
            LatLng coords = getCoordinatesFromString(city);
            if (coords != null) return coords;
        }

        // Try district
        if (district != null) {
            LatLng coords = getCoordinatesFromString(district);
            if (coords != null) return coords;
        }

        // Try governorate
        if (governorate != null) {
            LatLng coords = getCoordinatesFromString(governorate);
            if (coords != null) return coords;
        }

        // Default to Lebanon center
        return LEBANON_LOCATIONS.get("lebanon");
    }

    /**
     * Add more locations dynamically
     */
    public static void addLocation(String name, double latitude, double longitude) {
        LEBANON_LOCATIONS.put(name.toLowerCase(), new LatLng(latitude, longitude));
    }
}