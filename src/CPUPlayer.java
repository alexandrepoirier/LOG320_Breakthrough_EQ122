import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

class CPUPlayer
{
    private final Mark cpuMark;
    private final Mark opponentMark;

    private long searchStartTime;
    private static final long TIME_LIMIT_MS = 4650;
    private static AtomicBoolean IS_TIME_UP = new AtomicBoolean(false);

    ParallelAlphaBeta parallelAlphaBeta;

    public CPUPlayer(Mark cpu){
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.R) ? Mark.B : Mark.R;
        parallelAlphaBeta = new ParallelAlphaBeta(Runtime.getRuntime().availableProcessors() + 1, cpuMark, opponentMark, IS_TIME_UP);
    }

    public Mark getCpuMark(){
        return cpuMark;
    }

    public Mark getOpponentMark(){
        return opponentMark;
    }

    private boolean isTimeUp() {
        if(System.currentTimeMillis() - searchStartTime >= TIME_LIMIT_MS){
            IS_TIME_UP.set(true);
            return true;
        }

        return false;
    }

    public Move getBestMove(Board board){
        IS_TIME_UP.set(false);
        searchStartTime = System.currentTimeMillis();
        parallelAlphaBeta.scoreMap.clear();
        ArrayList<Move> moves = generateBestMoves(board);

        if(Client.DEBUG_MODE){
            System.out.printf("Took %.2f s to get moves%n", (float)(System.currentTimeMillis() - searchStartTime) / 1000.);
        }

        return moves.getFirst();
    }

    public ArrayList<Move> generateBestMoves(Board board){
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board,cpuMark);

        if (possibleMoves.isEmpty()) { // safety but should never happen
            return bestMoves;
        }

        // order moves by best to worst
        possibleMoves.sort((Move m1, Move m2) -> {
            if(m1.isWinningMove() != m2.isWinningMove()){ return m1.isWinningMove() ? -1 : 1; }
            if(m1.isEatingMove() != m2.isEatingMove()){ return m1.isEatingMove() ? -1 : 1; }
            return 0;
        });

        int currentTargetDepth = 1;
        int maxDepthReached = 0;
        long totalNodes = 0;
        int bestScore;
        boolean completedDepth;
        
        while (!isTimeUp()) {
            // Reorder moves after getting scores from first iteration
            if (currentTargetDepth > 1){
                possibleMoves.sort((a, b) -> b.getScore() - a.getScore());
            }

            ArrayList<Move> currentBestMoves = new ArrayList<Move>();

            bestScore = Integer.MIN_VALUE;
            completedDepth = true;

            ArrayList<Future<Move>> futures = parallelAlphaBeta.submit(board, possibleMoves, currentTargetDepth, Integer.MIN_VALUE, Integer.MAX_VALUE);

            int i = -1;
            while(!futures.isEmpty()){
                i = (i+1)%futures.size();

                if(isTimeUp()){
                    completedDepth = false;
                    for (Future<Move> f : futures) {
                        f.cancel(true);
                    }
                    break;
                }

                Future<Move> future = futures.get(i);

                if(future.isDone()){
                    try{
                        Move move = future.get();

                        if (move.getScore() > bestScore) {
                            bestScore = move.getScore();
                            currentBestMoves.clear();
                            currentBestMoves.add(move);
                        } else if (move.getScore() == bestScore) {
                            currentBestMoves.add(move);
                        }
                    }catch(Exception e){
                        // Suck it
                    }finally{
                        futures.remove(i);
                    }
                }
            }

            if (completedDepth && parallelAlphaBeta.getExploredNodesCount() > 0) {
                maxDepthReached = currentTargetDepth;
                totalNodes += parallelAlphaBeta.getExploredNodesCount();
            }

            if (completedDepth && !currentBestMoves.isEmpty()) {
                bestMoves = new ArrayList<Move>(currentBestMoves);

                if (bestScore == Scoring.WIN_SCORE) {
                    break;
                }
            }
            
            currentTargetDepth++;
        }

        if(Client.DEBUG_MODE){
            System.out.printf("Max depth: %d, total nodes: %d%n", maxDepthReached, totalNodes);
        }

        if (bestMoves.isEmpty() && !possibleMoves.isEmpty()) {
            bestMoves.add(possibleMoves.getFirst());
        }
        
        return bestMoves;
    }
}
