package semenReader;

import com.java_polytech.pipeline_interfaces.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigReader implements IConfigReader {
    private static final int PARAM_NAME_IND = 0;
    private static final int PARAM_IND = 1;
    private static final String COMMENTS_REGULAR = "#.*";
    private static final String SPACES_REGULAR = "\\s+";
    private static final String SPLITTER_STRING = "=";
    private static final String EMPTY_STRING = "";

    private final RC.RCWho who;
    private final IGrammar grammar;
    private final Map<String, String> params = new HashMap<>();

    public ConfigReader(RC.RCWho who, IGrammar grammar) {
        this.who = who;
        this.grammar = grammar;
    }

    @Override
    public RC readConfig(String filename) {
        RC rc = RC.RC_SUCCESS;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String readLine;
            while ((readLine = reader.readLine()) != null) {
                readLine = readLine.replaceAll(COMMENTS_REGULAR, EMPTY_STRING)
                        .replaceAll(SPACES_REGULAR, EMPTY_STRING);

                if (!readLine.isEmpty()) {
                    rc = setParam(readLine);
                }

                if (!rc.isSuccess()) {
                    return rc;
                }
            }
        } catch (IOException e) {
            return new RC(who, RC.RCType.CODE_CONFIG_FILE_ERROR, "Can't open grammar file.");
        }

        if (params.size() != grammar.getSize()) {
            rc = new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Not enough params in config file.");
        }

        return rc;
    }

    @Override
    public String getParam(String lexeme) {
        return params.get(lexeme);
    }

    private RC setParam(String paramStr) {
        String[] paramSet = paramStr.split(SPLITTER_STRING);

        if (grammar.contains(paramSet[PARAM_NAME_IND])) {
            params.put(paramSet[PARAM_NAME_IND], paramSet[PARAM_IND]);
            return RC.RC_SUCCESS;
        }

        return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Unknown parameter in grammar file.");
    }
}