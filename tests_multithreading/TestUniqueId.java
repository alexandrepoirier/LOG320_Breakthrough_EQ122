import java.util.*;

public class TestUniqueId {
    static Random rng = new Random(42);

    public static void main(String[] args) {
        System.out.println("=== Test UniqueId Collisions ===");

        testRandomBoards();
        testSwappedPieces();
        testSinglePieceDifference();

        System.out.println("\nTOUS LES TESTS PASSES !");
    }

    static void testRandomBoards() {
        System.out.print("Test 500k random boards... ");
        int total = 500_000;
        HashMap<Long, String> seen = new HashMap<>();
        int collisions = 0;

        for (int i = 0; i < total; i++) {
            Board b = randomBoard();
            long id = b.generateUniqueId();
            String key = boardToString(b);

            if (seen.containsKey(id) && !seen.get(id).equals(key)) {
                collisions++;
            } else {
                seen.put(id, key);
            }
        }

        if (collisions > 0) {
            throw new RuntimeException("Found " + collisions + " collisions out of " + total);
        }
        System.out.println("OK (" + seen.size() + " unique hashes)");
    }

    static void testSwappedPieces() {
        System.out.print("Test swapped pieces give different hash... ");
        int failures = 0;

        for (int i = 0; i < 100_000; i++) {
            Board b1 = randomBoard();
            Mark[][] g1 = b1.getBoard();

            // Find two cells with different content
            int c1, r1, c2, r2;
            do {
                c1 = rng.nextInt(8); r1 = rng.nextInt(8);
                c2 = rng.nextInt(8); r2 = rng.nextInt(8);
            } while ((c1 == c2 && r1 == r2) || g1[c1][r1] == g1[c2][r2]);

            // Create a copy with those two cells swapped
            Board b2 = new Board(8);
            Mark[][] g2 = b2.getBoard();
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    g2[x][y] = g1[x][y];
            g2[c1][r1] = g1[c2][r2];
            g2[c2][r2] = g1[c1][r1];
            b2.recount();

            if (b1.generateUniqueId() == b2.generateUniqueId()) {
                failures++;
            }
        }

        if (failures > 0) {
            throw new RuntimeException(failures + " swap pairs produced same hash!");
        }
        System.out.println("OK");
    }

    static void testSinglePieceDifference() {
        System.out.print("Test single piece change gives different hash... ");
        int failures = 0;
        Mark[] marks = {Mark.EMPTY, Mark.R, Mark.B};

        for (int i = 0; i < 100_000; i++) {
            Board b1 = randomBoard();
            int col = rng.nextInt(8), row = rng.nextInt(8);
            Mark original = b1.getBoard()[col][row];

            // Pick a different mark
            Mark newMark;
            do { newMark = marks[rng.nextInt(3)]; } while (newMark == original);

            Board b2 = new Board(8);
            Mark[][] g2 = b2.getBoard();
            Mark[][] g1 = b1.getBoard();
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    g2[x][y] = g1[x][y];
            g2[col][row] = newMark;
            b2.recount();

            if (b1.generateUniqueId() == b2.generateUniqueId()) {
                failures++;
            }
        }

        if (failures > 0) {
            throw new RuntimeException(failures + " single-change pairs produced same hash!");
        }
        System.out.println("OK");
    }

    static Board randomBoard() {
        Board b = new Board(8);
        Mark[][] g = b.getBoard();
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                int r = rng.nextInt(6);
                if (r == 0) g[i][j] = Mark.R;
                else if (r == 1) g[i][j] = Mark.B;
                else g[i][j] = Mark.EMPTY;
            }
        b.recount();
        return b;
    }

    static String boardToString(Board b) {
        StringBuilder sb = new StringBuilder(64);
        Mark[][] g = b.getBoard();
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                sb.append(g[i][j].value());
        return sb.toString();
    }
}
