import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

class CPUPlayer
{
    private Mark cpuMark;
    private Mark opponentMark;

    private int numExploredNodes;
    private long searchStartTime;
    private static final long TIME_LIMIT_MS = 4800;

    public CPUPlayer(Mark cpu){
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.R) ? Mark.B : Mark.R;
    }

    public int  getNumOfExploredNodes(){
        return numExploredNodes;
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

    public ArrayList<Move> getPossibleMoves(Board board, Mark mark){
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(mark == Mark.R){
            possibleMoves = getPossibleRedMoves(board);
        } else if (mark == Mark.B) {
            possibleMoves = getPossibleBlackMoves(board);
        }
        return possibleMoves;
    }

    private ArrayList<Move> getPossibleBlackMoves(Board board) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (board.getBoard()[col][row] == Mark.B){
                    possibleMoves.addAll(getBlackMove(board,col,row));
                }
            }
        }
        return possibleMoves;
    }

    private ArrayList<Move> getBlackMove(Board board,int col,int row) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(row < 7 && row >= 0){
            if(col - 1 >=0 && board.getBoard()[col-1][row+1] != Mark.B){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col-1), (byte)(row+1)}, Mark.B));
            }
            if(board.getBoard()[col][row+1] == Mark.EMPTY){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col), (byte)(row+1)}, Mark.B));
            }
            if(col+1 <= 7 &&board.getBoard()[col+1][row+1] != Mark.B){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col+1), (byte)(row+1)}, Mark.B));
            }
        }
        return possibleMoves;
    }

    private ArrayList<Move> getPossibleRedMoves(Board board) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                if (board.getBoard()[col][row] == Mark.R){
                    possibleMoves.addAll(getRedMove(board, col, row));
                }
            }
        }
        return possibleMoves;
    }

    private ArrayList<Move> getRedMove(Board board, int col,int row) {
        ArrayList<Move> possibleMoves = new ArrayList<>();
        if(row <= 7 && row > 0){
            if(col - 1 >=0 && board.getBoard()[col-1][row-1] != Mark.R){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col-1), (byte)(row-1)}, Mark.R));
            }
            if(board.getBoard()[col][row-1] == Mark.EMPTY){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col), (byte)(row-1)}, Mark.R));
            }
            if(col+1 <= 7 &&board.getBoard()[col+1][row-1] != Mark.R){
                possibleMoves.add(new Move(new byte[]{(byte)col, (byte)row}, new byte[] {(byte)(col+1), (byte)(row-1)}, Mark.R));
            }
        }
        return possibleMoves;
    }

    public Move getBestMove(Board board){
        // generate all possible moves and pick best!
        Board copyBoard = new Board(board);
        return getNextMoveAB(copyBoard).getFirst();
    }

    public ArrayList<Move> getNextMoveMinMax(Board board)
    {
        ArrayList<Move> coupsPossibles = getPossibleMoves(board, cpuMark);
        if (coupsPossibles.isEmpty()) {
            numExploredNodes = 0;
            return new ArrayList<>();
        }

        // Profondeur fixe de 4
        List<Callable<ParallelSearchHelper.ResultatEvaluation>> taches = new ArrayList<>();
        for (Move coup : coupsPossibles) {
            taches.add(() -> evaluerCoupRacineMinMax(board, coup, 4));
        }

        ParallelSearchHelper.ResultatGlobal res = ParallelSearchHelper.executerEnParallele(taches);
        numExploredNodes = res.totalNoeuds;
        return res.meilleursCoups;
    }

    ParallelSearchHelper.ResultatEvaluation evaluerCoupRacineMinMax(Board board, Move coup, int profMax) {
        Board copie = new Board(board);
        copie.play(coup, cpuMark);
        int[] compteur = new int[]{0};
        // Adapte minMax pour prendre une profondeur max si necessaire, sinon on ignore profMax ici car minMax n'a pas ete modifie pour ca dans cette etape
        int score = minMax(copie, false, 0, compteur, profMax);
        return new ParallelSearchHelper.ResultatEvaluation(coup, score, compteur[0]);
    }

    private int minMax(Board board, boolean isMaximizing, int depth, int[] nodes, int maxDepth) {
        nodes[0]++;

        int boardVal = board.evaluate(cpuMark);
        if (boardVal == Integer.MAX_VALUE){
            return boardVal - depth;
        }
        if (boardVal == Integer.MIN_VALUE){
            return boardVal + depth;
        }

        if (depth >= maxDepth) {
            return boardVal;
        }

        if (isMaximizing) {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
            int maxScore = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, cpuMark);
                int score = minMax(board, false, depth+1, nodes, maxDepth);
                board.undoMove(move);
                maxScore = Math.max(maxScore, score);
            }
            return maxScore;
        } else {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,opponentMark);
            int minScore = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, opponentMark);
                int score = minMax(board, true, depth+1, nodes, maxDepth);
                board.undoMove(move);
                minScore = Math.min(minScore, score);
            }
            return minScore;
        }
    }

    public ArrayList<Move> getNextMoveAB(Board board){
        numExploredNodes = 0;
        searchStartTime = System.currentTimeMillis();
        
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
        
        if (possibleMoves.isEmpty()) {
            return bestMoves;
        }

        int currentDepth = 1;
        int maxDepthReached = 0;
        
        while (!isTimeUp()) {
            List<Callable<ParallelSearchHelper.ResultatEvaluation>> taches = new ArrayList<>();
            final int prof = currentDepth;
            // Variable partagée pour communiquer le meilleur score trouvé entre les threads
            AtomicInteger globalAlpha = new AtomicInteger(Integer.MIN_VALUE);

            for (Move move : possibleMoves) {
                taches.add(() -> evaluerCoupRacineAlphaBeta(board, move, prof, globalAlpha));
            }

            ParallelSearchHelper.ResultatGlobal resultatDepth = ParallelSearchHelper.executerEnParallele(taches);

            if (isTimeUp()) {
                break;
            }

            bestMoves = resultatDepth.meilleursCoups;
            maxDepthReached = currentDepth;
            numExploredNodes += resultatDepth.totalNoeuds;

            if (resultatDepth.meilleurScore >= Integer.MAX_VALUE - 1000) {
                break;
            }
            
            currentDepth++;
        }
        
        if (bestMoves.isEmpty() && !possibleMoves.isEmpty()) {
            bestMoves.add(possibleMoves.getFirst());
        }
        
        return bestMoves;
    }

    ParallelSearchHelper.ResultatEvaluation evaluerCoupRacineAlphaBeta(Board board, Move coup, int profMax, AtomicInteger globalAlpha) {
        Board copie = new Board(board);
        copie.play(coup, cpuMark);
        int[] compteur = new int[]{0};
        // Alpha initial: on prend le maximum entre -Inf et ce que les autres threads ont déjà trouvé
        int alphaInitial = Integer.MIN_VALUE;
        if (globalAlpha != null) {
            alphaInitial = Math.max(alphaInitial, globalAlpha.get());
        }

        int score = alphaBeta(copie, false, 0, alphaInitial, Integer.MAX_VALUE, profMax, compteur, globalAlpha);
        
        // Mettre à jour l'alpha global si on a trouvé mieux
        if (globalAlpha != null) {
            globalAlpha.updateAndGet(current -> Math.max(current, score));
        }

        return new ParallelSearchHelper.ResultatEvaluation(coup, score, compteur[0]);
    }

    private int alphaBeta(Board board, boolean isMaximizing, int depth, int alpha, int beta, int maxDepth, int[] nodes, AtomicInteger globalAlpha) {
        nodes[0]++;

        if (isTimeUp()) {
            return 0;
        }

        // Mise à jour de alpha avec la valeur partagée globale pour élaguer plus fort
        if (globalAlpha != null) {
            alpha = Math.max(alpha, globalAlpha.get());
        }
        
        if (beta <= alpha) {
            return alpha; // Elagage immédiat grâce à l'info des autres threads
        }

        int boardVal = board.evaluate(cpuMark);
        if (boardVal == Integer.MAX_VALUE){
            return boardVal - depth;
        }
        if (boardVal == Integer.MIN_VALUE){
            return boardVal + depth;
        }
        
        if (depth >= maxDepth) {
            return boardVal;
        }
        
        if (isMaximizing) {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,cpuMark);
            if (possibleMoves.isEmpty()) {
                return Integer.MIN_VALUE + depth;
            }
            int maxScore = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                if (isTimeUp()) {
                    break;
                }
                
                // Rafraîchir alpha avant chaque coup pour bénéficier des découvertes récentes
                if (globalAlpha != null) {
                   alpha = Math.max(alpha, globalAlpha.get());
                   if (beta <= alpha) break;
                }

                board.play(move, cpuMark);
                int score = alphaBeta(board, false, depth+1, alpha, beta, maxDepth, nodes, globalAlpha);
                board.undoMove(move);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);
                
                // On ne met PAS à jour globalAlpha ici (seulement à la racine pour éviter les valeurs instables)
                
                if (beta <= alpha) {
                    break; 
                }
            }
            return maxScore;
        } else {
            ArrayList<Move> possibleMoves = getPossibleMoves(board,opponentMark);
            if (possibleMoves.isEmpty()) {
                return Integer.MAX_VALUE - depth;
            }
            int minScore = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                if (isTimeUp()) {
                    break;
                }
                
                // Rafraîchir alpha
                if (globalAlpha != null) {
                   alpha = Math.max(alpha, globalAlpha.get());
                   if (beta <= alpha) break;
                }

                board.play(move, opponentMark);
                int score = alphaBeta(board, true, depth+1, alpha, beta, maxDepth, nodes, globalAlpha);
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
