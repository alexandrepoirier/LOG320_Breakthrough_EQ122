import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelAlphaBeta {
    ExecutorService executor;
    private int exploredNodesCount = 0;
    private long TIME_LIMIT_MS = 0;
    private long START_TIME = 0;
    private Mark cpuMark;
    private Mark opponentMark;

    public ParallelAlphaBeta(int maxThreads, long timeLimit, Mark cpuMark, Mark opponentMark) {
        executor = Executors.newFixedThreadPool(maxThreads);
        this.TIME_LIMIT_MS = timeLimit;
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
    }

    public ArrayList<Future<Move>> submit(Board board, ArrayList<Move> moves, int targetDepth, int alpha, int beta, long startTime) {
        START_TIME = startTime;
        exploredNodesCount = 0;

        ArrayList<Future<Move>> futures = new ArrayList<>();

        for(Move move : moves) {
            futures.add(executor.submit(() -> {
                Board threadBoard = new Board(board);
                threadBoard.play(move);
                int score = alphaBetaInternal(threadBoard, false, 0, targetDepth, alpha, beta);
                move.setScore(score);
                return move;
            }));
        }

        return futures;
    }

    public int getExploredNodesCount() {
        return exploredNodesCount;
    }

    private boolean isTimeUp(){
        return System.currentTimeMillis() - START_TIME > TIME_LIMIT_MS;
    }

    private int alphaBetaInternal(Board board, boolean isMaximizing, int localDepth, int targetDepth, int alpha, int beta){
        exploredNodesCount++;

        if (isTimeUp()) {
            return 0;
        }

        int boardVal = board.evaluate(cpuMark, opponentMark);
        if (boardVal == Scoring.WIN_SCORE){
            return boardVal;
        }
        if (boardVal == Scoring.LOSE_SCORE){
            return boardVal;
        }

        if (localDepth >= targetDepth) {
            return boardVal;
        }

        if (isMaximizing) {
            ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board,cpuMark);
            if (possibleMoves.isEmpty()) {
                return Integer.MIN_VALUE + localDepth;
            }

            int maxScore = Integer.MIN_VALUE;

            for (Move move : possibleMoves) {
                if (isTimeUp()) {
                    break;
                }

                board.play(move);
                int score = alphaBetaInternal(board, false, localDepth+1, targetDepth, alpha, beta);
                board.undoMove(move);

                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);

                if (beta <= alpha) {
                    break;
                }
            }

            return maxScore;
        } else {
            ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board,opponentMark);
            if (possibleMoves.isEmpty()) {
                return Integer.MAX_VALUE - localDepth;
            }

            int minScore = Integer.MAX_VALUE;

            for (Move move : possibleMoves) {
                if (isTimeUp()) {
                    break;
                }

                board.play(move);
                int score = alphaBetaInternal(board, true, localDepth+1, targetDepth, alpha, beta);
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
