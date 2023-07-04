package main;

import main.constants.NumericalConstants;
import main.domain.Parameters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DataProcess {
    public static Parameters initParams(String inputPath) throws IOException {
        Parameters parameters = new Parameters();

        readLocalFile(parameters, inputPath);

        calculateDistance(parameters);

        return parameters;
    }

    public static void calculateDistance(Parameters parameters) {
        parameters.distanceOriginal = new double[parameters.customerNum + 2][parameters.customerNum + 2];
        parameters.cost = new double[parameters.customerNum + 2][parameters.customerNum + 2];
        parameters.distance = new double[parameters.customerNum + 2][parameters.customerNum + 2];
        parameters.travelTime = new double[parameters.customerNum + 2][parameters.customerNum + 2];

        double max;
        parameters.maxLength = 0.0;
        for (int i = 0; i < parameters.customerNum + 2; i++) {
            max = 0.0;
            for (int j = 0; j < parameters.customerNum + 2; j++) {
                parameters.distanceOriginal[i][j] = ((int) (10 * Math
                        .sqrt((parameters.positionX.get(i) - parameters.positionX.get(j)) * (parameters.positionX.get(i) - parameters.positionX.get(j))
                                + (parameters.positionY.get(i) - parameters.positionY.get(j)) * (parameters.positionY.get(i) - parameters.positionY.get(j))))) / 10.0;
                // truncate to get the same results as in Solomon
                if (max < parameters.distanceOriginal[i][j])
                    max = parameters.distanceOriginal[i][j];
            }
            parameters.maxLength += max;
        }
        for (int i = 0; i < parameters.customerNum + 2; i++) {
            parameters.distanceOriginal[i][0] = NumericalConstants.veryBigNumber;
            parameters.distanceOriginal[parameters.customerNum + 1][i] = NumericalConstants.veryBigNumber;
            parameters.distanceOriginal[i][i] = NumericalConstants.veryBigNumber;
        }
        for (int i = 0; i < parameters.customerNum + 2; i++)
            System.arraycopy(parameters.distanceOriginal[i], 0, parameters.distance[i], 0, parameters.customerNum + 2);


        // time windows
        for (int i = 0; i < parameters.customerNum + 2; i++)
            for (int j = 0; j < parameters.customerNum + 2; j++)
                parameters.travelTime[i][j] = parameters.distanceOriginal[i][j] / parameters.speed;

        for (int j = 0; j < parameters.customerNum + 2; j++) {
            parameters.cost[0][j] = parameters.distance[0][j];
            parameters.cost[j][parameters.customerNum + 1] = parameters.distance[j][parameters.customerNum + 1];
        }

        // cost for the other edges are defined during column generation
        parameters.edges = new double[parameters.customerNum + 2][parameters.customerNum + 2];
    }

    public static void readLocalFile(Parameters parameters, String inputPath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputPath));

        String line = "";

        for (int i = 0; i < 5; i++)
            line = br.readLine();

        String[] tokens = line.split("\\s+");
        parameters.vehicleNum = Integer.parseInt(tokens[1]);
        parameters.capacity = Integer.parseInt(tokens[2]);

        for (int i = 0; i < 4; i++)
            br.readLine();

        line = br.readLine();
        while (line != null) {
            tokens = line.split("\\s+");
            if (tokens.length == 0) break;
            parameters.customerIndex.add(tokens[1]);
            parameters.positionX.add(Double.parseDouble(tokens[2]));
            parameters.positionY.add(Double.parseDouble(tokens[3]));
            parameters.demand.add(Double.parseDouble(tokens[4]));
            parameters.readyTime.add(Integer.parseInt(tokens[5]));
            int dueTimeTmp = Integer.parseInt(tokens[6]);
            int serviceTimeTmp = Integer.parseInt(tokens[7]);
            // check if the service should be done before due time
            if (parameters.serviceInTW)
                dueTimeTmp -= serviceTimeTmp;
            parameters.dueTime.add(dueTimeTmp);
            parameters.serviceTime.add(serviceTimeTmp);
            line = br.readLine();
        }
        br.close();
        parameters.customerNum = parameters.customerIndex.size()-1;
        // second depot : copy of the first one for arrival
        parameters.customerIndex.add(parameters.customerIndex.get(0));
        parameters.demand.add(0.0);
        parameters.readyTime.add(parameters.readyTime.get(0));
        parameters.dueTime.add(parameters.dueTime.get(0));
        parameters.serviceTime.add(0);
        parameters.positionX.add(parameters.positionX.get(0));
        parameters.positionY.add(parameters.positionY.get(0));
    }
}
