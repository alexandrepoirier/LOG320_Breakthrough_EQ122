import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelAlphaBeta {
    ExecutorService executor;
    private AtomicInteger exploredNodesCount = new AtomicInteger(0);
    private long TIME_LIMIT_MS = 0;
    private long START_TIME = 0;
    private Mark cpuMark;
    private Mark opponentMark;
    ConcurrentHashMap<Double, BoardScoreEntry> scoreMap =  new ConcurrentHashMap<>((int)20E6);

    public ParallelAlphaBeta(int maxThreads, long timeLimit, Mark cpuMark, Mark opponentMark) {
        executor = Executors.newFixedThreadPool(maxThreads);
        this.TIME_LIMIT_MS = timeLimit;
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
    }

    public ArrayList<Future<Move>> submit(Board board, ArrayList<Move> moves, int targetDepth, int alpha, int beta, long startTime) {
        START_TIME = startTime;
        exploredNodesCount.set(0);

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
        return exploredNodesCount.get();
    }

    private boolean isTimeUp(){
        return System.currentTimeMillis() - START_TIME > TIME_LIMIT_MS;
    }

    private int alphaBetaInternal(Board board, boolean isMaximizing, int localDepth, int targetDepth, int alpha, int beta){
        if(Client.DEBUG_MODE) {
            exploredNodesCount.incrementAndGet();
        }

        // [BEGIN] Early exit conditions
        if (isTimeUp()) {
            return 0;
        }

        if (board.hasWon(cpuMark)){
            return Scoring.WIN_SCORE;
        }
        if (board.hasWon(opponentMark)){
            return Scoring.LOSE_SCORE;
        }
        // [END] Early exit conditions

        double boardId = board.generateUniqueId();
        BoardScoreEntry scoreEntry = scoreMap.get(boardId);

        // [BEING] Reached terminal node
        if (localDepth >= targetDepth) {
            if(scoreEntry != null && scoreEntry.nodeType == BoardScoreEntry.NodeType.TERMINAL) {
                return scoreEntry.value;
            }
            else{
                int boardValue = board.evaluate(cpuMark, opponentMark);
                scoreMap.put(boardId, new BoardScoreEntry(boardValue, BoardScoreEntry.NodeType.TERMINAL, localDepth));
                return boardValue;
            }
        }
        // [END] Reached terminal node

        // [BEGIN] Core algorithm
        ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board, isMaximizing ? cpuMark : opponentMark);

        if (possibleMoves.isEmpty()) {
            return isMaximizing ? Integer.MIN_VALUE + localDepth : Integer.MAX_VALUE - localDepth;
        }

        int optimalScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        if (isMaximizing) {
            if(scoreEntry != null
                    && scoreEntry.nodeType == BoardScoreEntry.NodeType.MAX
                    && scoreEntry.depth >= (localDepth + Client.getTurnCount())
            ) {
                if(beta <= scoreEntry.value){
                    return scoreEntry.value;
                }
            }

            for (Move move : possibleMoves) {
                if (isTimeUp()) {
                    break;
                }

                board.play(move);
                int score = alphaBetaInternal(board, false, localDepth+1, targetDepth, alpha, beta);
                board.undoMove(move);

                optimalScore = Math.max(optimalScore, score);
                alpha = Math.max(alpha, optimalScore);

                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            if(scoreEntry != null
                    && scoreEntry.nodeType == BoardScoreEntry.NodeType.MIN
                    && scoreEntry.depth >= (localDepth + Client.getTurnCount())
            ) {
                if(scoreEntry.value <= alpha){
                    return scoreEntry.value;
                }
            }

            for (Move move : possibleMoves) {
                if (isTimeUp()) {
                    break;
                }

                board.play(move);
                int score = alphaBetaInternal(board, true, localDepth+1, targetDepth, alpha, beta);
                board.undoMove(move);

                optimalScore = Math.min(optimalScore, score);
                beta = Math.min(beta, optimalScore);

                if (beta <= alpha) {
                    break;
                }
            }
        }
        // [END] Core algorithm

        if (!isTimeUp()) {
            scoreMap.put(boardId,
                    new BoardScoreEntry(optimalScore,
                            isMaximizing ? BoardScoreEntry.NodeType.MAX : BoardScoreEntry.NodeType.MIN,
                            localDepth + Client.getTurnCount())
            );
        }

        return optimalScore;
    }
}
