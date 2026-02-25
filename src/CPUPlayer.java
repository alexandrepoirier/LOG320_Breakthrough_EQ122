import java.util.ArrayList;

// IMPORTANT: Il ne faut pas changer la signature des méthodes
// de cette classe, ni le nom de la classe.
// Vous pouvez par contre ajouter d'autres méthodes (ça devrait 
// être le cas)
class CPUPlayer
{
    private Mark cpuMark;
    private Mark opponentMark;

    // Contient le nombre de noeuds visités (le nombre
    // d'appel à la fonction MinMax ou Alpha Beta)
    // Normalement, la variable devrait être incrémentée
    // au début de votre MinMax ou Alpha Beta.
    private int numExploredNodes;

    // Le constructeur reçoit en paramètre le
    // joueur MAX (X ou O)
    public CPUPlayer(Mark cpu){
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.X) ? Mark.O : Mark.X;
    }

    // Ne pas changer cette méthode
    public int  getNumOfExploredNodes(){
        return numExploredNodes;
    }

    // Retourne la liste des coups possibles.  Cette liste contient
    // plusieurs coups possibles si et seuleument si plusieurs coups
    // ont le même score.
    public ArrayList<Move> getNextMoveMinMax(Board board)
    {
        numExploredNodes = 0;
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        int bestScore = Integer.MIN_VALUE;
        
        ArrayList<Move> possibleMoves = board.getEmptySpaces();
        
        System.out.println("\nMinMax: Evaluation des moves ppossibles");
        for (Move move : possibleMoves) {
            board.play(move, cpuMark);
            int score = minMax(board, false);
            board.undoMove(move);
            
            System.out.println("Move (" + move.getRow() + ", " + move.getCol() + ") -> Score: " + score);
            
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }
        System.out.println("Meilleur score: " + bestScore);
        System.out.print("Meilleurs moves: ");
        for (Move m : bestMoves) {
            System.out.print("(" + m.getRow() + ", " + m.getCol() + ") ");
        }
        System.out.println();
        
        return bestMoves;
    }

    private int minMax(Board board, boolean isMaximizing) {
        numExploredNodes++;
        
        if (board.isGameOver()) {
            return board.evaluate(cpuMark);
        }
        
        ArrayList<Move> possibleMoves = board.getEmptySpaces();
        
        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, cpuMark);
                int score = minMax(board, false);
                board.undoMove(move);
                maxScore = Math.max(maxScore, score);
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, opponentMark);
                int score = minMax(board, true);
                board.undoMove(move);
                minScore = Math.min(minScore, score);
            }
            return minScore;
        }
    }

    // Retourne la liste des coups possibles.  Cette liste contient
    // plusieurs coups possibles si et seuleument si plusieurs coups
    // ont le même score.
    public ArrayList<Move> getNextMoveAB(Board board){
        numExploredNodes = 0;
        ArrayList<Move> bestMoves = new ArrayList<Move>();
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        ArrayList<Move> possibleMoves = board.getEmptySpaces();
        
        System.out.println("\n=== Alpha-Beta: Evaluation des moves ===");
        for (Move move : possibleMoves) {
            board.play(move, cpuMark);
            int score = alphaBeta(board, false, alpha, beta);
            board.undoMove(move);
            
            System.out.println("Move (" + move.getRow() + ", " + move.getCol() + ") -> Score: " + score + " [alpha=" + alpha + ", beta=" + beta + "]");
            
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
            
            alpha = Math.max(alpha, bestScore);
        }
        System.out.println("Meilleur score: " + bestScore);
        System.out.print("Meilleurs moves: ");
        for (Move m : bestMoves) {
            System.out.print("(" + m.getRow() + ", " + m.getCol() + ") ");
        }
        System.out.println();
        
        return bestMoves;
    }

    private int alphaBeta(Board board, boolean isMaximizing, int alpha, int beta) {
        numExploredNodes++;
        
        // Terminal state check
        if (board.isGameOver()) {
            return board.evaluate(cpuMark);
        }
        
        ArrayList<Move> possibleMoves = board.getEmptySpaces();
        
        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, cpuMark);
                int score = alphaBeta(board, false, alpha, beta);
                board.undoMove(move);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);
                if (beta <= alpha) {
                    break; 
                }
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                board.play(move, opponentMark);
                int score = alphaBeta(board, true, alpha, beta);
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
