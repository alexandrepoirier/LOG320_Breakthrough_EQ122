import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Aide générique pour l'exécution parallèle des recherches [pur alpha et beta
 */
class ParallelSearchHelper {

    static class ResultatEvaluation {
        final Move coup;
        final int score;
        final int noeudsExplores;

        ResultatEvaluation(Move coup, int score, int noeudsExplores) {
            this.coup = coup;
            this.score = score;
            this.noeudsExplores = noeudsExplores;
        }
    }

    static class ResultatGlobal {
        final ArrayList<Move> meilleursCoups;
        final int meilleurScore;
        final int totalNoeuds;

        ResultatGlobal(ArrayList<Move> meilleursCoups, int meilleurScore, int totalNoeuds) {
            this.meilleursCoups = meilleursCoups;
            this.meilleurScore = meilleurScore;
            this.totalNoeuds = totalNoeuds;
        }
    }

    /**
     * Exécute évaluation en parallèle et retourne le meilleur résultat.
     */
    static ResultatGlobal executerEnParallele(List<Callable<ResultatEvaluation>> taches) {
        int nbThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executeur = Executors.newFixedThreadPool(nbThreads);

        try {
            List<Future<ResultatEvaluation>> futurs = executeur.invokeAll(taches);

            int meilleurScore = Integer.MIN_VALUE;
            int totalNoeuds = 0;
            ArrayList<Move> meilleursCoups = new ArrayList<>();

            for (Future<ResultatEvaluation> futur : futurs) {
                ResultatEvaluation res = futur.get();
                totalNoeuds += res.noeudsExplores;

                if (res.score > meilleurScore) {
                    meilleurScore = res.score;
                    meilleursCoups.clear();
                    meilleursCoups.add(res.coup);
                } else if (res.score == meilleurScore) {
                    meilleursCoups.add(res.coup);
                }
            }

            return new ResultatGlobal(meilleursCoups, meilleurScore, totalNoeuds);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Erreur recherche parallèle", e);
        } finally {
            executeur.shutdownNow();
        }
    }
}

