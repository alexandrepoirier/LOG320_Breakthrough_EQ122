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
    private static final long TIME_LIMIT_MS = 4600;
    
    // creation de t able de Transposition
    private TranspositionTable tt = new TranspositionTable();

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
        
        // Tri des coups (move rdering)
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

        // --- Vérification Transposition Table ---
        String key = tt.generateKey(board);
        TranspositionTable.TTEntry entry = tt.get(key);
        if (entry != null && entry.depth >= (maxDepth - depth)) { // Si la profondeur stockée est suffisante
            if (entry.flag == TranspositionTable.EXACT) {
                return entry.value;
            } else if (entry.flag == TranspositionTable.LOWERBOUND) {
                alpha = Math.max(alpha, entry.value);
            } else if (entry.flag == TranspositionTable.UPPERBOUND) {
                beta = Math.min(beta, entry.value);
            }
            if (alpha >= beta) {
                return entry.value; // Coupure immédiate grâce à la TT
            }
        }
        // ----------------------------------------

        if (globalAlpha != null) {
            alpha = Math.max(alpha, globalAlpha.get());
        }
        
        if (beta <= alpha) return alpha;
        
        // Sauvegarde de l'alpha initial pour déterminer le type de coupe plus tard
        int originalAlpha = alpha;

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
        
        // --- Stockage dans Transposition Table ---
        if (!isTimeUp()) { // On ne stocke pas des résultats partiels interrompus par le temps
            int flag;
            if (bestVal <= originalAlpha) {
                flag = TranspositionTable.UPPERBOUND; // N'a pas amélioré alpha -> on a trouvé une borne sup
            } else if (bestVal >= beta) {
                flag = TranspositionTable.LOWERBOUND; // A provoqué une coupe -> on a trouvé une borne inf
            } else {
                flag = TranspositionTable.EXACT; // Valeur exacte
            }
            // On stocke la profondeur "restante" explorée depuis ce nœud
            tt.put(key, maxDepth - depth, bestVal, flag);
        }
        // -----------------------------------------
        
        return bestVal;
    }

    private void sortMoves(ArrayList<Move> moves) {
        moves.sort((m1, m2) -> {
            if (m1.isWinning() != m2.isWinning()) return m1.isWinning() ? -1 : 1;
            if (m1.isCapture() != m2.isCapture()) return m1.isCapture() ? -1 : 1;
            return 0;
        });
    }

    public ArrayList<Move> getNextMoveMinMax(Board board) {
        numExploredNodes = 0;
        searchStartTime = System.currentTimeMillis();
        
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        ArrayList<Move> possibleMoves = MoveGenerator.getPossibleMoves(board, cpuMark);
        
        sortMoves(possibleMoves);
        
        if (possibleMoves.isEmpty()) {
            return bestMoves;
        }

        int bestScore = Integer.MIN_VALUE;
        int currentDepth = 1;
        
        while (!isTimeUp()) {
            int tempBestScore = Integer.MIN_VALUE;
            ArrayList<Move> tempBestMoves = new ArrayList<>();

            for (Move move : possibleMoves) {
                if (isTimeUp()) break;
                
                Board copie = new Board(board);
                copie.play(move, cpuMark);
                
                int score = minMax(copie, false, 0, currentDepth);
                
                if (score > tempBestScore) {
                    tempBestScore = score;
                    tempBestMoves.clear();
                    tempBestMoves.add(move);
                } else if (score == tempBestScore) {
                    tempBestMoves.add(move);
                }
            }

            if (!isTimeUp()) {
                bestScore = tempBestScore;
                bestMoves = tempBestMoves;
                
                if (bestScore >= Integer.MAX_VALUE - 1000) {
                    break;
                }
            }
            
            currentDepth++;
        }
        
        if (bestMoves.isEmpty() && !possibleMoves.isEmpty()) {
            bestMoves.add(possibleMoves.getFirst());
        }
        
        return bestMoves;
    }

    private int minMax(Board board, boolean isMaximizing, int depth, int maxDepth) {
        numExploredNodes++;

        if (isTimeUp()) return 0;

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

            board.play(move, currentMark);
            int val = minMax(board, !isMaximizing, depth+1, maxDepth);
            board.undoMove(move);

            if (isMaximizing) {
                bestVal = Math.max(bestVal, val);
            } else {
                bestVal = Math.min(bestVal, val);
            }
        }
        
        return bestVal;
    }
}
