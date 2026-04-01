public class BoardHash {
    private static final long[] POW13 = new long[8];
    private static final long[] POW31 = new long[8];
    private static final long MOD = 4611686018427388039L;
    static {
        POW13[0] = 1; POW31[0] = 1;
        for (int i = 1; i < 8; i++) {
            POW13[i] = POW13[i - 1] * 13;
            POW31[i] = POW31[i - 1] * 31;
        }
    }

    private static long cellHash(int col, int row, Mark mark) {
        return (mark.value() * POW13[col] * POW31[row]) % MOD;
    }

    // hash avec recompute 
    public static long computeFull(Mark[][] board, int size) {
        long id = 0;
        for (int row = 0; row < size; row++) {
            long rp = POW31[row];
            for (int col = 0; col < size; col++) {
                id += (board[col][row].value() * POW13[col] * rp) % MOD;
            }
        }
        return id;
    }

    public static long updatePlay(long hash, int startCol, int startRow, Mark player, int endCol, int endRow, Mark captured) {
        hash -= cellHash(startCol, startRow, player);
        hash -= cellHash(endCol, endRow, captured);
        hash += cellHash(endCol, endRow, player);
        hash += cellHash(startCol, startRow, Mark.EMPTY);
        return hash;
    }

    public static long updateUndo(long hash, int startCol, int startRow, Mark player, int endCol, int endRow, Mark captured) {
        hash -= cellHash(endCol, endRow, player);
        hash -= cellHash(startCol, startRow, Mark.EMPTY);
        hash += cellHash(startCol, startRow, player);
        hash += cellHash(endCol, endRow, captured);
        return hash;
    }
}
