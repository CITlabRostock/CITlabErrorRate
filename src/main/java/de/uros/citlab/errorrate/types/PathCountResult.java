package de.uros.citlab.errorrate.types;

import de.uros.citlab.errorrate.interfaces.ILineComparison;
import de.uros.citlab.errorrate.util.ObjectCounter;

import java.util.LinkedList;
import java.util.List;

public class PathCountResult {
    private ObjectCounter<Count> counter;
    private ObjectCounter<Substitution> substitutions;
    private List<ILineComparison> lineComparisons;

    public PathCountResult() {
        this(new ObjectCounter<>(), new ObjectCounter<>(), new LinkedList<>());
    }

    public PathCountResult(ObjectCounter<Count> counter, ObjectCounter<Substitution> substitutions, List<ILineComparison> lineComparisons) {
        this.counter = counter;
        this.substitutions = substitutions;
        this.lineComparisons = lineComparisons;
    }

    public ObjectCounter<Count> getCounter() {
        return counter;
    }

    public ObjectCounter<Substitution> getSubstitutions() {
        return substitutions;
    }

    public List<ILineComparison> getLineComparisons() {
        return lineComparisons;
    }

    public void add(Substitution substitution, Count... count) {
        this.substitutions.add(substitution);
        for (Count c : count) {
            counter.add(c);
        }
    }

    public void add(ILineComparison lineComparison) {
        lineComparisons.add(lineComparison);
    }

    public void addAll(PathCountResult toAdd) {
        counter.addAll(toAdd.counter);
        substitutions.addAll(toAdd.substitutions);
        if (toAdd.lineComparisons != null) {
            lineComparisons.addAll(toAdd.lineComparisons);
        }
    }

}
