package main.domain;


import java.util.ArrayList;
import java.util.List;

/**
 * this class contains the inputs and methods to read the inputs for the Branch and Price CVRP with TW
 */
public class Parameters {
    public int vehicleNum;
    public int customerNum;
    public int capacity;
    /**
     * original distances for the Branch and Bound
     */
    public double[][] distanceOriginal;
    public double[][] travelTime;
    public List<Double> positionX = new ArrayList<>();
    public List<Double> positionY = new ArrayList<>();
    public List<Double> demand = new ArrayList<>();
    public List<Integer> readyTime = new ArrayList<>();
    public List<Integer> dueTime = new ArrayList<>();
    public List<Integer> serviceTime = new ArrayList<>();
    public double speed;
    /**
     * a Route.java with a length longer than this is not possible (we need it to check the feasibility of the Column Gen solution)
     */
    public double maxLength;
    public boolean serviceInTW;
    public List<String> customerIndex = new ArrayList<>();

    public Parameters() {
        this.serviceInTW = false;
        this.customerNum = 0;
        this.speed = 1;
        this.vehicleNum = 0;
    }



}