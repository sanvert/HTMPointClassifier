package edu.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

import java.util.List;

public class Tweet {
    @Expose
    private String id;

    @Expose
    private String city;

    private String text;

    @Expose
    private List<Double> coordinates;

    public Tweet(final String id, final String city, final String text, final List<Double> coordinates) {
        this.id = id;
        this.city = city;
        this.text = text;
        this.coordinates = coordinates;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(final List<Double> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Tweet{");
        sb.append("id='").append(id).append('\'');
        sb.append(", city='").append(city).append('\'');
        sb.append(", text='").append(text).append('\'');
        sb.append(", coordinates=").append(coordinates);
        sb.append('}');
        return sb.toString();
    }
}
