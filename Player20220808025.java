package players;

import game.*;
import java.util.List;
import java.util.Random;

public class Player20220808025 extends Player {
    private final int boardSize;
    private final Random random;

    // Parameters for Monte Carlo simulation
    private static final int SIMULATION_COUNT = 5000;

    // Parameters for Minimax algorithm
    private final int SEARCH_DEPTH = 7;
    private final int TIME_LIMIT_MS = 950; // 950ms time limit

    public Player20220808025(Board board) {
        super(board);
        this.boardSize = board.getSize();
        this.random = new Random();
    }

    @Override
    public Move nextMove() {
        if (boardSize <= 10) {
            return monteCarloNextMove();
        } else {
            return minimaxNextMove();
        }
    }

    private Move monteCarloNextMove() {
        List<Move> possibleMoves = board.getPossibleMoves();
        if (possibleMoves.isEmpty()) return null;

        Move bestMove = null;
        double bestScore = -1;

        for (Move move : possibleMoves) {
            double averageScore = simulateMove(move);
            if (averageScore > bestScore) {
                bestScore = averageScore;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private double simulateMove(Move move) {
        double totalScore = 0;
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            Board simulationBoard = new Board(board);
            simulationBoard.applyMove(move);
            totalScore += simulateGame(simulationBoard);
        }
        return totalScore / SIMULATION_COUNT;
    }

    private double simulateGame(Board simulationBoard) {
        while (!simulationBoard.isGameOver()) {
            List<Move> possibleMoves = simulationBoard.getPossibleMoves();
            if (possibleMoves.isEmpty()) break;

            Move randomMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
            simulationBoard.applyMove(randomMove);
        }
        return simulationBoard.getScore();
    }

    private Move minimaxNextMove() {
        long startTime = System.currentTimeMillis();
        List<Move> possibleMoves = board.getPossibleMoves();
        if (possibleMoves.isEmpty()) return null;

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : possibleMoves) {
            if (System.currentTimeMillis() - startTime >= TIME_LIMIT_MS) {
                break; // If time limit is exceeded, return the best move found so far
            }
            // Create a copy of the board state and apply the move
            Board simulatedBoard = new Board(board);
            if (!simulatedBoard.applyMove(move)) continue;

            int val = minimax(simulatedBoard, SEARCH_DEPTH, move, startTime);

            // Penalize moves that are close to the board edge
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
        return bestMove;
    }

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
            if (previousMove != null && isReverse(previousMove, move)) {
                score -= 10;
            }
            best = Math.max(best, score);
        }
        best += boardState.getPossibleMoves().size();
        return best;
    }

    private int evaluateBoard(Board boardState) {
        return boardState.getScore() + 2 * boardState.getPossibleMoves().size();
    }

    private boolean isReverse(Move m1, Move m2) {
        return m1.getDRow() == -m2.getDRow() && m1.getDCol() == -m2.getDCol();
    }
}