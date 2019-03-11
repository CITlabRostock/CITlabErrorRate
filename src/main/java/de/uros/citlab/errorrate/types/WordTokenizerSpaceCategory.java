package de.uros.citlab.errorrate.types;

import eu.transkribus.interfaces.ITokenizer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class WordTokenizerSpaceCategory implements ITokenizer {

    @Override
    public List<String> tokenize(String s) {
        LinkedList<String> ll = new LinkedList<>();
        String[] split = s.split("\n");
        for (int i = 0; i < split.length; i++) {
            ll.addAll(Arrays.asList(split[i].split("\\s")));
            ll.add("\n");
        }
        ll.removeLast();
        return ll;
    }

}
