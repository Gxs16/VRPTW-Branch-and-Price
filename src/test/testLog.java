package test;

import java.util.logging.Logger;

public class testLog {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(testLog.class.getSimpleName());
        logger.info("Test info");
        logger.warning("Test warning");
        logger.severe("Test severe");
        logger.fine("Test fine");
        logger.config("Test config");
    }
}
