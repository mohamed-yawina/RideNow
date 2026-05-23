package com.example.ridenow;

public class RideRequest {
    private String id;
    private String clientName;
    private String destination;
    private String pickupAddress;
    private String vehicleType;
    private double offer;
    private double clientRating;
    private double distance;
    private String status;

    public RideRequest() {}

    public RideRequest(String id, String clientName, String destination, String vehicleType, double offer) {
        this.id = id;
        this.clientName = clientName;
        this.destination = destination;
        this.vehicleType = vehicleType;
        this.offer = offer;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public double getOffer() { return offer; }
    public void setOffer(double offer) { this.offer = offer; }

    public double getClientRating() { return clientRating; }
    public void setClientRating(double clientRating) { this.clientRating = clientRating; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}