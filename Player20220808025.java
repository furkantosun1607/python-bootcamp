package players;
import game.*;
import java.util.List;

public class Player20220808025 extends Player {
    private Move lastMove = null;
    private final int SEARCH_DEPTH = 7;
    private final int TIME_LIMIT_MS = 950; // 950ms süre sınırı

    public Player20220808025(Board board) {
        super(board);
    }

    @Override
    public Move nextMove() {
        long startTime = System.currentTimeMillis();
        List<Move> possibleMoves = board.getPossibleMoves();
        if (possibleMoves.isEmpty()) return null;

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : possibleMoves) {
            if (System.currentTimeMillis() - startTime >= TIME_LIMIT_MS) {
                break; // Süre sınırını aşıyorsak en iyi bulduğumuz hamleyi dön
            }
            // Her hamle için board durumunun bir kopyasını oluşturup hamleyi uygular.
            Board simulatedBoard = new Board(board);
            if (!simulatedBoard.applyMove(move)) continue;

            int val;
            // board size 10 ise alınabilecek en yüksek puanı alır
            if (board.getSize() == 10) {
                val = fullSearch(simulatedBoard, startTime);
            } else {// board size 25 ise search depth 1 azaltılır
                if (board.getSize() == 25) {
                    val = minimax(simulatedBoard, SEARCH_DEPTH - 1, move, startTime);
                } else {
                    //  board size 50 ise search depth 3 azaltılır
                    val = minimax(simulatedBoard, SEARCH_DEPTH - 3, move, startTime);
                }
            }

            int newRow = board.getPlayerRow() + move.getDRow();
            int newCol = board.getPlayerCol() + move.getDCol();
            int boardSize = board.getSize();
            if (newRow < 2 || newRow > boardSize - 3 || newCol < 2 || newCol > boardSize - 3) {
                val -= 5;
            }

            if (val > bestValue) {
                bestValue = val;
                bestMove = move;
            }
        }

        lastMove = bestMove;
        return bestMove;
    }


    /**
     * depth sıfıra inene veya oyun bitene kadar hamleleri simüle eden ve en iyi sonucu döndüren fonksiyon.
     */

    private int minimax(Board boardState, int depth, Move previousMove, long startTime) {
        if (depth == 0 || boardState.isGameOver() || System.currentTimeMillis() - startTime >= TIME_LIMIT_MS) {
            return evaluateBoard(boardState);
        }

        int best = Integer.MIN_VALUE;
        List<Move> moves = boardState.getPossibleMoves();
        for (Move move : moves) {
            Board newBoard = new Board(boardState);
            if (!newBoard.applyMove(move)) continue;

            int score = minimax(newBoard, depth - 1, move, startTime);
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

    // 10x10 boardlar için alınabilecek en yüksek puanı her yolu deneyerek alır
    private int fullSearch(Board boardState, long startTime) {
        if (boardState.isGameOver() || System.currentTimeMillis() - startTime >= TIME_LIMIT_MS) {
            return evaluateBoard(boardState);
        }

        int best = Integer.MIN_VALUE;
        List<Move> moves = boardState.getPossibleMoves();
        for (Move move : moves) {
            Board newBoard = new Board(boardState);
            if (!newBoard.applyMove(move)) continue;

            int score = fullSearch(newBoard, startTime);
            best = Math.max(best, score);
        }

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
