package com.example.java.delivery.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KakaoMapService {

    @Value("${kakao.map.api-key:}")
    private String apiKey;

    /**
     * Calculates the distance between two coordinates in meters.
     * Uses Kakao Mobility Directions API if API key is provided, falls back to Haversine straight-line distance.
     */
    public double getDrivingDistanceMeters(double startLat, double startLon, double endLat, double endLon) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                return callKakaoDirectionsApi(startLat, startLon, endLat, endLon);
            } catch (Exception e) {
                // Fallback to Haversine straight-line distance
                return calculateHaversineDistance(startLat, startLon, endLat, endLon);
            }
        }
        return calculateHaversineDistance(startLat, startLon, endLat, endLon);
    }

    private double callKakaoDirectionsApi(double startLat, double startLon, double endLat, double endLon) throws Exception {
        String origin = startLon + "," + startLat;
        String destination = endLon + "," + endLat;
        String urlString = "https://apis-navi.kakaomobility.com/v1/directions"
                         + "?origin=" + origin
                         + "&destination=" + destination;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "KakaoAK " + apiKey);
        conn.setRequestProperty("Content-type", "application/json");

        if (conn.getResponseCode() == 200) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();

            String response = sb.toString();
            // Parse distance from response e.g., "distance": 12345
            int distanceIndex = response.indexOf("\"distance\":");
            if (distanceIndex != -1) {
                int start = distanceIndex + 11;
                int end = response.indexOf(",", start);
                if (end == -1) {
                    end = response.indexOf("}", start);
                }
                String distanceStr = response.substring(start, end).trim();
                return Double.parseDouble(distanceStr);
            }
        }
        conn.disconnect();
        throw new RuntimeException("Kakao API response failed");
    }

    /**
     * Haversine formula calculation for straight-line distance.
     */
    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371e3; // Earth radius in meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // in meters
    }
}
