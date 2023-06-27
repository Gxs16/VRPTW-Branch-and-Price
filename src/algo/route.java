package algo;

import java.util.ArrayList;

public class Route implements Cloneable {
    public double cost, Q;
    // first resource: cost (e.g. distance or strict travel time)

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

    public void setQ(double a) {
        this.Q = a;
    }

    public double getQ() {
        return this.Q;
    }

    public ArrayList<Integer> getPath() {
        return this.path;
    }

    public void switchPath() {
        Integer swap;
        int nb = path.size() / 2;
        for (int i = 0; i < nb; i++) {
            swap = path.get(i);
            path.set(i, path.get(path.size() - 1 - i));
            path.set(path.size() - 1 - i, swap);
        }
    }
}