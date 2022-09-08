package semenReader;

import java.util.Arrays;
import java.util.List;

public class Grammar implements IGrammar {
    private final List<String> lexemes;

    public Grammar(String... lexemes) {
        this.lexemes = Arrays.asList(lexemes);
    }

    @Override
    public int getSize() {
        return lexemes.size();
    }

    @Override
    public boolean contains(String lexeme) {
        return lexemes.contains(lexeme);
    }
}