package demiglace;

import demiglace.callgraph.CallGraphEdge;

import java.util.HashSet;
import java.util.List;


public class ParsingResult {
    HashSet<CallGraphEdge> edges;

    public ParsingResult(HashSet<CallGraphEdge> edges) {
        this.edges = edges;
    }

    public HashSet<CallGraphEdge> getEdges() {
        return edges;
    }
}
