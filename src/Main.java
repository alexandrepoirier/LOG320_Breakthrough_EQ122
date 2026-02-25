import java.util.Scanner;
import java.util.ArrayList;


public class Main {
    public static void main(String[] args) {
        final int BOARD_SIZE = 3;
        Scanner scan = new Scanner(System.in);

        Board board = new Board(BOARD_SIZE);
        
        // Setup players
        System.out.println("Tic-Tac-Toe Game");
        System.out.println("Choose your mark (X or O): ");
        String playerChoice = scan.nextLine().toUpperCase();
        
        Mark humanMark = playerChoice.equals("X") ? Mark.X : Mark.O;
        Mark cpuMark = (humanMark == Mark.X) ? Mark.O : Mark.X;
        
        CPUPlayer cpu = new CPUPlayer(cpuMark);
        
        System.out.println("Choose algorithm (1 for MinMax, 2 for Alpha-Beta): ");
        int algorithm = scan.nextInt();
        
        Mark currentPlayer = Mark.X;

        while (!board.isGameOver()) {
            board.displayBoard();
            
            if (currentPlayer == humanMark) {
                // tour du joueur
                System.out.println("Your turn (" + humanMark + ")");
                System.out.print("Enter row (0-" + (BOARD_SIZE-1) + "): ");
                int row = scan.nextInt();
                System.out.print("Enter col (0-" + (BOARD_SIZE-1) + "): ");
                int col = scan.nextInt();
                
                try {
                    board.play(new Move(row, col), humanMark);
                } catch (Exception e) {
                    System.out.println("Invalid move: " + e.getMessage());
                    continue;
                }
            } else {
                // AI turn
                System.out.println("CPU's turn (" + cpuMark + ")");
                ArrayList<Move> bestMoves;
                
                if (algorithm == 1) {
                    bestMoves = cpu.getNextMoveMinMax(board);
                } else {
                    bestMoves = cpu.getNextMoveAB(board);
                }
                
                if (!bestMoves.isEmpty()) {
                    Move cpuMove = bestMoves.get(0);
                    board.play(cpuMove, cpuMark);
                    System.out.println("CPU played at (" + cpuMove.getRow() + ", " + cpuMove.getCol() + ")");
                    System.out.println("Nodes explored: " + cpu.getNumOfExploredNodes());
                }
            }
            
            // Swiiiitch
            currentPlayer = (currentPlayer == Mark.X) ? Mark.O : Mark.X;
        }
        
        // Game over
        board.displayBoard();
        int result = board.evaluate(cpuMark);
        
        if (result == 100) {
            System.out.println("Boo womp.");
        } else if (result == -100) {
            System.out.println("A winner is you!");
        } else {
            System.out.println("Match nul!");
        }
        
        scan.close();
    }
}