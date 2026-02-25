import java.util.ArrayList;

// IMPORTANT: Il ne faut pas changer la signature des méthodes
// de cette classe, ni le nom de la classe.
// Vous pouvez par contre ajouter d'autres méthodes (ça devrait 
// être le cas)
class Board {
    private Mark[][] board;
    private int size;

    // Ne pas changer la signature de cette méthode
    public Board() {
        this(3);
    
    }

    public Board(int n) {
        this.size = n;
        this.board = new Mark[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = Mark.EMPTY;
            }
        }
    }

    // Place la pièce 'mark' sur le plateau, à la
    // position spécifiée dans Move
    //
    // Ne pas changer la signature de cette méthode
    public void play(Move m, Mark mark){
        int row = m.getRow();
        int col = m.getCol();
        
        // position is valid?
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Invalid position: (" + row + ", " + col + ")");
        }
        
        // position is empty?
        if (board[row][col] != Mark.EMPTY) {
            throw new IllegalStateException("Position (" + row + ", " + col + ") est déjà occupé par " + board[row][col]);
        }
        
        board[row][col] = mark;
    }

    // retourne  100 pour une victoire
    //          -100 pour une défaite
    //           0   pour un match nul
    // Ne pas changer la signature de cette méthode
    public int evaluate(Mark mark){
        Mark opponent = (mark == Mark.X) ? Mark.O : Mark.X;
        if (hasWon(mark)) {
            return 100;
        }
        if (hasWon(opponent)) {
            return -100;
        }
        return 0;
    }
    
    // Check si un certain mark a gagné
    private boolean hasWon(Mark player) {
        // rows
        for (int i = 0; i < size; i++) {
            boolean rowWin = true;
            for (int j = 0; j < size; j++) {
                if (board[i][j] != player) {
                    rowWin = false;
                    break;
                }
            }
            if (rowWin) return true;
        }
        
        // colonne
        for (int j = 0; j < size; j++) {
            boolean colWin = true;
            for (int i = 0; i < size; i++) {
                if (board[i][j] != player) {
                    colWin = false;
                    break;
                }
            }
            if (colWin) return true;
        }
        
        // diag top gauche @ bas droite
        boolean diagWin = true;
        for (int i = 0; i < size; i++) {
            if (board[i][i] != player) {
                diagWin = false;
                break;
            }
        }
        if (diagWin) return true;
        
        // diag top droit @ bas gauche
        boolean antiDiagWin = true;
        for (int i = 0; i < size; i++) {
            if (board[i][size - 1 - i] != player) {
                antiDiagWin = false;
                break;
            }
        }
        if (antiDiagWin) return true;
        
        return false;
    }

    // Display the board
    public void displayBoard() {
        System.out.println();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                String symbol = (board[i][j] == Mark.EMPTY) ? " " : board[i][j].toString();
                System.out.print(" " + symbol + " ");
                if (j < size - 1) {
                    System.out.print("|");
                }
            }
            System.out.println();
            if (i < size - 1) {
                for (int j = 0; j < size; j++) {
                    System.out.print("---");
                    if (j < size - 1) {
                        System.out.print("+");
                    }
                }
                System.out.println();
            }
        }
        System.out.println();
    }

    public ArrayList<Move> getEmptySpaces(){
        ArrayList<Move> empty = new ArrayList<Move>();
        for (int row = 0; row < size; row++){
            for (int col = 0; col < size; col++){
                if (board[row][col] == Mark.EMPTY)
                    empty.add(new Move(row, col));
            }
        }
        return empty;
    }

    public void undoMove(Move m) {
        board[m.getRow()][m.getCol()] = Mark.EMPTY;
    }

    public boolean isGameOver() {
        return hasWon(Mark.X) || hasWon(Mark.O) || getEmptySpaces().isEmpty();
    }
}
