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
        if (hasWon(mark)) return Integer.MAX_VALUE;
        if (hasWon(opponent)) return Integer.MIN_VALUE;
        
        int score = 0;
        
        // Poids
        final int P_PIECE = 20;
        final int P_AVANCE = 5;
        final int P_MENACE = -30;
        final int P_PROTEGE = 10;
        final int P_MOBILITE = 1;

        for(int col = 0; col < size; col++){
            for(int row = 0; row < size; row++){
                Mark cell = board[col][row];
                if(cell == Mark.EMPTY) continue;
                
                int val = 0;
                
                // 1. Avancement (Quadratique pour encourager la fin)
                if (cell == Mark.R) {
                    int av = (7 - row);
                    val += P_PIECE + (av * av * P_AVANCE);
                } else {
                    int av = row;
                    val += P_PIECE + (av * av * P_AVANCE);
                }

                // 2. Sécurité
                if (estMenace(col, row, cell)) val += P_MENACE;
                if (estProtege(col, row, cell)) val += P_PROTEGE;
                
                // 3. Mobilité
                val += compteMobilite(col, row, cell) * P_MOBILITE;

                score += (cell == mark) ? val : -val;
            }
        }
        
        return score;
    }
    
    private boolean estMenace(int col, int row, Mark moi) {
        Mark ennemi = (moi == Mark.R) ? Mark.B : Mark.R;
        // L'ennemi attaque depuis 'devant' lui.
        // Si je suis R (va vers 0), l'ennemi B (va vers 7) est en row-1.
        int rowSrcEnnemi = (moi == Mark.R) ? row - 1 : row + 1;
        
        if (rowSrcEnnemi < 0 || rowSrcEnnemi >= size) return false;
        
        if (col > 0 && board[col-1][rowSrcEnnemi] == ennemi) return true;
        if (col < size-1 && board[col+1][rowSrcEnnemi] == ennemi) return true;
        return false;
    }
    
    private boolean estProtege(int col, int row, Mark moi) {
        // Ami derrière moi
        int rowAmi = (moi == Mark.R) ? row + 1 : row - 1;
        
        if (rowAmi < 0 || rowAmi >= size) return false;
        
        if (col > 0 && board[col-1][rowAmi] == moi) return true;
        if (col < size-1 && board[col+1][rowAmi] == moi) return true;
        return false;
    }

    private int compteMobilite(int col, int row, Mark moi) {
        int mob = 0;
        int rowDest = (moi == Mark.R) ? row - 1 : row + 1;
        
        if (rowDest < 0 || rowDest >= size) return 0;
        
        if (board[col][rowDest] == Mark.EMPTY) mob++;
        if (col > 0 && board[col-1][rowDest] != moi) mob++; // Manger ou bouger
        if (col < size-1 && board[col+1][rowDest] != moi) mob++;
        
        return mob;
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
