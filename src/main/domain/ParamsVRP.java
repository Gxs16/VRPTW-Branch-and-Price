package main.domain;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * this class contains the inputs and methods to read the inputs for the Branch and Price CVRP with TW
 */
public class ParamsVRP {
    public int mvehic;
    public int clientsNum;
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
    public double[] positionX;
    public double[] positionY;
    /**
     * demand
     */
    public double[] d;
    /**
     * time windows: a=early, b=late, s=service
     */
    public int[] a;
    /**
     * time windows: a=early, b=late, s=service
     */
    public int[] b;
    /**
     * time windows: a=early, b=late, s=service
     */
    public int[] s;
    public double veryBigNumber;
    public double speed;
    public double gap;
    public double maxLength;
    public boolean serviceInTW;
    String[] citiesLab;

    private final Logger logger = Logger.getLogger(ParamsVRP.class.getSimpleName());

    public ParamsVRP() {
        gap = 0.00000000001;
        serviceInTW = false;
        clientsNum = 25;
        speed = 1;
        mvehic = 0;
        veryBigNumber = 1E10;
    }

    public void initParams(String inputPath) {
        int i, j;

        try {

            BufferedReader br = new BufferedReader(new FileReader(inputPath));

            String line = "";

            // //////////////////////////
            // for local file system
            // BufferedReader br = new BufferedReader(new FileReader(inputPath));

            for (i = 0; i < 5; i++)
                line = br.readLine();

            String[] tokens = line.split("\\s+");
            mvehic = Integer.parseInt(tokens[1]);
            capacity = Integer.parseInt(tokens[2]);

            citiesLab = new String[clientsNum + 2];
            d = new double[clientsNum + 2];
            a = new int[clientsNum + 2];
            b = new int[clientsNum + 2];
            s = new int[clientsNum + 2];
            positionX = new double[clientsNum + 2];
            positionY = new double[clientsNum + 2];
            distanceOriginal = new double[clientsNum + 2][clientsNum + 2];
            cost = new double[clientsNum + 2][clientsNum + 2];
            distance = new double[clientsNum + 2][clientsNum + 2];
            travelTime = new double[clientsNum + 2][clientsNum + 2];

            for (i = 0; i < 4; i++)
                br.readLine();

            for (i = 0; i < clientsNum + 1; i++) {
                line = br.readLine();
                //System.out.println(line);
                tokens = line.split("\\s+");
                citiesLab[i] = tokens[1]; // customer number
                positionX[i] = Double.parseDouble(tokens[2]); // x coordinate
                positionY[i] = Double.parseDouble(tokens[3]); // y coordinate
                d[i] = Double.parseDouble(tokens[4]);
                a[i] = Integer.parseInt(tokens[5]); // ready time
                b[i] = Integer.parseInt(tokens[6]); // due time
                s[i] = Integer.parseInt(tokens[7]); // service
                // check if the service should be done before due time
                if (serviceInTW)
                    b[i] -= s[i];
            }
            br.close();

            // second depot : copy of the first one for arrival
            citiesLab[clientsNum + 1] = citiesLab[0];
            d[clientsNum + 1] = 0.0;
            a[clientsNum + 1] = a[0];
            b[clientsNum + 1] = b[0];
            s[clientsNum + 1] = 0;
            positionX[clientsNum + 1] = positionX[0];
            positionY[clientsNum + 1] = positionY[0];

            // ---- distances
            double max;
            maxLength = 0.0;
            for (i = 0; i < clientsNum + 2; i++) {
                max = 0.0;
                for (j = 0; j < clientsNum + 2; j++) {
                    distanceOriginal[i][j] = ((int) (10 * Math
                            .sqrt((positionX[i] - positionX[j]) * (positionX[i] - positionX[j])
                                    + (positionY[i] - positionY[j]) * (positionY[i] - positionY[j])))) / 10.0;
                    // truncate to get the same results as in Solomon
                    if (max < distanceOriginal[i][j])
                        max = distanceOriginal[i][j];
                }
                maxLength += max; // a Route.java with a length longer than this is not
                // possible (we need it to check the feasibility of
                // the Column Gen sol.
            }
            for (i = 0; i < clientsNum + 2; i++) {
                distanceOriginal[i][0] = veryBigNumber;
                distanceOriginal[clientsNum + 1][i] = veryBigNumber;
                distanceOriginal[i][i] = veryBigNumber;
            }
            /*
             * for(i = 0; i < 20; i++)
             *   distBase[10][i] = verybig;
             * for(i = 21; i < nbclients+2; i++)
             *   distBase[10][i] = verybig;
             * for(i = 0; i < 10; i++)
             *   distBase[i][20] = verybig;
             * for(i = 11; i < nbclients+2; i++)
             *   distBase[i][20] = verybig;
             * distBase[20][10] = verybig;
             */
            for (i = 0; i < clientsNum + 2; i++)
                for (j = 0; j < clientsNum + 2; j++) {
                    distance[i][j] = distanceOriginal[i][j];
                }


            // ---- time
            for (i = 0; i < clientsNum + 2; i++)
                for (j = 0; j < clientsNum + 2; j++)
                    travelTime[i][j] = distanceOriginal[i][j] / speed;

            for (j = 0; j < clientsNum + 2; j++) {
                cost[0][j] = distance[0][j];
                cost[j][clientsNum + 1] = distance[j][clientsNum + 1];
            }
            // cost for the other edges are defined during column generation

        } catch (IOException e) {
            logger.severe("Error: " + e);
        }

        edges = new double[clientsNum + 2][clientsNum + 2];

    }
}