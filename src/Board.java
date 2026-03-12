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

    public int evaluate(Mark mark){
        Mark opponent = (mark == Mark.R) ? Mark.B : Mark.R;
        if (hasWon(mark)) {
            return Integer.MAX_VALUE;
        }
        if (hasWon(opponent)) {
            return Integer.MIN_VALUE;
        }
        
        // Heuristique temporaire selon la position
        int score = 0;
        
        for(int col = 0; col < size; col++){
            for(int row = 0; row < size; row++){
                if(board[col][row] == mark){
                    // Reward pieces closer to opponent's goal
                    if(mark == Mark.R){
                        score += 10 + (7 - row) * 5; // Closer to row 0 is better for Red
                    } else {
                        score += 10 + row * 5; // Closer to row 7 is better for Black
                    }
                } else if(board[col][row] == opponent){
                    if(mark == Mark.R){
                        score -= 10 + row * 5;
                    } else {
                        score -= 10 + (7 - row) * 5;
                    }
                }
            }
        }
        
        return score;
    }
    
    private boolean hasWon(Mark player) {
        if(player == Mark.R){
            for(int col = 0; col < size; col++){
                if(board[col][0] == Mark.R){
                    return true;
                }
            }
            boolean noMoreBlack = true;
            for(int col = 0; col < size; col++){
                for(int row = 0; row < size; row++){
                    if(board[col][row] == Mark.B){
                        noMoreBlack = false;
                        break;
                    }
                }
                if(!noMoreBlack) break;
            }
            if(noMoreBlack){
                return true;
            }
        }
        if(player == Mark.B){
            for(int col = 0; col < size; col++){
                if(board[col][7] == Mark.B){
                    return true;
                }
            }
            boolean noMoreRed = true;
            for(int col = 0; col < size; col++){
                for(int row = 0; row < size; row++){
                    if(board[col][row] == Mark.R){
                        noMoreRed = false;
                        break;
                    }
                }
                if(!noMoreRed) break;
            }
            if(noMoreRed){
                return true;
            }
        }
        return false;
    }

    public void play(Move m, Mark mark){
        m.setMoveTo(board[m.getEndCol()][m.getEndRow()]);
        board[m.getEndCol()][m.getEndRow()] = mark;
        board[m.getStartCol()][m.getStartRow()] = Mark.EMPTY;
    }

    public void undoMove(Move m) {
        board[m.getEndCol()][m.getEndRow()] = m.getMoveTo();
        board[m.getStartCol()][m.getStartRow()] = m.getPlayer();
    }

    public Mark[][] getBoard() {
        return board;
    }
}
