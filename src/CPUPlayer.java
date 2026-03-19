import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Future;

class CPUPlayer
{
    private final Mark cpuMark;
    private final Mark opponentMark;

    private long searchStartTime;
    private static final long TIME_LIMIT_MS = 4800;

    ParallelAlphaBeta parallelAlphaBeta;

    public CPUPlayer(Mark cpu){
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.R) ? Mark.B : Mark.R;
        parallelAlphaBeta = new ParallelAlphaBeta(Runtime.getRuntime().availableProcessors() + 1, TIME_LIMIT_MS, cpuMark, opponentMark);
    }

    public Mark getCpuMark(){
        return cpuMark;
    }

    public Mark getOpponentMark(){
        return opponentMark;
    }

    private boolean isTimeUp() {
        return (System.currentTimeMillis() - searchStartTime) >= TIME_LIMIT_MS;
    }

    public Move getBestMove(Board board){
        searchStartTime = System.currentTimeMillis();
        ArrayList<Move> moves = generateBestMoves(board);
        parallelAlphaBeta.cleanupMap();

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
            if(m1.isWinningMove() != m2.isWinningMove()){ return m1.isWinningMove() ? 1 : -1; }
            if(m1.isWinningMove() != m2.isWinningMove()){ return m1.isEatingMove() ? 1 : -1; }
            return 0;
        });

        int currentTargetDepth = 1;
        int bestScore, alpha, beta;
        boolean completedDepth;
        
        while (!isTimeUp()) {
            // Reorder moves after getting scores from first iteration
            if (currentTargetDepth > 1){
                possibleMoves.sort(Comparator.comparingInt(Move::getScore));
            }

            ArrayList<Move> currentBestMoves = new ArrayList<Move>();

            bestScore = Integer.MIN_VALUE;
            alpha = Integer.MIN_VALUE;
            beta = Integer.MAX_VALUE;
            completedDepth = true;

            ArrayList<Future<Move>> futures = parallelAlphaBeta.submit(board, possibleMoves, currentTargetDepth, alpha, beta, searchStartTime);

            int i = -1;
            while(futures.size() > 0){
                i = (i+1)%futures.size();

                if(isTimeUp()){
                    completedDepth = false;
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

                        alpha = Math.max(alpha, bestScore);
                    }catch(Exception e){
                        // Suck it
                    }finally{
                        futures.remove(i);
                    }
                }
            }

            if(Client.DEBUG_MODE && completedDepth){
                System.out.printf("Completed depth : %d, explored %d nodes%n", currentTargetDepth, parallelAlphaBeta.getExploredNodesCount());
                //System.out.printf("Collision count : %d%n", parallelAlphaBeta.collisions.get());
            }

            if (completedDepth && !currentBestMoves.isEmpty()) {
                bestMoves = new ArrayList<Move>(currentBestMoves);

                if (bestScore == Scoring.WIN_SCORE) {
                    break;
                }
            }
            
            currentTargetDepth++;
        }

        if (bestMoves.isEmpty() && !possibleMoves.isEmpty()) {
            bestMoves.add(possibleMoves.getFirst());
        }
        
        return bestMoves;
    }
}
