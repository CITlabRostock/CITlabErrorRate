package de.uros.citlab.errorrate.htr.end2end;

public class Voter {
    public boolean isSpace(String s) {
        return " ".equals(s);
    }

    public boolean isLineBreak(String s) {
        return "\n".equals(s);
    }

    public boolean isLineBreakOrSpace(String s) {
        return isSpace(s) || isLineBreak(s);
    }
}
