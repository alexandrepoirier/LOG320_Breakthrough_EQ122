public class BoardScoreEntry {
    public enum NodeType {
        EXACT,    // exact score
        MIN,      // upper bound (failed low)
        MAX       // lower bound (failed high)
    }

    int value;
    NodeType nodeType;
    int depth;

    public BoardScoreEntry(int value, NodeType nodeType, int depth) {
        this.value = value;
        this.nodeType = nodeType;
        this.depth = depth;
    }
}
