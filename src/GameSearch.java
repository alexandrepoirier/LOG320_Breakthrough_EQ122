import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

class GameSearch {
    private Mark cpuMark;
    private Mark opponentMark;
    private int numExploredNodes;
    private long searchStartTime;
    private static final long TIME_LIMIT_MS = 4800;

    public GameSearch(Mark cpuMark, Mark opponentMark) {
        this.cpuMark = cpuMark;
        this.opponentMark = opponentMark;
    }

    public int getNumExploredNodes() {
        return numExploredNodes;
    }

    private boolean isTimeUp() {
        return (System.currentTimeMillis() - searchStartTime) >= TIME_LIMIT_MS;
    }

    public ArrayList<Move> getNextMoveAB(Board board) {
        numExploredNodes = 0;
        searchStartTime = System.currentTimeMillis();
        
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board, cpuMark);
        
        // Tri des coups (Move Ordering)
        sortMoves(possibleMoves);
        
        if (possibleMoves.isEmpty()) {
            return bestMoves;
        }

        int currentDepth = 1;
        
        while (!isTimeUp()) {
            List<Callable<ParallelSearchHelper.ResultatEvaluation>> taches = new ArrayList<>();
            final int prof = currentDepth;
            AtomicInteger globalAlpha = new AtomicInteger(Integer.MIN_VALUE);

            for (Move move : possibleMoves) {
                taches.add(() -> evaluerCoupRacineAlphaBeta(board, move, prof, globalAlpha));
            }

            ParallelSearchHelper.ResultatGlobal res = ParallelSearchHelper.executerEnParallele(taches);

            if (isTimeUp()) {
                break;
            }

            bestMoves = res.meilleursCoups;
            numExploredNodes += res.totalNoeuds;

            if (res.meilleurScore >= Integer.MAX_VALUE - 1000) {
                break;
            }
            
            currentDepth++;
        }
        
        if (bestMoves.isEmpty() && !possibleMoves.isEmpty()) {
            bestMoves.add(possibleMoves.getFirst());
        }
        
        return bestMoves;
    }

    private ParallelSearchHelper.ResultatEvaluation evaluerCoupRacineAlphaBeta(Board board, Move coup, int profMax, AtomicInteger globalAlpha) {
        Board copie = new Board(board);
        copie.play(coup, cpuMark);
        int[] compteur = new int[]{0};
        int alphaInitial = Integer.MIN_VALUE;
        if (globalAlpha != null) {
            alphaInitial = Math.max(alphaInitial, globalAlpha.get());
        }

        int score = alphaBeta(copie, false, 0, alphaInitial, Integer.MAX_VALUE, profMax, compteur, globalAlpha);
        
        if (globalAlpha != null) {
            globalAlpha.updateAndGet(current -> Math.max(current, score));
        }

        return new ParallelSearchHelper.ResultatEvaluation(coup, score, compteur[0]);
    }

    private int alphaBeta(Board board, boolean isMaximizing, int depth, int alpha, int beta, int maxDepth, int[] nodes, AtomicInteger globalAlpha) {
        nodes[0]++;

        if (isTimeUp()) return 0;

        if (globalAlpha != null) {
            alpha = Math.max(alpha, globalAlpha.get());
        }
        
        if (beta <= alpha) return alpha;

        int boardVal = board.evaluate(cpuMark);
        if (boardVal == Integer.MAX_VALUE) return boardVal - depth;
        if (boardVal == Integer.MIN_VALUE) return boardVal + depth;
        
        if (depth >= maxDepth) return boardVal;
        
        Mark currentMark = isMaximizing ? cpuMark : opponentMark;
        ArrayList<Move> moves = MoveGenerator.getPossibleMoves(board, currentMark);
        
        sortMoves(moves);
        
        if (moves.isEmpty()) {
            return isMaximizing ? (Integer.MIN_VALUE + depth) : (Integer.MAX_VALUE - depth);
        }

        int bestVal = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : moves) {
            if (isTimeUp()) break;
            
            if (globalAlpha != null && isMaximizing) {
               alpha = Math.max(alpha, globalAlpha.get());
               if (beta <= alpha) break;
            }

            board.play(move, currentMark);
            int val = alphaBeta(board, !isMaximizing, depth+1, alpha, beta, maxDepth, nodes, globalAlpha);
            board.undoMove(move);

            if (isMaximizing) {
                bestVal = Math.max(bestVal, val);
                alpha = Math.max(alpha, bestVal);
            } else {
                bestVal = Math.min(bestVal, val);
                beta = Math.min(beta, bestVal);
            }

            if (beta <= alpha) break;
        }
        return bestVal;
    }

    private void sortMoves(ArrayList<Move> moves) {
        moves.sort((m1, m2) -> {
            if (m1.isWinning() != m2.isWinning()) return m1.isWinning() ? -1 : 1;
            if (m1.isCapture() != m2.isCapture()) return m1.isCapture() ? -1 : 1;
            return 0;
        });
    }
}

