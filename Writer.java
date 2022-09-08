package semen;

import com.java_polytech.pipeline_interfaces.*;
import semenReader.ConfigReader;
import semenReader.Grammar;
import semenReader.IConfigReader;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Writer implements IWriter {
    private static final String BUFFERS_SIZE = "buffer_size";
    private static final int MAX_BUFFER_SIZE = 1000000;
    private static final int SIZE_OF_INT = 4;


    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE currentType;
    private IMediator mediator;
    private BufferedOutputStream outputStream;
    private int bufferSize;

    @Override
    public RC setOutputStream(OutputStream outputStream) {
        if (outputStream == null) {
            return RC.RC_WRITER_FAILED_TO_WRITE;
        }

        this.outputStream = new BufferedOutputStream(outputStream, bufferSize);
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConfig(String s) {
        IConfigReader config = new ConfigReader(RC.RCWho.WRITER, new Grammar(BUFFERS_SIZE));

        RC rc = config.readConfig(s);
        if (!rc.isSuccess())
            return rc;

        try {
            bufferSize = Integer.parseInt(config.getParam(BUFFERS_SIZE));
        } catch (NumberFormatException e) {
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
        }

        if (bufferSize <= 0 || bufferSize > MAX_BUFFER_SIZE) {
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
        }
        if (bufferSize % SIZE_OF_INT != 0) {
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Size of buffer doesn't fit for ints");
        }
        return RC.RC_SUCCESS;
    }

    @Override
    public RC consume() {
        byte[] dataBytes = getDataBytes();
        if (dataBytes == null) {
            try {
                outputStream.flush();
            }
            catch (IOException e) {
                return RC.RC_WRITER_FAILED_TO_WRITE;
            }
            return RC.RC_SUCCESS;
        }
        try {
            outputStream.write(dataBytes);
        }
        catch (IOException e) {
            return RC.RC_WRITER_FAILED_TO_WRITE;
        }
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setProvider(IProvider provider) {
        for (TYPE prType : provider.getOutputTypes()) {
            for (TYPE supType : supportedTypes) {
                if (prType.equals(supType)) {
                    currentType = prType;
                    break;
                }
            }
        }

        if (currentType == null) {
            return RC.RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;
        }

        mediator = provider.getMediator(currentType);
        return RC.RC_SUCCESS;
    }

    private byte[] getDataBytes() {
        Object data = mediator.getData();
        if (data == null) {
            return null;
        }

        switch (currentType) {
            case BYTE_ARRAY:
                return (byte[]) data;
            case CHAR_ARRAY:
                return new String((char[]) data).getBytes(StandardCharsets.UTF_8);
            case INT_ARRAY:
                ByteBuffer byteBuff = ByteBuffer.allocate(((int[]) data).length * SIZE_OF_INT);
                byteBuff.asIntBuffer().put((int[]) data);
                return byteBuff.array();
        }

        return null;
    }
}
