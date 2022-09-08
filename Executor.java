package semen;

import com.java_polytech.pipeline_interfaces.*;
import semenReader.ConfigReader;
import semenReader.Grammar;
import semenReader.IConfigReader;

import java.util.Objects;


public class Executor implements IExecutor {

    private static final int SIZE_OF_INT = 4;
    private static final int MAX_BUFFER_SIZE = 1000000;
    private static final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    private byte[] bufferCod;
    private TYPE currentType;
    private IMediator mediator;
    String COD;
    String subFilename;
    private final static int firstArray=0;
    private final static int secondArray=1;
    int writerBufSize;
    private final static int error=-1;
    private final static String YES="YES";
    private final static String NO="NO";
    private final static String SubTable="SubTable";
    private final static String WriterBufferSize="WriterBufferSize";
    private final static String CODName="COD";
    int countCodingSimbs=0;
    IConsumer consumer;
    Byte[][] subTable;
    InitSubTable initSubTable;

    @Override
    public RC setConfig(String s) {
        IConfigReader config = new ConfigReader(RC.RCWho.EXECUTOR, new Grammar(SubTable, WriterBufferSize, CODName));
        RC rc = config.readConfig(s);
        if (!rc.isSuccess()) {
            return rc;
        }
        try {
            writerBufSize = Integer.parseInt(config.getParam(WriterBufferSize));
        } catch (NumberFormatException e) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        if (writerBufSize <= 0 || writerBufSize > MAX_BUFFER_SIZE) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        if (writerBufSize % SIZE_OF_INT != 0) {
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Size of buffer doesn't fit for ints");
        }
        subFilename=config.getParam(SubTable);

        initSubTable = new InitSubTable(subFilename);
        if(initSubTable.getStatus()==error){
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        initSubTable.readSubFile();
        if(initSubTable.getStatus()==error){
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        initSubTable.CloseString();
        if(initSubTable.getStatus()==error){
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }

        COD=config.getParam(CODName);
        if(Objects.equals(COD,YES)) {
            subTable= initSubTable.subTableforcoding;
        }else if(Objects.equals(COD,NO)){
            subTable= initSubTable.subTablefordecoding;
        }else{
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }

        if(subTable==null){
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        return RC.RC_SUCCESS;
    }

    @Override
    public RC consume() {
        byte[] data = (byte[]) mediator.getData();
        RC rc = RC.RC_SUCCESS;
        if (data == null) {
            if(countCodingSimbs==0) {
                bufferCod = null;
                rc=consumer.consume();
            }else{
                if(countCodingSimbs!=writerBufSize) {
                    byte[] lastBufCod = new byte[countCodingSimbs];
                    for (int i = 0; i < countCodingSimbs; i++) {
                        lastBufCod[i] = bufferCod[i];
                    }
                    bufferCod=lastBufCod;
                    rc=consumer.consume();
                    if (!rc.isSuccess()) {
                        return rc;
                    }
                    bufferCod=null;
                    rc=consumer.consume();
                }else{
                    rc=consumer.consume();
                    if (!rc.isSuccess()) {
                        return rc;
                    }
                    bufferCod=null;
                    rc=consumer.consume();
                }
            }
            return rc;
        }else{
            if(bufferCod==null)
            {
                bufferCod=new byte[writerBufSize];
            }
            for (int i = 0; i < data.length; i++) {
                if (subTable[firstArray][data[i]]==null) {
                    bufferCod[countCodingSimbs] = data[i];
                    countCodingSimbs++;
                } else {
                    bufferCod[countCodingSimbs] = subTable[secondArray][subTable[firstArray][data[i]]];
                    countCodingSimbs++;
                }
                if(countCodingSimbs==writerBufSize){
                    consumer.consume();
                    bufferCod=new byte[writerBufSize];
                    countCodingSimbs=0;
                }
            }
            if(countCodingSimbs==writerBufSize) {
                rc=consumer.consume();
            }
            return rc;
        }
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
        if (type.equals(TYPE.BYTE_ARRAY)) {
            return new ByteMediator();
        }
        return null;
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

    private class ByteMediator implements IMediator {
        @Override
        public Object getData() {
            if (bufferCod == null) {
                return null;
            }

            byte[] outputData = new byte[bufferCod.length];
            System.arraycopy(bufferCod, 0, outputData, 0, bufferCod.length);
            return outputData;
        }
    }
}
