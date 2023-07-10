package main.domain;

import java.util.ArrayList;
import java.util.Collections;

public class Route implements Cloneable {
    public double cost;
    // first resource: cost (e.g. distance or strict travel time)
    public double quantity;

    public ArrayList<Integer> path;

    public Route() {
        this.path = new ArrayList<>();
        this.cost = 0.0;
    }

    /*
     * @update 2013. 6. 8
     * @modify Geunho Kim
     */
    // method for deep cloning
    public Route clone() throws CloneNotSupportedException {
        Route route = (Route) super.clone();
        route.path = (ArrayList<Integer>) path.clone();
        return route;
    }

    public void addCity(int city) {
        this.path.add(city);
    }

    public void setCost(double c) {
        this.cost = c;
    }

    public double getCost() {
        return this.cost;
    }

    public void setQuantity(double a) {
        this.quantity = a;
    }

    public double getQuantity() {
        return this.quantity;
    }

    public ArrayList<Integer> getPath() {
        return this.path;
    }

    public void reversePath() {
        Collections.reverse(this.path);
    }
}