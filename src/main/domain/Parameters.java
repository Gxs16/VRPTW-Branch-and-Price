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
     * for the ShortestPathWithRC sub problem
     */
    public double[][] cost;
    /**
     * original distances for the Branch and Bound
     */
    public double[][] distanceOriginal;
    /**
     * distances that will be updated during the B&B before being used in the CG & ShortestPathWithRC
     */
    public double[][] distance;
    public double[][] travelTime;
    /**
     * weight of each edge during branch and bound
     */
    public double[][] edges;
    public List<Double> positionX = new ArrayList<>();
    public List<Double> positionY = new ArrayList<>();
    public List<Double> demand = new ArrayList<>();
    public List<Integer> readyTime = new ArrayList<>();
    public List<Integer> dueTime = new ArrayList<>();
    public List<Integer> serviceTime = new ArrayList<>();
    public double veryBigNumber;
    public double speed;
    public double gap;
    public double maxLength;
    public boolean serviceInTW;
    public List<String> customerIndex = new ArrayList<>();

    public Parameters() {
        gap = 0.00000000001;
        serviceInTW = false;
        this.customerNum = 0;
        speed = 1;
        vehicleNum = 0;
        veryBigNumber = 1E10;
    }

}