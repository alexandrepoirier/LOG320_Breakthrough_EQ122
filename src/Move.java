class Move
{
    private byte[] start;
    private byte[] end;
    private final Mark player;
    private Mark moveTo;

    public Move(byte[] start, byte[] end, Mark player) {
        this.start = start;
        this.end = end;
        this.player = player;
    }

    public Move(String str, Mark player) throws InvalidMoveException{
        if (str.length() == 4){
            try{
                this.player = player;
                setMoveInternal(str.substring(0, 2).toCharArray(), str.substring(2, 4).toCharArray());
            }catch (Exception e){
                throw new InvalidMoveException();
            }
        }else if (str.length() == 5){
            try{
                this.player = player;
                setMoveInternal(str.split("-")[0].toCharArray(), str.split("-")[1].toCharArray());
            }catch (Exception e){
                throw new InvalidMoveException();
            }
        }else {
            throw new InvalidMoveException("Move contains wrong number of characters");
        }
    }

    private void setMoveInternal(char[] start, char[] end) throws InvalidMoveException{
        if (Client.DEBUG_MODE) System.out.println(String.format("Called : setMove(%c%c, %c%c)", start[0], start[1], end[0], end[1]));
        this.start = new byte[] {(byte)(start[0] - 65), (byte)(7-(start[1] - 49))};
        this.end = new byte[] {(byte)(end[0] - 65), (byte)(7-(end[1] - 49))};
        validateMoveInternal();
    }

    private void validateMoveInternal() throws InvalidMoveException{
        // Validate bounds
        if ( !(start[0] >= 0 && start[0] <= 8
                && start[1] >= 0 && start[1] <= 8
                && end[0] >= 0 && end[0] <= 8
                && end[1] >= 0 && end[1] <= 8)
        )
        {
            throw new InvalidMoveException("Move out of bounds");
        }

        // Validate move itself
        if ( Math.abs(start[0] - end[0]) > 1
                || (player == Mark.B && (end[1] - start[1]) != 1)
                || (player == Mark.R && (end[1] - start[1]) != -1)
                || (start[0] - end[0] == 0 && start[1] - end[1] == 0)){
            throw new InvalidMoveException("Move direction is wrong");
        }
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append((char)(start[0]+65));
        sb.append((char)(7-start[1] + 49));
        sb.append("-");
        sb.append((char)(end[0]+65));
        sb.append((char)(7-end[1] + 49));

        return sb.toString();
    }

    public byte getStartRow() { return start[1]; }
    public byte getStartCol() { return start[0]; }
    public byte getEndRow() { return end[1]; }
    public byte getEndCol() { return end[0]; }
    public Mark getPlayer() { return player; }
    public void setMoveTo(Mark moveTo) { this.moveTo = moveTo; }
    public Mark getMoveTo() { return moveTo; }
}

class InvalidMoveException extends Exception
{
    public InvalidMoveException(){}
    public InvalidMoveException(String message) { super(message); }
}
