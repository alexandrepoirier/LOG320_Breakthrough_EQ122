import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelAlphaBeta {
    private final boolean MAP_ENABLED = true;
    public ThreadPoolExecutor executor;
    private final AtomicBoolean IS_TIME_UP;
    private final AtomicBoolean CAN_SUB_THREAD = new AtomicBoolean(false);
    private final AtomicInteger exploredNodesCount = new AtomicInteger(0);
//    public AtomicInteger collisions = new AtomicInteger(0);
    private final Mark cpuMark;
    private final Mark opponentMark;
    ConcurrentHashMap<Long, BoardScoreEntry> scoreMap =  new ConcurrentHashMap<>((int)20E6);
//    ConcurrentHashMap<Long, Board> boardKeyMap = new ConcurrentHashMap<>((int)20E6);

    public ParallelAlphaBeta(int maxThreads, Mark cpuMark, Mark opponentMark, AtomicBoolean isTimeUp) {
        executor = new ThreadPoolExecutor(maxThreads, maxThreads, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
        IS_TIME_UP = isTimeUp;
    }

    public ArrayList<Future<Move>> submit(Board board, ArrayList<Move> moves, int targetDepth, int alpha, int beta, int turn) {
        return submitInternal(board, moves,targetDepth, alpha, beta, true, turn);
    }

    private ArrayList<Future<Move>> submitInternal(Board board, ArrayList<Move> moves, int targetDepth,
                                                   int alpha, int beta,
                                                   boolean initialCall, int turn) {
        CAN_SUB_THREAD.set(false);

        if(initialCall){
            exploredNodesCount.set(0);
//            collisions.set(0);
        }

        ArrayList<Future<Move>> futures = new ArrayList<>();

        if(IS_TIME_UP.get()) {
            return futures;
        }

        for(Move move : moves) {
            futures.add(executor.submit(() -> {
                Board threadBoard = new Board(board);
                threadBoard.play(move);
                int score = alphaBetaInternal(threadBoard, false, 0, targetDepth, alpha, beta, turn);
                move.setScore(score);
                return move;
            }));
        }

        CAN_SUB_THREAD.set(true);

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

    private float getThreadPoolActivityRatio(){
        return (float)executor.getActiveCount() / executor.getCorePoolSize();
    }

    private int alphaBetaInternal(Board board, boolean isMaximizing, int localDepth, int targetDepth,
                                  int alpha, int beta, int turn){
        if(Client.DEBUG_MODE) {
            exploredNodesCount.incrementAndGet();
        }

        // [BEGIN] Early exit conditions
        if (board.hasWon(cpuMark)){
            return Scoring.WIN_SCORE;
        }
        if (board.hasWon(opponentMark)){
            return Scoring.LOSE_SCORE;
        }
        // [END] Early exit conditions

        long boardId = board.generateUniqueId();
        BoardScoreEntry scoreEntry = scoreMap.get(boardId);

        // this condition is necessary if the opponent is very quick as the time up flag might go on/off too quickly
        if (IS_TIME_UP.get() || Client.getTurnCount() > turn) {
            return 0;
        }

        // [BEGIN] Reached terminal node
        if (localDepth >= targetDepth) {
            if(scoreEntry != null && scoreEntry.nodeType == BoardScoreEntry.NodeType.TERMINAL) {
                return scoreEntry.value;
            }
            else{
                int boardValue = board.evaluate(cpuMark, opponentMark);

                if(MAP_ENABLED) {
                    scoreMap.put(boardId,
                            new BoardScoreEntry(boardValue,
                                    BoardScoreEntry.NodeType.TERMINAL,
                                    localDepth + Client.getTurnCount())
                    );
                }
                //boardKeyMap.put(boardId, new Board(board));
                return boardValue;
            }
        }
        // [END] Reached terminal node

        // [BEGIN] Core algorithm
        ArrayList<Move> possibleMoves = null;

        if(!IS_TIME_UP.get()) {
            possibleMoves = MoveGenerator.getPossibleMoves(board, isMaximizing ? cpuMark : opponentMark);
        }

        if(possibleMoves == null || possibleMoves.isEmpty()){
            return isMaximizing ? Integer.MIN_VALUE + localDepth : Integer.MAX_VALUE - localDepth;
        }

        int optimalScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        if(MAP_ENABLED){
            // Check if we stored the value and if it is still useful
            if (isMaximizing) {
                if (scoreEntry != null
                        && scoreEntry.nodeType == BoardScoreEntry.NodeType.MAX
                        && scoreEntry.depth >= (targetDepth - localDepth)
                ) {
                    if (beta <= scoreEntry.value) {
                        return scoreEntry.value;
                    }
                }
            }else {
                if(scoreEntry != null
                        && scoreEntry.nodeType == BoardScoreEntry.NodeType.MIN
                        && scoreEntry.depth >= (targetDepth - localDepth)
                ) {
                    if(scoreEntry.value <= alpha){
                        return scoreEntry.value;
                    }
                }
            }
        }

        // Thread management : start new threads if possible to maximize parallelization
        if(getThreadPoolActivityRatio() <= 0.6
                && CAN_SUB_THREAD.get()
                && executor.getQueue().isEmpty()
        ){
            ArrayList<Future<Move>> futures = submitInternal(board, possibleMoves, localDepth + 1,
                    alpha, beta, false, turn);

            if(Client.DEBUG_MODE){
                System.out.printf("Starting %d sub-threads%n", futures.size());
            }

            int i = -1;
            while(futures.size() > 0){
                i = (i+1)%futures.size();

                if(IS_TIME_UP.get()){
                    for(Future f : futures){
                        f.cancel(true);
                    }

                    break;
                }

                Future<Move> future = futures.get(i);

                if(future.isDone()){
                    try{
                        Move move = future.get();

                        if(isMaximizing) {
                            optimalScore = Math.max(optimalScore, move.getScore());
                            alpha = Math.max(alpha, optimalScore);
                        }else {
                            optimalScore = Math.min(optimalScore, move.getScore());
                            beta = Math.min(beta, optimalScore);
                        }

                        if (beta <= alpha) {
                            break;
                        }
                    }catch(Exception e){
                        // Suck it
                    }finally{
                        futures.remove(i);
                        i--;
                    }

//                    if(Client.DEBUG_MODE){
//                        System.out.printf("- %d/%d sub-futures completed%n", possibleMoves.size() - futures.size(), possibleMoves.size());
//                    }
                }

                if(Client.DEBUG_MODE && futures.isEmpty()){
                    System.out.printf("Completed %d sub-futures%n", possibleMoves.size());
                }
            }
        }else{ // process here within active thread
            for (Move move : possibleMoves) {
                if (IS_TIME_UP.get()) {
                    break;
                }

                board.play(move);
                int score = alphaBetaInternal(board, false, localDepth + 1, targetDepth,
                        alpha, beta, turn);
                board.undoMove(move);

                if(isMaximizing) {
                    optimalScore = Math.max(optimalScore, score);
                    alpha = Math.max(alpha, optimalScore);
                }else {
                    optimalScore = Math.min(optimalScore, score);
                    beta = Math.min(beta, optimalScore);
                }

                if (beta <= alpha) {
                    break;
                }
            }
        }
        // [END] Core algorithm

        // Store value in map if we completed exploration of all branches
        if (MAP_ENABLED && !IS_TIME_UP.get()) {
            scoreMap.put(boardId,
                    new BoardScoreEntry(optimalScore,
                            isMaximizing ? BoardScoreEntry.NodeType.MAX : BoardScoreEntry.NodeType.MIN,
                            targetDepth - localDepth)
            );

//            if(Client.DEBUG_MODE) {
//                // Board ID collision test
//                if (boardKeyMap.containsKey(boardId)) {
//                    if (!boardKeyMap.get(boardId).equals(board)) {
//                        collisions.incrementAndGet();
//                    }
//                } else {
//                    boardKeyMap.put(boardId, new Board(board));
//                }
//            }
        }

        return optimalScore;
    }
}
