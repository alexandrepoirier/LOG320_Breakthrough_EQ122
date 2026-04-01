import java.util.ArrayList;

public class MoveGenerator {
    public static ArrayList<Move> getPossibleMoves(Board board, Mark mark) {
        ArrayList<Move> moves = new ArrayList<>(24);
        Mark[][] b = board.getBoard();

        if (mark == Mark.R) {
            for (int col = 0; col < 8; col++) {
                for (int row = 1; row < 8; row++) {
                    if (b[col][row] != Mark.R) continue;
                    if (col > 0 && b[col - 1][row - 1] != Mark.R) {
                        Move m = new Move(new byte[]{(byte) col, (byte) row}, new byte[]{(byte) (col - 1), (byte) (row - 1)}, Mark.R);
                        m.setTarget(b[col - 1][row - 1]);
                        moves.add(m);
                    }
                    if (b[col][row - 1] == Mark.EMPTY) {
                        Move m = new Move(new byte[]{(byte) col, (byte) row}, new byte[]{(byte) col, (byte) (row - 1)}, Mark.R);
                        m.setTarget(Mark.EMPTY);
                        moves.add(m);
                    }
                    if (col < 7 && b[col + 1][row - 1] != Mark.R) {
                        Move m = new Move(new byte[]{(byte) col, (byte) row}, new byte[]{(byte) (col + 1), (byte) (row - 1)}, Mark.R);
                        m.setTarget(b[col + 1][row - 1]);
                        moves.add(m);
                    }
                }
            }
        } else if (mark == Mark.B) {
            for (int col = 0; col < 8; col++) {
                for (int row = 0; row < 7; row++) {
                    if (b[col][row] != Mark.B) continue;
                    if (col > 0 && b[col - 1][row + 1] != Mark.B) {
                        Move m = new Move(new byte[]{(byte) col, (byte) row}, new byte[]{(byte) (col - 1), (byte) (row + 1)}, Mark.B);
                        m.setTarget(b[col - 1][row + 1]);
                        moves.add(m);
                    }
                    if (b[col][row + 1] == Mark.EMPTY) {
                        Move m = new Move(new byte[]{(byte) col, (byte) row}, new byte[]{(byte) col, (byte) (row + 1)}, Mark.B);
                        m.setTarget(Mark.EMPTY);
                        moves.add(m);
                    }
                    if (col < 7 && b[col + 1][row + 1] != Mark.B) {
                        Move m = new Move(new byte[]{(byte) col, (byte) row}, new byte[]{(byte) (col + 1), (byte) (row + 1)}, Mark.B);
                        m.setTarget(b[col + 1][row + 1]);
                        moves.add(m);
                    }
                }
            }
        }
        return moves;
    }
}
