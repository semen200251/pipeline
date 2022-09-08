package semen;

import com.java_polytech.pipeline_interfaces.*;
import semenReader.ConfigReader;
import semenReader.Grammar;
import semenReader.IConfigReader;


import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

public class Reader implements IReader {

    static final String BUFFERS_SIZE = "BUFFERS_SIZE";
    private static final int MAX_BUFFER_SIZE = 1000000;
    private static final int SIZE_OF_CHAR = 2;
    private static final int SIZE_OF_INT = 4;


    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE currentType;
    private InputStream inputStream;
    private byte[] buffer;
    private int bufferSize;
    private IConsumer consumer;

    @Override
    public RC setInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return RC.RC_READER_FAILED_TO_READ;
        }

        this.inputStream = inputStream;
        return RC.RC_SUCCESS;
    }

    @Override
    public RC run() {
        RC rc;
        try {
            while(inputStream.available() != 0) {
                if (inputStream.available() > bufferSize) {
                    buffer = new byte[bufferSize];
                } else {
                    buffer = new byte[inputStream.available()];
                }

                inputStream.read(buffer);
                if ((currentType.equals(TYPE.INT_ARRAY) && buffer.length % SIZE_OF_INT != 0)
                        || (currentType.equals(TYPE.CHAR_ARRAY) && buffer.length % SIZE_OF_CHAR != 0)) {
                    return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Read portion of bytes can't be converted to chosen type");
                }
                rc = consumer.consume();
                if (!rc.isSuccess()) {
                    return rc;
                }
            }
        } catch (Exception var2) {
            return RC.RC_READER_FAILED_TO_READ;
        }
        this.buffer=null;
        rc = this.consumer.consume();
        return !rc.isSuccess() ? rc : RC.RC_SUCCESS;
    }

    @Override
    public RC setConfig(String s) {
        IConfigReader config = new ConfigReader(RC.RCWho.READER, new Grammar(BUFFERS_SIZE));
        RC rc = config.readConfig(s);
        if (!rc.isSuccess()) {
            return rc;
        }

        try {
            bufferSize = Integer.parseInt(config.getParam(BUFFERS_SIZE));
        } catch (NumberFormatException e) {
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }
        if (bufferSize % SIZE_OF_INT != 0) {
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Size of buffer doesn't fit for ints");
        }
        if (bufferSize <= 0 || bufferSize > MAX_BUFFER_SIZE) {
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }


        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer consumer) {
        this.consumer = consumer;
        return consumer.setProvider(this);
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supportedTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        switch (type) {
            case BYTE_ARRAY:
                currentType = TYPE.BYTE_ARRAY;
                return new ByteMediator();
            case CHAR_ARRAY:
                currentType = TYPE.CHAR_ARRAY;
                return new CharMediator();
            case INT_ARRAY:
                currentType = TYPE.INT_ARRAY;
                return new IntMediator();
        }
        return null;
    }

    private class ByteMediator implements IMediator {

        @Override
        public Object getData() {
            if (buffer == null) {
                return null;
            }

            byte[] outputData = new byte[buffer.length];
            System.arraycopy(buffer, 0, outputData, 0, buffer.length);
            return outputData;
        }
    }

    private class IntMediator implements IMediator {

        @Override
        public Object getData() {
            if (buffer == null) {
                return null;
            }

            IntBuffer intBuffer = ByteBuffer.wrap(buffer).asIntBuffer();
            int[] outputData = new int[intBuffer.remaining()];
            intBuffer.get(outputData);
            return outputData;
        }
    }

    private class CharMediator implements IMediator {

        @Override
        public Object getData() {
            if (buffer == null) {
                return null;
            }

            return new String(buffer, StandardCharsets.UTF_8).toCharArray();
        }
    }

}

