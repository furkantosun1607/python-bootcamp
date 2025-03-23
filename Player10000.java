package players;
import game.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Player10000 extends Player {
    // Constants for search parameters and evaluation weights
    private static final int MAX_DEPTH = 3; // Good balance of depth vs performance
    private static final double MOBILITY_WEIGHT = 4.0; // High priority on having future move options
    private static final double COVERAGE_WEIGHT = 2.5; // Board coverage is important
    private static final double SPREAD_WEIGHT = 2.0; // Encourage exploring all quadrants
    private static final double CENTER_WEIGHT = 1.0; // Modest weight for central position
    private static final double EDGE_PENALTY = 0.8; // Penalty for staying near edges

    // Board dimensions
    private final int boardSize;
    private final int centerRow;
    private final int centerCol;

    public Player10000(Board board) {
        super(board);
        this.boardSize = board.getSize();
        this.centerRow = boardSize / 2;
        this.centerCol = boardSize / 2;
    }

    @Override
    public Move nextMove() {
        List<Move> possibleMoves = board.getPossibleMoves();

        // No valid moves remain
        if (possibleMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Start timing to avoid timeout issues
        long startTime = System.currentTimeMillis();
        long timeLimit = 900; // ms (safety margin below 1000ms timeout)

        // Evaluate each possible move
        for (Move move : possibleMoves) {
            // Check time to avoid timeout
            if (System.currentTimeMillis() - startTime > timeLimit) {
                break;
            }

            // Simulate this move
            Board simulatedBoard = new Board(board);
            if (!simulatedBoard.applyMove(move)) continue;

            // Calculate immediate score for this move
            double immediateScore = evaluatePosition(simulatedBoard);

            // Look ahead to find the best sequence of moves
            double futureScore = lookAhead(simulatedBoard, 1, MAX_DEPTH);

            // Combine immediate and future scores
            double totalScore = immediateScore + futureScore;

            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestMove = move;
            }
        }

        // If we couldn't evaluate all moves due to time constraints but found at least one good move, use it
        // Otherwise, default to the first valid move
        return bestMove != null ? bestMove : possibleMoves.get(0);
    }

    // Evaluate how good a board position is
    private double evaluatePosition(Board board) {
        int playerRow = board.getPlayerRow();
        int playerCol = board.getPlayerCol();
        int availableMoves = board.getPossibleMoves().size();
        double coverage = board.getCoveragePercentage();

        // Calculate distance from center (Manhattan distance)
        int distanceFromCenter = Math.abs(playerRow - centerRow) + Math.abs(playerCol - centerCol);

        // Calculate quadrant distribution to encourage spreading
        int[] quadrantCounts = calculateQuadrantDistribution(board);
        double spreadScore = calculateSpreadScore(quadrantCounts);

        // Calculate edge proximity (0 = center, 1 = edge)
        double edgeProximity = calculateEdgeProximity(playerRow, playerCol);

        // Calculate final score with weights
        double score = 0.0;
        score += availableMoves * MOBILITY_WEIGHT; // Future mobility is critical
        score += coverage * COVERAGE_WEIGHT; // Higher coverage is better
        score += spreadScore * SPREAD_WEIGHT; // Better spread across quadrants
        score -= (distanceFromCenter * CENTER_WEIGHT) / boardSize; // Prefer staying somewhat central
        score -= edgeProximity * EDGE_PENALTY; // Penalty for being close to edges

        return score;
    }

    // Look ahead multiple moves and return the best possible score
    private double lookAhead(Board board, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth || board.isGameOver()) {
            return 0.0;
        }

        List<Move> possibleMoves = board.getPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return 0.0;
        }

        double bestScore = Double.NEGATIVE_INFINITY;

        // Reduce branching factor at deeper levels for better performance
        int movesToEvaluate = Math.min(possibleMoves.size(), maxDepth == 3 ? 4 : 3);
        List<Move> selectedMoves = selectBestMovesForLookahead(board, possibleMoves, movesToEvaluate);

        for (Move move : selectedMoves) {
            Board simulatedBoard = new Board(board);
            if (!simulatedBoard.applyMove(move)) continue;

            // Evaluate this position and continue looking ahead
            double immediateScore = evaluatePosition(simulatedBoard);
            double futureScore = lookAhead(simulatedBoard, currentDepth + 1, maxDepth);

            // Apply discount factor to future scores (prioritize immediate gains)
            double totalScore = immediateScore + (futureScore / (currentDepth + 1.5));

            bestScore = Math.max(bestScore, totalScore);
        }

        return bestScore;
    }

    // Select a subset of promising moves for deeper lookahead
    private List<Move> selectBestMovesForLookahead(Board board, List<Move> moves, int count) {
        // If we have few moves, just return them all
        if (moves.size() <= count) {
            return new ArrayList<>(moves);
        }

        // Create a list of moves with their preliminary scores
        List<ScoredMove> scoredMoves = new ArrayList<>();

        for (Move move : moves) {
            Board simulatedBoard = new Board(board);
            if (!simulatedBoard.applyMove(move)) continue;

            // Quick evaluation - mobility focused
            double score = quickEvaluate(simulatedBoard);
            scoredMoves.add(new ScoredMove(move, score));
        }

        // Sort by score (descending) and select the top moves
        scoredMoves.sort(Comparator.comparingDouble(ScoredMove::getScore).reversed());

        List<Move> selectedMoves = new ArrayList<>();
        for (int i = 0; i < count && i < scoredMoves.size(); i++) {
            selectedMoves.add(scoredMoves.get(i).getMove());
        }

        return selectedMoves;
    }

    // Simplified evaluation for move selection
    private double quickEvaluate(Board board) {
        int availableMoves = board.getPossibleMoves().size();
        int playerRow = board.getPlayerRow();
        int playerCol = board.getPlayerCol();

        // Calculate distance from center (0-1 scale, 0 = edge, 1 = center)
        double centerProximity = 1.0 - (double)(Math.abs(playerRow - centerRow) +
                Math.abs(playerCol - centerCol)) / boardSize;

        // Emphasize mobility with some consideration for position
        return availableMoves * 2.0 + centerProximity * boardSize * 0.5;
    }

    // Calculate how pieces are distributed across the four quadrants of the board
    private int[] calculateQuadrantDistribution(Board board) {
        int[] counts = new int[4];

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (board.isVisited(r, c)) {
                    // Determine quadrant (0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right)
                    int quadrant = (r < centerRow ? 0 : 2) + (c < centerCol ? 0 : 1);
                    counts[quadrant]++;
                }
            }
        }

        return counts;
    }

    // Calculate a score based on how evenly the board is covered
    private double calculateSpreadScore(int[] quadrantCounts) {
        double total = quadrantCounts[0] + quadrantCounts[1] + quadrantCounts[2] + quadrantCounts[3];
        if (total == 0) return 0;

        double balance = 0;
        double expected = total / 4.0;

        for (int count : quadrantCounts) {
            double diff = Math.abs(count - expected);
            balance += diff;
        }

        // Return a normalized score (higher is better distribution)
        return 1.0 - (balance / total);
    }

    // Calculate how close the player is to a board edge
    private double calculateEdgeProximity(int row, int col) {
        // Distance from nearest horizontal edge (0 = at edge, boardSize/2 = center)
        int distFromHorizontalEdge = Math.min(row, boardSize - 1 - row);

        // Distance from nearest vertical edge (0 = at edge, boardSize/2 = center)
        int distFromVerticalEdge = Math.min(col, boardSize - 1 - col);

        // Minimum distance to any edge
        int minDist = Math.min(distFromHorizontalEdge, distFromVerticalEdge);

        // Normalize to 0-1 range (1 = at edge, 0 = at center)
        return 1.0 - (2.0 * minDist / boardSize);
    }

    // Helper class to associate a move with its score
    private static class ScoredMove {
        private final Move move;
        private final double score;

        public ScoredMove(Move move, double score) {
            this.move = move;
            this.score = score;
        }

        public Move getMove() {
            return move;
        }

        public double getScore() {
            return score;
        }
    }
}
