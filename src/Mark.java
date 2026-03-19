import java.math.BigInteger;

public enum Mark{
    R (2),
    B (3),
    EMPTY (-1),
    UNKNOWN (0);

    private final int value;

    private Mark(int value){
        this.value = value;
    }

    public int value() {return value; }
}
