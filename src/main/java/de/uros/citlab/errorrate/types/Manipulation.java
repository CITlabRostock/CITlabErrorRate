package de.uros.citlab.errorrate.types;

public enum Manipulation {
    /**
     * Insertion: a token (from the reference) had to be inserted to come from recognition to reference
     * ==> Edit distance = 1
     */
    INS,
    /**
     * Deletion: a token (from the recognition) had to be deleted to come from recognition to reference
     * ==> Edit distance = 1
     */
    DEL,
    /**
     * Correct: the tokens of recognition and reference are equal
     * ==> Edit distance = 0
     */
    COR,
    /**
     * Substitution: a token (from the recognition) had to be substituted (by a token from the reference)
     * ==> Edit distance = 1
     */
    SUB;
}
