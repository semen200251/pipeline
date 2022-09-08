package semenReader;

import com.java_polytech.pipeline_interfaces.*;

public interface IConfigReader {
    RC readConfig(String filename);
    String getParam(String lexeme);
}