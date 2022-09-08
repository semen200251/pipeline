

import com.java_polytech.pipeline_interfaces.*;
import semenReader.ConfigReader;
import semenReader.Grammar;
import semenReader.IConfigReader;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Manager {
    private static final String INPUT_FILE_STRING = "inputFile";
    private static final String OUTPUT_FILE_STRING = "outputFile";
    private static final String READER_CONFIG_FILE_STRING = "readerConfigFile";
    private static final String WRITER_CONFIG_FILE_STRING = "writerConfigFile";
    private static final String EXECUTOR_CONFIG_FILE_LIST_STRING = "executor_config_file_list";
    private static final String READER_CLASS_STRING = "readerClassName";
    private static final String WRITER_CLASS_STRING = "writerClassName";
    private static final String EXECUTOR_CLASS_LIST_STRING = "executor_class_list";

    private static final String SPLITTER_FOR_EXECUTORS = ",";
    private static final String SPACE = " ";
    private static final String EMPTY_STRING = "";

    private final Logger logger;
    private IConfigReader config;

    private InputStream inputStream;
    private OutputStream outputStream;

    private IReader reader;
    private IWriter writer;
    private IExecutor[] executors;

    public Manager(Logger logger) {
        this.logger = logger;
    }

    public RC run(String configFilename) {
        RC rc = setConfig(configFilename);
        if (!rc.isSuccess()) {
            return rc;
        }
        StringBuilder message;
        logger.log(Level.INFO, "Config was successfully read \n");
        if (!(rc = openStreams()).isSuccess()) {
            return rc;
        }
        if (!(rc = setParticipants()).isSuccess()) {
            return rc;
        }
        if (!(rc = reader.setConsumer(executors[0])).isSuccess()) {
            return rc;
        }
        for (int i = 0; i < executors.length - 1; i++) {
            if (!(rc = executors[i].setConsumer(executors[i + 1])).isSuccess()) {
                return rc;
            }
        }
        if (!(rc = executors[executors.length - 1].setConsumer(writer)).isSuccess()) {
            return rc;
        }
        if (!(rc = reader.setConfig(config.getParam(READER_CONFIG_FILE_STRING))).isSuccess()) {
            return rc;
        }
        if (!(rc = setExecutorsConfigs()).isSuccess()) {
            return rc;
        }
        if (!(rc = writer.setConfig(config.getParam(WRITER_CONFIG_FILE_STRING))).isSuccess()) {
            return rc;
        }
        if (!(rc = reader.setInputStream(inputStream)).isSuccess()) {
            return rc;
        }
        if(!(rc = writer.setOutputStream(outputStream)).isSuccess()){
            return rc;
        }

        message = new StringBuilder("Pipeline was successfully built \n");
        for (IExecutor executor : executors) {
            message.append(executor).append("\n");
        }
        logger.log(Level.INFO, message.toString());

        rc = reader.run();
        if (!rc.isSuccess()) {
            return rc;
        }

        return closeStreams();
    }

    private RC setConfig(String s) {
        config = new ConfigReader(RC.RCWho.MANAGER,
                new Grammar(INPUT_FILE_STRING, OUTPUT_FILE_STRING, READER_CONFIG_FILE_STRING, WRITER_CONFIG_FILE_STRING,
                        EXECUTOR_CONFIG_FILE_LIST_STRING, READER_CLASS_STRING, WRITER_CLASS_STRING, EXECUTOR_CLASS_LIST_STRING
                ));
        return config.readConfig(s);
    }

    private RC openStreams() {
        try {
            inputStream = new FileInputStream(config.getParam(INPUT_FILE_STRING));
        } catch (FileNotFoundException ex) {
            return RC.RC_MANAGER_INVALID_INPUT_FILE;
        }
        try {
            outputStream = new FileOutputStream(config.getParam(OUTPUT_FILE_STRING));
        } catch (IOException ex) {
            return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
        }
        logger.log(Level.INFO, "Streams were successfully opened");
        return RC.RC_SUCCESS;
    }

    private RC closeStreams() {
        boolean isClosed = true;
        try {
            inputStream.close();
        } catch (IOException ex) {
            isClosed = false;
        }
        try {
            outputStream.close();
        } catch (IOException ex) {
            isClosed = false;
        }

        if (!isClosed) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Error during closing stream");
        }
        logger.log(Level.INFO, "Streams were successfully closed");
        return RC.RC_SUCCESS;
    }

    private RC setParticipants() {
        reader = (IReader) getClassByName(config.getParam(READER_CLASS_STRING), IReader.class);
        if (reader == null) {
            return RC.RC_MANAGER_INVALID_READER_CLASS;
        }

        writer = (IWriter) getClassByName(config.getParam(WRITER_CLASS_STRING), IWriter.class);
        if (writer == null) {
            return RC.RC_MANAGER_INVALID_WRITER_CLASS;
        }

        executors = getExecutors(config.getParam(EXECUTOR_CLASS_LIST_STRING));
        if (executors == null) {
            return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
        }
        logger.log(Level.INFO, "Participants were successfully added");
        return RC.RC_SUCCESS;
    }


    private IExecutor[] getExecutors(String executorsName) {
        executorsName = executorsName.replaceAll(SPACE, EMPTY_STRING);
        String[] splitedNames = executorsName.split(SPLITTER_FOR_EXECUTORS);
        IExecutor[] executors = new IExecutor[splitedNames.length];
        for (int i = 0; i < executors.length; i++) {
            executors[i] = (IExecutor) getClassByName(splitedNames[i], IExecutor.class);
            if (executors[i] == null) {
                return null;
            }
        }
        return executors;
    }


    private RC setExecutorsConfigs() {
        String executorsConfigName = config.getParam(EXECUTOR_CONFIG_FILE_LIST_STRING).replaceAll(SPACE, EMPTY_STRING);
        String[] splitedNames = executorsConfigName.split(SPLITTER_FOR_EXECUTORS);
        if (splitedNames.length != executors.length) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "The count of configs for executors not equal to count of classes");
        }

        RC rc = RC.RC_SUCCESS;
        for (int i = 0; i < splitedNames.length; i++) {
            rc = executors[i].setConfig(splitedNames[i]);
            if (!rc.isSuccess()) {
                return rc;
            }
        }
        return rc;
    }

    private Object getClassByName(String className, Class<?> inter) {
        Object tmp = null;
        try {
            Class<?> clazz = Class.forName(className);
            if (inter.isAssignableFrom(clazz)) {
                tmp = clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            return null;
        }

        return tmp;
    }
}
