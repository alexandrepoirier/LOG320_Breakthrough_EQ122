import java.util.*;

public class TestHashComparison {
    static Random rng = new Random(42);

    public static void main(String[] args) {
        System.out.println("=== Hash Comparison Test ===\n");

        testCorrectness();
        testSpeed();
    }

    static void testCorrectness() {
        System.out.print("Correctness: incremental matches full recompute... ");
        int failures = 0;

        for (int t = 0; t < 10_000; t++) {
            Board b = randomBoard();
            Mark[][] g = b.getBoard();

            long fullHash = BoardHash.computeFull(g, 8);
            long incHash = fullHash;

            ArrayList<Move> moves = MoveGenerator.getPossibleMoves(b, Mark.R);
            if (moves.isEmpty()) moves = MoveGenerator.getPossibleMoves(b, Mark.B);
            if (moves.isEmpty()) continue;

            Move m = moves.get(rng.nextInt(moves.size()));

            // Simulate play
            Mark captured = g[m.getEndCol()][m.getEndRow()];
            incHash = BoardHash.updatePlay(incHash, m.getStartCol(), m.getStartRow(), m.getPlayer(),
                    m.getEndCol(), m.getEndRow(), captured);

            b.play(m);
            long fullAfterPlay = BoardHash.computeFull(b.getBoard(), 8);

            if (incHash != fullAfterPlay) failures++;

            // Simulate undo
            incHash = BoardHash.updateUndo(incHash, m.getStartCol(), m.getStartRow(), m.getPlayer(),
                    m.getEndCol(), m.getEndRow(), captured);
            b.undoMove(m);
            long fullAfterUndo = BoardHash.computeFull(b.getBoard(), 8);

            if (incHash != fullAfterUndo) failures++;
        }

        if (failures > 0) throw new RuntimeException(failures + " mismatches!");
        System.out.println("OK\n");
    }

    static void testSpeed() {
        Board b = randomBoard();
        Mark[][] g = b.getBoard();
        ArrayList<Move> moves = MoveGenerator.getPossibleMoves(b, Mark.R);
        if (moves.isEmpty()) moves = MoveGenerator.getPossibleMoves(b, Mark.B);
        Move m = moves.get(0);

        int iterations = 2_000_000;

        // Warm up
        for (int i = 0; i < 100_000; i++) {
            BoardHash.computeFull(g, 8);
        }

        // Full recompute speed
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            BoardHash.computeFull(g, 8);
        }
        long fullTime = System.nanoTime() - start;

        // Incremental speed
        long hash = BoardHash.computeFull(g, 8);
        Mark captured = g[m.getEndCol()][m.getEndRow()];

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            hash = BoardHash.updatePlay(hash, m.getStartCol(), m.getStartRow(), m.getPlayer(),
                    m.getEndCol(), m.getEndRow(), captured);
            hash = BoardHash.updateUndo(hash, m.getStartCol(), m.getStartRow(), m.getPlayer(),
                    m.getEndCol(), m.getEndRow(), captured);
        }
        long incTime = System.nanoTime() - start;

        System.out.printf("Full recompute:  %d ms (%d iterations)%n", fullTime / 1_000_000, iterations);
        System.out.printf("Incremental:     %d ms (%d play+undo pairs)%n", incTime / 1_000_000, iterations);
        System.out.printf("Speedup:         %.1fx%n", (double) fullTime / incTime);
    }

    static Board randomBoard() {
        Board b = new Board(8);
        Mark[][] g = b.getBoard();
        // Place pieces in a realistic-ish layout
        for (int col = 0; col < 8; col++) {
            g[col][6] = Mark.R; g[col][7] = Mark.R;
            g[col][0] = Mark.B; g[col][1] = Mark.B;
        }
        // Scatter a few
        for (int i = 0; i < 6; i++) {
            int c = rng.nextInt(8), r = 2 + rng.nextInt(4);
            g[c][r] = (rng.nextBoolean()) ? Mark.R : Mark.B;
        }
        b.recount();
        return b;
    }
}
