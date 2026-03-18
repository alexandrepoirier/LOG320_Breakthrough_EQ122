class Board {
    private Mark[][] board;
    private int size;
    private int blackCount = 0;
    private int redCount = 0;

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
            if(Integer.parseInt(boardValues[i]) == 2){
                board[x][y] = Mark.B;
                blackCount++;
            }
            else if(Integer.parseInt(boardValues[i]) == 4){
                board[x][y] = Mark.R;
                redCount++;
            }
            else{
                board[x][y] = Mark.EMPTY;
            }

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
        this.redCount = b.redCount;
        this.blackCount = b.blackCount;

        for (int row = 0; row < b.size; row++) {
            System.arraycopy(b.board[row], 0, board[row], 0, b.size);
        }
    }

    public int evaluate(Mark player, Mark opponent) {
        if (hasWon(player)) {
            return Scoring.WIN_SCORE;
        }
        if (hasWon(opponent)) {
            return Scoring.LOSE_SCORE;
        }

        int score = 0;

        // Optimizing code by pre-filtering player's mark
        if(player == Mark.R){
            // Ignore first row to save time
            for(int col = 0; col < size; col++){
                for(int row = 1; row < size; row++){
                    if(board[col][row] == player){
                        // Reward pieces closer to opponent's zone, which is row 0
                        score += Scoring.PLAYER_MARK + (7 - row) * Scoring.POS_FACTOR;
                    } else if(board[col][row] == opponent){
                        // Opponent mark in player territory is bad
                        score -= Scoring.OPPONENT_MARK + row * (int)Math.pow(1.1, row) * Scoring.POS_FACTOR;
                    }
                }
            }
        }else{
            // Ignore last row to save time
            for(int col = 0; col < size; col++){
                for(int row = 0; row < size - 1; row++){
                    if(board[col][row] == player){
                        // Reward pieces closer to opponent's zone, which is row 8
                        score += Scoring.PLAYER_MARK + row * Scoring.POS_FACTOR;
                    } else if(board[col][row] == opponent){
                        score -= Scoring.OPPONENT_MARK + (7 - row) * (int)Math.pow(1.1, (7-row)) * Scoring.POS_FACTOR;
                    }
                }
            }
        }
        
        return score;
    }
    
    public boolean hasWon(Mark player) {
        if(player == Mark.R){
            for(int col = 0; col < size; col++){
                if(board[col][0] == Mark.R){
                    return true;
                }
            }

            return blackCount == 0;
        }

        if(player == Mark.B){
            for(int col = 0; col < size; col++){
                if(board[col][7] == Mark.B){
                    return true;
                }
            }

            return redCount == 0;
        }

        return false;
    }

    public void play(Move m){
        m.setTarget(board[m.getEndCol()][m.getEndRow()]);
        board[m.getEndCol()][m.getEndRow()] = m.getPlayer();
        board[m.getStartCol()][m.getStartRow()] = Mark.EMPTY;

        if(m.getTarget() == Mark.B){
            blackCount--;
        }else if(m.getTarget() == Mark.R){
            redCount--;
        }
    }

    public void undoMove(Move m) {
        board[m.getEndCol()][m.getEndRow()] = m.getTarget();
        board[m.getStartCol()][m.getStartRow()] = m.getPlayer();

        if(m.getTarget() == Mark.B){
            blackCount++;
        }else if(m.getTarget() == Mark.R){
            redCount++;
        }
    }

    public Mark[][] getBoard() {
        return board;
    }

    public double generateUniqueId() {
        int index = 0;
        double id = 0;

        for(int row = 0; row < size; row++){
            for(int col = 0; col < size; col++){
                // test this algo for collisions
                id += (board[col][row].value() * Math.pow(13, index++)) % 4611686018427388039L;
            }
        }

        return id;
    }
}
