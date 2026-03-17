import java.util.ArrayList;

public class MoveGenerator {
    public static ArrayList<Move> getPossibleMoves(Board board, Mark mark){
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(mark == Mark.R){
            possibleMoves = getPossibleRedMoves(board);
        } else if (mark == Mark.B) {
            possibleMoves = getPossibleBlackMoves(board);
        }
        return possibleMoves;
    }

    private static ArrayList<Move> getPossibleBlackMoves(Board board) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (board.getBoard()[col][row] == Mark.B){
                    possibleMoves.addAll(getBlackMove(board,col,row));
                }
            }
        }
        return possibleMoves;
    }

    private static ArrayList<Move> getBlackMove(Board board,int col,int row) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(row < 7 && row >= 0){
            if(col - 1 >=0 && board.getBoard()[col-1][row+1] != Mark.B){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col-1), (byte)(row+1)}, Mark.B));
            }
            if(board.getBoard()[col][row+1] == Mark.EMPTY){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col), (byte)(row+1)}, Mark.B));
            }
            if(col+1 <= 7 &&board.getBoard()[col+1][row+1] != Mark.B){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col+1), (byte)(row+1)}, Mark.B));
            }
        }
        return possibleMoves;
    }

    private static ArrayList<Move> getPossibleRedMoves(Board board) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (board.getBoard()[col][row] == Mark.R){
                    possibleMoves.addAll(getRedMove(board, col, row));
                }
            }
        }
        return possibleMoves;
    }

    private static ArrayList<Move> getRedMove(Board board, int col,int row) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(row <= 7 && row > 0){
            if(col - 1 >=0 && board.getBoard()[col-1][row-1] != Mark.R){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col-1), (byte)(row-1)}, Mark.R));
            }
            if(board.getBoard()[col][row-1] == Mark.EMPTY){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col), (byte)(row-1)}, Mark.R));
            }
            if(col+1 <= 7 &&board.getBoard()[col+1][row-1] != Mark.R){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col+1), (byte)(row-1)}, Mark.R));
            }
        }
        return possibleMoves;
    }
}
