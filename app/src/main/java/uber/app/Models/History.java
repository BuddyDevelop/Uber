package uber.app.Models;

import java.util.List;

public class History {
    private String customerId;
    private String driverId;
    private int rating;
    private String timestamp;
    private List<Double> fromLatLng;
    private List<Double> toLatLng;

    public History() {}

    public History( String customerId, String driverId, int rating ) {
        this.customerId = customerId;
        this.driverId = driverId;
        this.rating = rating;
    }

    public History( String customerId, String driverId, int rating, String timestamp ) {
        this.customerId = customerId;
        this.driverId = driverId;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public History( String customerId, String driverId, int rating, String timestamp, List<Double> fromLatLng, List<Double> toLatLng ) {
        this.customerId = customerId;
        this.driverId = driverId;
        this.rating = rating;
        this.timestamp = timestamp;
        this.fromLatLng = fromLatLng;
        this.toLatLng = toLatLng;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId( String customerId ) {
        this.customerId = customerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId( String driverId ) {
        this.driverId = driverId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating( int rating ) {
        this.rating = rating;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp( String timestamp ) {
        this.timestamp = timestamp;
    }

    public List<Double> getFromLatLng() { return fromLatLng; }

    public void setFromLatLng( List<Double> fromLatLng ) { this.fromLatLng = fromLatLng; }

    public List<Double> getToLatLng() { return toLatLng; }

    public void setToLatLng( List<Double> toLatLng ) { this.toLatLng = toLatLng; }
}
