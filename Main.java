import com.java_polytech.pipeline_interfaces.RC;



import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
    private static final int ARG_NUM_FOR_FILENAME = 0;
    private static final String LOGGER_NAME = "Logger";
    private static final String LOG_OUT_FILENAME = "log.txt";

    private static Logger createLogger() throws IOException {
        Logger logger = Logger.getLogger(LOGGER_NAME);
        FileHandler fileHandler;
        fileHandler = new FileHandler(LOG_OUT_FILENAME);
        logger.addHandler(fileHandler);
        SimpleFormatter formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);
        logger.setUseParentHandlers(false);

        return logger;
    }

    public static void main(String[] args) {
        Logger logger;
        try {
            logger = createLogger();
        } catch (IOException e) {
            System.out.println("Error: Can't create logger.");
            return;
        }

        String message;
        boolean error = false;
        if (args[ARG_NUM_FOR_FILENAME] != null) {
            Manager manager = new Manager(logger);
            RC rc = manager.run(args[ARG_NUM_FOR_FILENAME]);

            message = rc.info;
            if (!rc.isSuccess()) {
                message = "Error: " + rc.who.get() + ": " + rc.info;
                error = true;
            }
        } else {
            message = RC.RC_MANAGER_INVALID_ARGUMENT.info;
            error = true;
        }

        if (error) {
            logger.log(Level.SEVERE, message);
        } else {
            logger.log(Level.INFO, message);
        }

        System.out.println(message);
    }
}
