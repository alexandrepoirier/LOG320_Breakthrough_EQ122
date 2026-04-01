import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelAlphaBeta {
    ExecutorService executor;
    private AtomicBoolean IS_TIME_UP;
    private final AtomicInteger exploredNodesCount = new AtomicInteger(0);
    private final Mark cpuMark;
    private final Mark opponentMark;
    ConcurrentHashMap<Long, BoardScoreEntry> scoreMap = new ConcurrentHashMap<>(1_000_000);

    public ParallelAlphaBeta(int maxThreads, Mark cpuMark, Mark opponentMark, AtomicBoolean isTimeUp) {
        executor = Executors.newFixedThreadPool(maxThreads);
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        IS_TIME_UP = isTimeUp;
    }

    private final AtomicInteger sharedAlpha = new AtomicInteger(Integer.MIN_VALUE);

    public ArrayList<Future<Move>> submit(Board board, ArrayList<Move> moves, int targetDepth, int alpha, int beta) {
        exploredNodesCount.set(0);
        sharedAlpha.set(alpha);

        ArrayList<Future<Move>> futures = new ArrayList<>();

        for(Move move : moves) {
            futures.add(executor.submit(() -> {
                Board threadBoard = new Board(board);
                threadBoard.play(move);
                int threadAlpha = Math.max(alpha, sharedAlpha.get());
                int score = alphaBetaInternal(threadBoard, false, 0, targetDepth, threadAlpha, beta);
                sharedAlpha.updateAndGet(current -> Math.max(current, score));
                move.setScore(score);
                return move;
            }));
        }

        return futures;
    }

    public int getExploredNodesCount() {
        return exploredNodesCount.get();
    }

    private int alphaBetaInternal(Board board, boolean isMaximizing, int localDepth, int targetDepth, int alpha, int beta){
        if(Client.DEBUG_MODE) {
            exploredNodesCount.incrementAndGet();
        }

        if (IS_TIME_UP.get() || Thread.currentThread().isInterrupted()) {
            return 0;
        }

        if (board.hasWon(cpuMark)) return Scoring.WIN_SCORE;
        if (board.hasWon(opponentMark)) return Scoring.LOSE_SCORE;

        int remainingDepth = targetDepth - localDepth;

        // TT lookup
        long boardId = board.generateUniqueId();
        BoardScoreEntry entry = scoreMap.get(boardId);
        if (entry != null && entry.depth >= remainingDepth) {
            if (entry.nodeType == BoardScoreEntry.NodeType.EXACT) {
                return entry.value;
            } else if (entry.nodeType == BoardScoreEntry.NodeType.MAX) {
                alpha = Math.max(alpha, entry.value);
            } else if (entry.nodeType == BoardScoreEntry.NodeType.MIN) {
                beta = Math.min(beta, entry.value);
            }
            if (alpha >= beta) return entry.value;
        }

        if (remainingDepth <= 0) {
            return board.evaluate(cpuMark, opponentMark);
        }

        ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board, isMaximizing ? cpuMark : opponentMark);

        if (possibleMoves.isEmpty()) {
            return isMaximizing ? Integer.MIN_VALUE + localDepth : Integer.MAX_VALUE - localDepth;
        }

        // Order moves by TT scores without mutating move targets
        for (Move move : possibleMoves) {
            Mark dest = board.getBoard()[move.getEndCol()][move.getEndRow()];
            long childHash = BoardHash.updatePlay(board.generateUniqueId(),
                    move.getStartCol(), move.getStartRow(), move.getPlayer(),
                    move.getEndCol(), move.getEndRow(), dest);
            BoardScoreEntry childEntry = scoreMap.get(childHash);
            move.setScore(childEntry != null ? childEntry.value : 0);
        }
        possibleMoves.sort((a, b) -> isMaximizing ? b.getScore() - a.getScore() : a.getScore() - b.getScore());

        int originalAlpha = alpha;
        int optimalScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : possibleMoves) {
            if (IS_TIME_UP.get()) break;

            board.play(move);
            int score = alphaBetaInternal(board, !isMaximizing, localDepth + 1, targetDepth, alpha, beta);
            board.undoMove(move);

            if (isMaximizing) {
                optimalScore = Math.max(optimalScore, score);
                alpha = Math.max(alpha, optimalScore);
            } else {
                optimalScore = Math.min(optimalScore, score);
                beta = Math.min(beta, optimalScore);
            }

            if (beta <= alpha) break;
        }

        // TT store
        if (!IS_TIME_UP.get()) {
            BoardScoreEntry.NodeType type;
            if (optimalScore <= originalAlpha) {
                type = BoardScoreEntry.NodeType.MIN;
            } else if (optimalScore >= beta) {
                type = BoardScoreEntry.NodeType.MAX;
            } else {
                type = BoardScoreEntry.NodeType.EXACT;
            }

            BoardScoreEntry existing = scoreMap.get(boardId);
            if (existing == null || remainingDepth >= existing.depth) {
                scoreMap.put(boardId, new BoardScoreEntry(optimalScore, type, remainingDepth));
            }
        }

        return optimalScore;
    }
}
