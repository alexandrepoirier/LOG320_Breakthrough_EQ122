import java.util.ArrayList;

class Board {
    private Mark[][] board;
    private int size;

    public Board(int n) {
        this.size = n;
        this.board = new Mark[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = Mark.EMPTY;
            }
        }
    }

    public Board(int n, byte[] boardConfig){
        this(n);

        String s = new String(boardConfig).trim();
        String[] boardValues = s.split(" ");
        int x=0,y=0;

        for (int i = 0; i < boardValues.length; i++){
            board[x][y] = Integer.parseInt(boardValues[i]) == 2 ? Mark.B : Integer.parseInt(boardValues[i]) == 4 ? Mark.R : Mark.EMPTY;

            x++;
            if(x == 8){
                x = 0;
                y++;
            }
        }
    }

    public Board(Board b) {
        this.size = b.size;
        this.board = new Mark[b.size][b.size];

        for (int row = 0; row < b.size; row++) {
            System.arraycopy(b.board[row], 0, board[row], 0, b.size);
        }
    }

    public void play(Move m, Mark mark){
        int row = m.getEndRow();
        int col = m.getEndCol();
        
        // position is valid?
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Invalid position: (" + col + ", " + row + ")");
        }

        m.setMoveTo(board[col][row]);
        board[col][row] = mark;
        board[m.getStartCol()][m.getStartRow()] = Mark.EMPTY;
    }

    public int evaluate(Mark mark){
        Mark opponent = (mark == Mark.R) ? Mark.B : Mark.R;
        if (hasWon(mark)) {
            return 100;
        }
        if (hasWon(opponent)) {
            return -100;
        }
        return 0;
    }
    
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
                    empty.add(new Move(new byte[] {-1, -1}, new byte[] {-1, -1}, Mark.UNKNOWN)); // TODO remplacer cette méthode
            }
        }
        return empty;
    }

    public void undoMove(Move m) {
        board[m.getEndCol()][m.getEndRow()] = m.getMoveTo();
        board[m.getStartCol()][m.getStartRow()] = m.getPlayer();
    }

    public boolean isGameOver() {
        return hasWon(Mark.R) || hasWon(Mark.B) || getEmptySpaces().isEmpty();
    }

    public Mark[][] getBoard() {
        return board;
    }
}
