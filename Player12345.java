package players;

import game.*;
import java.util.List;
import java.util.Random;

public class Player12345 extends Player {
    private static final int SIMULATION_COUNT = 5000; // Increased number of simulations
    private final Random random;

    public Player12345(Board board) {
        super(board);
        this.random = new Random();
    }

    @Override
    public Move nextMove() {
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
}