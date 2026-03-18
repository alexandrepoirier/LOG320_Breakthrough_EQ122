public class BoardScoreEntry {
    public enum NodeType {
        TERMINAL,
        MIN,
        MAX
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
