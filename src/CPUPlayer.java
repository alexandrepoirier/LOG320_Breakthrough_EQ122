import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Future;

class CPUPlayer
{
    private Mark cpuMark;
    private Mark opponentMark;

//    private int numExploredNodes;
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

        if(Client.DEBUG_MODE){
            System.out.printf("Took %.2f s to get moves%n", (float)(System.currentTimeMillis() - searchStartTime) / 1000.);
        }

        return moves.getFirst();
    }

    public ArrayList<Move> generateBestMoves(Board board){
//        numExploredNodes = 0;
        
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

        // Iterative deepening
        int currentTargetDepth = 1;
        int iterativeDepthStep = 2;
        
        while (!isTimeUp()) {
            // Reorder moves after getting scores from first iteration
            if (currentTargetDepth > 1){
                possibleMoves.sort(Comparator.comparingInt(Move::getScore));
            }

            ArrayList<Move> currentBestMoves = new ArrayList<Move>();

            int bestScore = Integer.MIN_VALUE;
            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;
            boolean completedDepth = true;

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
            
//            for (Move move : possibleMoves) {
//                if (isTimeUp()) {
//                    completedDepth = false;
//                    break;
//                }
//
//                board.play(move);
//                int score = alphaBeta(board, false, 0, currentTargetDepth, alpha, beta);
//                move.setScore(score);
//                board.undoMove(move);
//
//                if (score > bestScore) {
//                    bestScore = score;
//                    currentBestMoves.clear();
//                    currentBestMoves.add(move);
//                } else if (score == bestScore) {
//                    currentBestMoves.add(move);
//                }
//
//                alpha = Math.max(alpha, bestScore);
//            }

            if(Client.DEBUG_MODE && completedDepth){
                System.out.printf("Completed depth : %d, explored %d nodes%n", currentTargetDepth, parallelAlphaBeta.getExploredNodesCount());
            }

            if (completedDepth && !currentBestMoves.isEmpty()) {
                bestMoves = new ArrayList<Move>(currentBestMoves);

                if (bestScore == Scoring.WIN_SCORE) {
                    break;
                }
            }
            
            currentTargetDepth+=iterativeDepthStep;
        }

        if (bestMoves.isEmpty() && !possibleMoves.isEmpty()) {
            bestMoves.add(possibleMoves.getFirst());
        }
        
        return bestMoves;
    }

//    private int alphaBeta(Board board, boolean isMaximizing, int localDepth, int targetDepth, int alpha, int beta) {
//        numExploredNodes++;
//
//        if (isTimeUp()) {
//            return 0;
//        }
//
//        int boardVal = board.evaluate(cpuMark, opponentMark);
//        if (boardVal == Scoring.WIN_SCORE){
//            return boardVal;
//        }
//        if (boardVal == Scoring.LOSE_SCORE){
//            return boardVal;
//        }
//
//        if (localDepth >= targetDepth) {
//            return boardVal;
//        }
//
//        if (isMaximizing) {
//            ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board,cpuMark);
//            if (possibleMoves.isEmpty()) {
//                return Integer.MIN_VALUE + localDepth;
//            }
//
//            int maxScore = Integer.MIN_VALUE;
//
//            for (Move move : possibleMoves) {
//                if (isTimeUp()) {
//                    break;
//                }
//
//                board.play(move);
//                int score = alphaBeta(board, false, localDepth+1, targetDepth, alpha, beta);
//                board.undoMove(move);
//
//                maxScore = Math.max(maxScore, score);
//                alpha = Math.max(alpha, maxScore);
//
//                if (beta <= alpha) {
//                    break;
//                }
//            }
//
//            return maxScore;
//        } else {
//            ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board,opponentMark);
//            if (possibleMoves.isEmpty()) {
//                return Integer.MAX_VALUE - localDepth;
//            }
//
//            int minScore = Integer.MAX_VALUE;
//
//            for (Move move : possibleMoves) {
//                if (isTimeUp()) {
//                    break;
//                }
//
//                board.play(move);
//                int score = alphaBeta(board, true, localDepth+1, targetDepth, alpha, beta);
//                board.undoMove(move);
//
//                minScore = Math.min(minScore, score);
//                beta = Math.min(beta, minScore);
//
//                if (beta <= alpha) {
//                    break;
//                }
//            }
//
//            return minScore;
//        }
//    }
}
