package semen;

import com.java_polytech.pipeline_interfaces.RC;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

public class InitSubTable {
    private RandomAccessFile subFile;
    public Byte[][] subTableforcoding;
    public Byte[][] subTablefordecoding;
    private int countSets=2;
    private int countSimbs=256;
    private static final String marker="=";
    private boolean isBijection = true;
    private int powerFirst = 0;
    private int powerSecond = 0;
    private static final int positionCoding = 1;
    private static final int positionDecoding = 0;
    private static final int currentCountSimbsInStr = 3;

    private final static int error=-1;
    private int status=0;

    InitSubTable(String nameFile){
        try{
            subFile=new RandomAccessFile(nameFile, "r");
        }catch(FileNotFoundException ex){
            System.out.println("Opening of sub file is failed");
            status=error;
            return;
        }
        try {
            if (countingSimbs()<=0) {
                throw new IOException("Sub file is empty");
            }
        }catch (IOException ex){
            System.out.println(ex.getMessage());
            status=error;
            return;
        }
        try{
            subFile.seek(0);
        }catch (IOException ex){
            System.out.println("Error seek in sub File");
            status=error;
            return;
        }
        subTableforcoding=new Byte[countSets][countSimbs];
        subTablefordecoding=new Byte[countSets][countSimbs];
        for(int i=0;i<countSets;i++){
            for(int j=0;j<countSimbs;j++){
                subTableforcoding[i][j] = null;
                subTablefordecoding[i][j] = null;
            }
        }
        int k=0;
    }

    private int countingSimbs(){
        int countSimbols=0;
        String readedString=new String();
        while(readedString!=null){
            try{
                readedString=subFile.readLine();
            }catch (IOException ex) {
                System.out.println("Error read sub file");
            }
            countSimbols++;
        }
        return --countSimbols;
    }

    void readSubFile(){
        String scannedLine=new String();
        int checkError=0;
        while(scannedLine!=null){
            try{
                scannedLine= subFile.readLine();
                if(scannedLine==null){
                    break;
                }
            }catch (IOException ex){
                System.out.println("Scanned line from sub file failed");
                status=error;
                return;
            }
            checkError=setParams(scannedLine);
            if(checkError==error){
                System.out.println("Problems in sub Table");
                status=error;
                return;
            }
        }
        if(isBijection==false){
            System.out.println("Isn't Bijection");
            status=error;
            return;
        }
        return;
    }

    private int setParams(String scannedLine){
        final int nameDecod = 0;
        final int nameCod = 1;
        try {
            if (scannedLine.length()!=currentCountSimbsInStr) {
                throw new IOException("There isn't determination someone item");
            }
        }catch (IOException ex){
            System.out.println(ex.getMessage());
            return error;
        }
        String[] splitLine = scannedLine.split(marker);

        if(splitLine.length!=2){
            return error;
        }
        if (splitLine[nameDecod].length() != 0) {
            powerFirst++;
        }
        if (splitLine[nameCod].length() != 0) {
            powerSecond++;
        }

        splitLine[nameDecod].trim();
        splitLine[nameCod].trim();

        for (int i = 0; i < countSimbs; i++) {
            if (Objects.equals(subTableforcoding[positionDecoding][i], (byte) splitLine[nameDecod].charAt(0))) {
                isBijection = false;
                System.out.println("Isn't Bijection");
                return error;
            }
            if (Objects.equals(subTableforcoding[positionCoding][i], (byte) splitLine[nameCod].charAt(0))) {
                isBijection = false;
                System.out.println("Isn't Bijection");
                return error;
            }
        }
        if (powerFirst != powerSecond) {
            isBijection = false;
            System.out.println("Isn't Bijection");
            return error;
        }
        subTableforcoding[positionDecoding][(byte) splitLine[nameDecod].charAt(0)] = (byte) splitLine[nameDecod].charAt(0);
        subTableforcoding[positionCoding][(byte) splitLine[nameDecod].charAt(0)] = (byte) splitLine[nameCod].charAt(0);
        subTablefordecoding[positionDecoding][(byte) splitLine[nameCod].charAt(0)]=(byte) splitLine[nameDecod].charAt(0);
        subTablefordecoding[positionCoding][(byte) splitLine[nameCod].charAt(0)]=(byte) splitLine[nameCod].charAt(0);
        return 0;
    }

    public void CloseString(){
        try{
            subFile.close();
        }catch (IOException ex){
            System.out.println("Error close sub File");
        }
    }
    public int getStatus(){
        return status;
    }
}

