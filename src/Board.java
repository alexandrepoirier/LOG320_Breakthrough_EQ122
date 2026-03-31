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

    public boolean equals(Board b) {
        if(this.size != b.size) { return false; }

        for(int i = 0; i < b.size; i++){
            for(int j = 0; j < b.size; j++){
                if(board[i][j] != b.board[i][j]) { return false; }
            }
        }

        return true;
    }

    public int algo1(Mark player, Mark opponent){
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
                for(int row = 0; row < size; row++){
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
                for(int row = 0; row < size; row++){
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

    public int algo2(Mark player, Mark opponent){
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
                for(int row = 0; row < size; row++){
                    if(board[col][row] == player){
                        // Reward pieces closer to opponent's zone, which is row 0
                        score += Scoring.PLAYER_MARK + (7 - row) * Scoring.POS_FACTOR;
                    } else if(board[col][row] == opponent){
                        // Opponent mark in player territory is bad
                        score -= Scoring.OPPONENT_MARK + row * Scoring.POS_FACTOR;
                    }
                }
            }
        }else{
            // Ignore last row to save time
            for(int col = 0; col < size; col++){
                for(int row = 0; row < size; row++){
                    if(board[col][row] == player){
                        // Reward pieces closer to opponent's zone, which is row 8
                        score += Scoring.PLAYER_MARK + row * Scoring.POS_FACTOR;
                    } else if(board[col][row] == opponent){
                        score -= Scoring.OPPONENT_MARK + (7 - row) * Scoring.POS_FACTOR;
                    }
                }
            }
        }

        return score;
    }


    public int algo3(Mark player, Mark opponent){
        if (hasWon(player)) return Integer.MAX_VALUE;
        if (hasWon(opponent)) return Integer.MIN_VALUE;

        int score = 0;

        // Poids
        final int P_PIECE = 20;
        final int P_AVANCE = 5;
        final int P_MENACE = -10;
        final int P_PROTEGE = 10;
        final int P_MOBILITE = 10;

        for(int col = 0; col < size; col++){
            for(int row = 0; row < size; row++){
                Mark cell = board[col][row];
                if(cell == Mark.EMPTY) continue;

                int val = 0;
                int av = 0;

                if (cell == Mark.R) {
                    av = (7 - row);
                } else {
                    av = row;
                }

                // 1. Avancement (Quadratique pour encourager la fin)
                val += P_PIECE + (av * av * P_AVANCE);
                // 2. Sécurité
                if (isThreatened(col, row, cell)) val += P_MENACE * av;
                if (isProtected(col, row, cell)) val += P_PROTEGE;
                if (isBlocking(col, row, cell)) val += P_MOBILITE;

                // 3. Mobilité
                val -= compteMobilite(col, row, cell) * P_MOBILITE;

                score += (cell == player) ? val : -val;
            }
        }

        return score;
    }

    private boolean isThreatened(int col, int row, Mark player) {
        Mark opponent = (player == Mark.R) ? Mark.B : Mark.R;
        // L'ennemi attaque depuis 'devant' lui.
        // Si je suis R (va vers 0), l'ennemi B (va vers 7) est en row-1.
        int rowSrcEnnemi = (player == Mark.R) ? row - 1 : row + 1;

        if (rowSrcEnnemi < 0 || rowSrcEnnemi >= size) return false;

        if (col > 0 && board[col-1][rowSrcEnnemi] == opponent) return true;
        if (col < size-1 && board[col+1][rowSrcEnnemi] == opponent) return true;

        return false;
    }

    private boolean isProtected(int col, int row, Mark moi) {
        // Ami derrière moi
        int rowAmi = (moi == Mark.R) ? row + 1 : row - 1;

        if (rowAmi < 0 || rowAmi >= size) return false;

        if (col > 0 && board[col-1][rowAmi] == moi) return true;
        if (col < size-1 && board[col+1][rowAmi] == moi) return true;

        return false;
    }

    private int compteMobilite(int col, int row, Mark player) {
        Mark opponent = (player == Mark.R) ? Mark.B : Mark.R;
        int mob = 0;
        int rowDest = (player == Mark.R) ? row - 1 : row + 1;

        if (rowDest < 0 || rowDest >= size) return 0;

        if (board[col][rowDest] == Mark.EMPTY) mob++;
        if (col > 0 && board[col-1][rowDest] == Mark.EMPTY) mob++;
        if (col > 0 && board[col-1][rowDest] == opponent) mob+=2;
        if (col < size-1 && board[col+1][rowDest] == Mark.EMPTY) mob++;
        if (col < size-1 && board[col+1][rowDest] == opponent) mob+=2;

        return mob;
    }

    private boolean isBlocking(int col, int row, Mark player) {
        // favorise de continuer à bloquer une pièce
        Mark opponent = (player == Mark.R) ? Mark.B : Mark.R;
        int rowDest = (player == Mark.R) ? row - 1 : row + 1;
        if (rowDest < 0 || rowDest >= size) return false;
        if(board[col][rowDest] == opponent) return true;
        return false;
    }

    public int evaluate(Mark player, Mark opponent) {
        return algo3(player, opponent);
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

    public long generateUniqueId() {
        long id = 0;

        for(int row = 0; row < size; row++){
            long rowPower = (long)Math.pow(31, row);
            for(int col = 0; col < size; col++){
                id += (board[col][row].value() * (long)Math.pow(13, col) * rowPower) % 4611686018427388039L;
            }
        }

        return id;
    }
}
