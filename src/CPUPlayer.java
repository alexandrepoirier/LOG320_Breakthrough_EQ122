import java.util.ArrayList;
import java.util.Collection;

class CPUPlayer
{
    private Mark cpuMark;
    private Mark opponentMark;

    private int numExploredNodes;

    public CPUPlayer(Mark cpu){
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.R) ? Mark.B : Mark.R;
    }

    public int  getNumOfExploredNodes(){
        return numExploredNodes;
    }

    public Mark getCpuMark(){
        return cpuMark;
    }

    public Mark getOpponentMark(){
        return opponentMark;
    }

    public ArrayList<Move> getPossibleMoves(Board board, Mark mark){
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(mark == Mark.R){
            possibleMoves = getPossibleRedMoves(board);
        } else if (mark == Mark.B) {
            possibleMoves = getPossibleBlackMoves(board);
        }
        return possibleMoves;
    }

    private ArrayList<Move> getPossibleBlackMoves(Board board) {
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

    private ArrayList<Move> getBlackMove(Board board,int col,int row) {
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

    private ArrayList<Move> getPossibleRedMoves(Board board) {
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

    private ArrayList<Move> getRedMove(Board board, int col,int row) {
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

    public Move getBestMove(Board board){
        // generate all possible moves and pick best!
        Board copyBoard = new Board(board);
        return getNextMoveAB(copyBoard).get(0);
    }

    public ArrayList<Move> getNextMoveMinMax(Board board)
    {
        numExploredNodes = 0;
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        int bestScore = Integer.MIN_VALUE;
        
        ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
        
        for (Move move : possibleMoves) {
            board.play(move, cpuMark);
            int score = minMax(board, false, 0);
            board.undoMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }
        return bestMoves;
    }

    private int minMax(Board board, boolean isMaximizing, int depth) {
        numExploredNodes++;

        int boardVal = board.evaluate(cpuMark);
        if (boardVal == Integer.MAX_VALUE){
            return boardVal - depth;
        }
        if (boardVal == Integer.MIN_VALUE){
            return boardVal + depth;
        }
        
        // Limite de profondeur
        if (depth >= 3) {
            return 0;
        }

        if (isMaximizing) {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
            int maxScore = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, cpuMark);
                int score = minMax(board, false, depth+1);
                board.undoMove(move);
                maxScore = Math.max(maxScore, score);
            }
            return maxScore;
        } else {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,opponentMark);
            int minScore = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, opponentMark);
                int score = minMax(board, true, depth+1);
                board.undoMove(move);
                minScore = Math.min(minScore, score);
            }
            return minScore;
        }
    }

    public ArrayList<Move> getNextMoveAB(Board board){
        numExploredNodes = 0;
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
        
        for (Move move : possibleMoves) {
            board.play(move, cpuMark);
            int score = alphaBeta(board, false, 0, alpha, beta);
            board.undoMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
            
            alpha = Math.max(alpha, bestScore);
        }
        
        return bestMoves;
    }

    private int alphaBeta(Board board, boolean isMaximizing, int depth,int alpha, int beta) {
        numExploredNodes++;

        int boardVal = board.evaluate(cpuMark);
        if (boardVal == Integer.MAX_VALUE){
            return boardVal - depth;
        }
        if (boardVal == Integer.MIN_VALUE){
            return boardVal + depth;
        }
        
        // Limite de profondeur
        if (depth >= 3) {
            return 0;
        }
        
        if (isMaximizing) {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
            int maxScore = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, cpuMark);
                int score = alphaBeta(board, false, depth+1, alpha, beta);
                board.undoMove(move);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);
                if (beta <= alpha) {
                    break; 
                }
            }
            return maxScore;
        } else {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,opponentMark);
            int minScore = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, opponentMark);
                int score = alphaBeta(board, true, depth+1, alpha, beta);
                board.undoMove(move);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, minScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return minScore;
        }
    }
}
