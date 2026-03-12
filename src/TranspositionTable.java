import java.util.concurrent.ConcurrentHashMap;

/**
 * Table de transposition pour la programmation dynamique.
 * Stocke les résultats des évaluations précédentes pour éviter de recalculer les mêmes positions.
 * Thread-safe pour fonctionner avec le multithreading.
 */
class TranspositionTable {
    private final ConcurrentHashMap<String, TTEntry> table = new ConcurrentHashMap<>(50000);

    // Types de flag
    static final int EXACT = 0;
    static final int LOWERBOUND = 1; // Alpha cut
    static final int UPPERBOUND = 2; // Beta cut

    static class TTEntry {
        int depth;
        int value;
        int flag;

        TTEntry(int depth, int value, int flag) {
            this.depth = depth;
            this.value = value;
            this.flag = flag;
        }
    }

    public TTEntry get(String key) {
        return table.get(key);
    }

    public void put(String key, int depth, int value, int flag) {
        // Stratégie de remplacement simple: on préfère les résultats plus profonds (plus précis)
        // Note: ConcurrentHashMap.compute permet atomicité mais peut être lent. 
        // Ici on accepte une légère "race condition" pour la perf, ou on vérifie juste depth.
        TTEntry existing = table.get(key);
        if (existing == null || depth >= existing.depth) {
            table.put(key, new TTEntry(depth, value, flag));
        }
    }

    public void clear() {
        table.clear();
    }

    public String generateKey(Board board) {
        // Génération simple de clé chaîne. 
        // Pour plus de performance, Zobrist Hashing serait idéal mais complexe à ajouter maintenant.
        StringBuilder sb = new StringBuilder(64);
        Mark[][] b = board.getBoard();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Mark m = b[i][j];
                // Représentation compacte
                if (m == Mark.R) sb.append('R');
                else if (m == Mark.B) sb.append('B');
                else sb.append('.');
            }
        }
        return sb.toString();
    }
}
