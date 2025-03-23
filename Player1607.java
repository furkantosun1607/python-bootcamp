package players;
import game.*;
import java.util.List;

public class Player1607 extends Player {
    private Move lastMove = null;
    // Derinlik parametresi: daha yüksek değer daha uzun vadeli arama sağlar fakat zaman maliyeti artar.
    private final int SEARCH_DEPTH = 4;

    public Player1607(Board board) {
        super(board);
    }

    @Override
    public Move nextMove() {
        List<Move> possibleMoves = board.getPossibleMoves();
        if (possibleMoves.isEmpty()) return null;

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : possibleMoves) {
            // Her hamle için board durumunun bir kopyasını oluşturup hamleyi uygula.
            Board simulatedBoard = new Board(board);
            if (!simulatedBoard.applyMove(move)) continue;

            // Geleceğe yönelik değeri minimax benzeri arama ile hesapla.
            int value = minimax(simulatedBoard, SEARCH_DEPTH - 1, move);

            // Ters hamle cezası: Bir önceki hamlenin tam tersine gitmek isteniyorsa ekstra ceza.
            if (lastMove != null && isReverse(lastMove, move)) {
                value -= 10;
            }

            // Kenar hamle cezası: Tahta kenarlarına yakın hamleler riskli olabilir.
            int newRow = board.getPlayerRow() + move.getDRow();
            int newCol = board.getPlayerCol() + move.getDCol();
            int boardSize = board.getSize();
            if (newRow < 2 || newRow > boardSize - 3 || newCol < 2 || newCol > boardSize - 3) {
                value -= 5;
            }

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }

        lastMove = bestMove;
        return bestMove;
    }

    /**
     * depth sıfıra inene veya oyun bitene kadar hamleleri simüle eden ve en iyi sonucu döndüren fonksiyon.
     */
    private int minimax(Board boardState, int depth, Move previousMove) {
        if (depth == 0 || boardState.isGameOver()) {
            return evaluateBoard(boardState);
        }

        int best = Integer.MIN_VALUE;
        List<Move> moves = boardState.getPossibleMoves();
        for (Move move : moves) {
            Board newBoard = new Board(boardState);
            if (!newBoard.applyMove(move)) continue;

            int score = minimax(newBoard, depth - 1, move);
            // Hemen ters hamle yapılırsa ceza uygula.
            if (previousMove != null && isReverse(previousMove, move)) {
                score -= 10;
            }
            best = Math.max(best, score);
        }

        // Ek olarak, o anki mobiliteyi (yapılabilecek hamle sayısını) da hesaba katarak bonus ekle.
        best += boardState.getPossibleMoves().size();
        return best;
    }

    /**
     * Mevcut board durumunu değerlendirirken, ziyaret edilmiş kare sayısı (skor)
     * ve mobilite (mevcut geçerli hamle sayısı) gibi faktörleri hesaba katar.
     */
    private int evaluateBoard(Board boardState) {
        return boardState.getScore() + 2 * boardState.getPossibleMoves().size();
    }

    /**
     * İki hamlenin birbirinin tersi olup olmadığını kontrol eder.
     */
    private boolean isReverse(Move m1, Move m2) {
        return m1.getDRow() == -m2.getDRow() && m1.getDCol() == -m2.getDCol();
    }
}
