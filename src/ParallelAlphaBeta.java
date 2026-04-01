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
    //public AtomicInteger collisions = new AtomicInteger(0);
    private final Mark cpuMark;
    private final Mark opponentMark;
    ConcurrentHashMap<Long, BoardScoreEntry> scoreMap =  new ConcurrentHashMap<>((int)20E6);
    //ConcurrentHashMap<Long, Board> boardKeyMap = new ConcurrentHashMap<>((int)20E6);

    public ParallelAlphaBeta(int maxThreads, Mark cpuMark, Mark opponentMark, AtomicBoolean isTimeUp) {
        executor = Executors.newFixedThreadPool(maxThreads);
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        IS_TIME_UP = isTimeUp;
    }

    public ArrayList<Future<Move>> submit(Board board, ArrayList<Move> moves, int targetDepth, int alpha, int beta) {
        exploredNodesCount.set(0);
        //collisions.set(0);

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

    public void cleanupMap(){
        executor.submit(() ->{
            if(Client.DEBUG_MODE){
                System.out.println("Cleaning up map...");
            }

            ArrayList<Long> toRemove = new ArrayList<>();
            scoreMap.forEach( (k,v) -> {
                if(v.depth <= 4){
                    toRemove.add(k);
                }
            });

            if(Client.DEBUG_MODE){
                System.out.printf("Found %d elements to remove%n",  toRemove.size());
            }

            for(Long k : toRemove){
                if(GameState.getState() != GameState.State.WAITING){
                    break;
                }
                scoreMap.remove(k);
            }

            if(Client.DEBUG_MODE){
                System.out.println("Done cleaning map");
            }
        });
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

        if (localDepth >= targetDepth) {
            return board.evaluate(cpuMark, opponentMark);
        }

        ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board, isMaximizing ? cpuMark : opponentMark);

        if (possibleMoves.isEmpty()) {
            return isMaximizing ? Integer.MIN_VALUE + localDepth : Integer.MAX_VALUE - localDepth;
        }

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

        return optimalScore;
    }
}
