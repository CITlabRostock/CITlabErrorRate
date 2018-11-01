package de.uros.citlab.errorrate.htr.end2end;

class Voter {
    boolean isSpace(String s) {
        return " ".equals(s);
    }

    boolean isLineBreak(String s) {
        return "\n".equals(s);
    }

    boolean isLineBreakOrSpace(String s) {
        return isSpace(s) || isLineBreak(s);
    }
}
