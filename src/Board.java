
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

    public int evaluate(Mark player, Mark opponent) {
        if (hasWon(player)) return Scoring.WIN_SCORE;
        if (hasWon(opponent)) return Scoring.LOSE_SCORE;

        int score = 0;
        int playerFrontDist = 7;
        int opponentFrontDist = 7;

        for (int col = 0; col < size; col++) {
            for (int row = 0; row < size; row++) {
                Mark cell = board[col][row];
                if (cell == Mark.EMPTY) continue;

                boolean isPlayer = (cell == player);
                Mark enemy = isPlayer ? opponent : player;
                int dir = (cell == Mark.R) ? -1 : 1;
                int distToGoal = (cell == Mark.R) ? row : 7 - row;

                if (isPlayer && distToGoal < playerFrontDist) playerFrontDist = distToGoal;
                if (!isPlayer && distToGoal < opponentFrontDist) opponentFrontDist = distToGoal;

                int val = 10;

                if (distToGoal <= 2) {
                    val += (7 - distToGoal) * (7 - distToGoal) * 3;
                } else {
                    val += (7 - distToGoal) * 5;
                }

                int threatRow = row - dir;
                boolean threatened = false;
                if (threatRow >= 0 && threatRow < size) {
                    if (col > 0 && board[col - 1][threatRow] == enemy) threatened = true;
                    if (col < size - 1 && board[col + 1][threatRow] == enemy) threatened = true;
                }

                boolean protectedByAlly = false;
                if (threatRow >= 0 && threatRow < size) {
                    if (col > 0 && board[col - 1][threatRow] == cell) protectedByAlly = true;
                    if (col < size - 1 && board[col + 1][threatRow] == cell) protectedByAlly = true;
                }

                if (threatened && !protectedByAlly) {
                    val -= 40;
                } else if (threatened && protectedByAlly) {
                    val -= 8;
                }
                if (protectedByAlly) {
                    val += 8;
                }

                int attackRow = row + dir;
                if (attackRow >= 0 && attackRow < size) {
                    if (col > 0 && board[col - 1][attackRow] == enemy) val += 25;
                    if (col < size - 1 && board[col + 1][attackRow] == enemy) val += 25;
                }

                if (distToGoal > 0 && distToGoal <= 4) {
                    boolean free = true;
                    for (int r = row + dir; r >= 0 && r < size; r += dir) {
                        if (board[col][r] == enemy) { free = false; break; }
                        if (col > 0 && board[col - 1][r] == enemy) { free = false; break; }
                        if (col < size - 1 && board[col + 1][r] == enemy) { free = false; break; }
                    }
                    if (free) {
                        val += (8 - distToGoal) * 25;
                    }
                }

                int homeRow = (cell == Mark.R) ? 7 : 0;
                int distFromHome = Math.abs(row - homeRow);
                if (distFromHome >= 2 && distFromHome <= 5) {
                    if (col > 0 && board[col - 1][row] == cell) val += 6;
                    if (col < size - 1 && board[col + 1][row] == cell) val += 6;
                }

                score += isPlayer ? val : -val;
            }
        }

        int playerCount = (player == Mark.R) ? redCount : blackCount;
        int opponentCount = (player == Mark.R) ? blackCount : redCount;
        score += (playerCount - opponentCount) * 30;

        if (playerFrontDist < opponentFrontDist) {
            score += (opponentFrontDist - playerFrontDist) * 20;
        } else if (opponentFrontDist < playerFrontDist) {
            score -= (playerFrontDist - opponentFrontDist) * 10;
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

    public void recount() {
        redCount = 0;
        blackCount = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == Mark.R) redCount++;
                else if (board[i][j] == Mark.B) blackCount++;
            }
        }
    }

    public long generateUniqueId() {
        long id = 0;
        for (int row = 0; row < size; row++) {
            long rowPower = (long) Math.pow(31, row);
            for (int col = 0; col < size; col++) {
                id += (board[col][row].value() * (long) Math.pow(13, col) * rowPower) % 4611686018427388039L;
            }
        }
        return id;
    }
}
