public enum Mark{
    R (1),
    B (2),
    EMPTY (0),
    UNKNOWN (-1);

    private final int value;

    private Mark(int value){
        this.value = value;
    }

    public int value() {return value; }
}
