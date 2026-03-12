import java.util.ArrayList;

class CPUPlayer
{
    private Mark cpuMark;
    private Mark opponentMark;
    private GameSearch searchEngine;

    public CPUPlayer(Mark cpu){
        this.cpuMark = cpu;
        this.opponentMark = (cpu == Mark.R) ? Mark.B : Mark.R;
        this.searchEngine = new GameSearch(cpuMark, opponentMark);
    }

    public int  getNumOfExploredNodes(){
        return searchEngine.getNumExploredNodes();
    }

    public Mark getCpuMark(){
        return cpuMark;
    }

    public Mark getOpponentMark(){
        return opponentMark;
    }

    public ArrayList<Move> getPossibleMoves(Board board, Mark mark){
        return MoveGenerator.getPossibleMoves(board, mark);
    }

    public Move getBestMove(Board board){
        Board copyBoard = new Board(board);
        return searchEngine.getNextMoveAB(copyBoard).getFirst();
    }

    public ArrayList<Move> getNextMoveAB(Board board) {
        return searchEngine.getNextMoveAB(board);
    }
}
