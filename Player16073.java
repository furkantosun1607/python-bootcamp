package players;
import game.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;

public class Player16073 extends Player {
    // Depth for the look-ahead search
    private final int MAX_DEPTH = 5;
    // Keep track of move history for pattern detection
    private List<Move> moveHistory;
    // Cache for evaluated positions
    private Map<String, Double> positionCache;

    public Player16073(Board board) {
        super(board);
        this.moveHistory = new ArrayList<>();
        this.positionCache = new HashMap<>();
    }

    @Override
    public Move nextMove() {
        List<Move> possibleMoves = board.getPossibleMoves();
        if (possibleMoves.isEmpty()) return null;

        // Sort moves by initial evaluation to prioritize promising moves first
        PriorityQueue<MoveEvaluation> rankedMoves = rankMoves(possibleMoves);

        // Apply iterative deepening with a time limit
        long startTime = System.currentTimeMillis();
        long timeLimit = 750; // 750ms time limit to stay under 1 second

        Move bestMove = rankedMoves.peek().move;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Start with a shallow search, then gradually increase depth
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            MoveEvaluation currentBest = null;

            // Reset the queue for each depth level
            PriorityQueue<MoveEvaluation> depthRankedMoves = new PriorityQueue<>(rankedMoves);

            while (!depthRankedMoves.isEmpty()) {
                MoveEvaluation eval = depthRankedMoves.poll();
                Move move = eval.move;

                // Check if we're running out of time
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    // If we're out of time, use the best move from the previous completed depth
                    break;
                }

                // Create a copy of the board and apply the move
                Board simulatedBoard = new Board(board);
                if (!simulatedBoard.applyMove(move)) continue;

                // Evaluate the move using look-ahead
                double score = lookAhead(simulatedBoard, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

                if (currentBest == null || score > currentBest.score) {
                    currentBest = new MoveEvaluation(move, score);
                }
            }

            if (currentBest != null && currentBest.score > bestScore) {
                bestScore = currentBest.score;
                bestMove = currentBest.move;
            }

            // If we're close to the time limit, stop increasing depth
            if (System.currentTimeMillis() - startTime > timeLimit * 0.7) {
                break;
            }
        }

        // Record the move for pattern detection in future turns
        moveHistory.add(bestMove);
        if (moveHistory.size() > 10) {
            moveHistory.remove(0);
        }

        return bestMove;
    }

    // Alpha-beta pruning with look-ahead search
    private double lookAhead(Board boardState, int depth, double alpha, double beta) {
        // Return the evaluation if we've reached the maximum depth or game over
        if (depth <= 0 || boardState.isGameOver()) {
            return evaluatePosition(boardState);
        }

        // Check the cache for this position
        String boardHash = getBoardHash(boardState);
        if (positionCache.containsKey(boardHash)) {
            return positionCache.get(boardHash);
        }

        List<Move> possibleMoves = boardState.getPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return evaluatePosition(boardState);
        }

        double bestScore = Double.NEGATIVE_INFINITY;

        // Sort moves by a quick evaluation to optimize alpha-beta pruning
        PriorityQueue<MoveEvaluation> rankedMoves = rankMoves(boardState, possibleMoves);

        while (!rankedMoves.isEmpty()) {
            Move move = rankedMoves.poll().move;

            Board nextBoard = new Board(boardState);
            if (!nextBoard.applyMove(move)) continue;

            double score = lookAhead(nextBoard, depth - 1, alpha, beta);
            bestScore = Math.max(bestScore, score);

            // Alpha-beta pruning logic
            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) {
                break;
            }
        }

        // Cache the result
        positionCache.put(boardHash, bestScore);
        return bestScore;
    }

    // Initial ranking of moves for the current board state
    private PriorityQueue<MoveEvaluation> rankMoves(List<Move> possibleMoves) {
        return rankMoves(board, possibleMoves);
    }

    // Rank moves for any board state
    private PriorityQueue<MoveEvaluation> rankMoves(Board boardState, List<Move> possibleMoves) {
        PriorityQueue<MoveEvaluation> rankedMoves = new PriorityQueue<>();

        for (Move move : possibleMoves) {
            Board simulatedBoard = new Board(boardState);
            if (!simulatedBoard.applyMove(move)) continue;

            // Calculate a quick heuristic score
            double quickScore = quickEvaluate(boardState, simulatedBoard, move);

            rankedMoves.add(new MoveEvaluation(move, quickScore));
        }

        return rankedMoves;
    }

    // Quick evaluation function for move sorting
    private double quickEvaluate(Board originalBoard, Board newBoard, Move move) {
        // Prefer moves that visit more cells
        double score = newBoard.getScore() - originalBoard.getScore();

        // Prefer moves that maintain more mobility options
        score += 0.5 * newBoard.getPossibleMoves().size();

        // Avoid moves that lead to corners or edges where mobility is restricted
        int boardSize = originalBoard.getSize();
        int newRow = newBoard.getPlayerRow();
        int newCol = newBoard.getPlayerCol();

        // Distance from the edge (higher is better)
        int edgeDistance = Math.min(Math.min(newRow, boardSize - 1 - newRow),
                Math.min(newCol, boardSize - 1 - newCol));
        score += 0.3 * edgeDistance;

        // Analyze board connectivity after the move
        score += 0.2 * analyzeConnectivity(newBoard);

        // Penalize moves that might repeat patterns (cycling behavior)
        if (detectCycle(move)) {
            score -= 2.0;
        }

        return score;
    }

    // Comprehensive evaluation of a board position
    private double evaluatePosition(Board boardState) {
        double score = 0;

        // Primary score is the number of visited cells
        score += 2.0 * boardState.getScore();

        // Mobility score (number of possible moves)
        List<Move> possibleMoves = boardState.getPossibleMoves();
        score += 1.5 * possibleMoves.size();

        // If there are no possible moves, this is game over
        if (possibleMoves.isEmpty()) {
            return score;
        }

        // Analyze future mobility
        double futureMobility = 0;
        for (Move move : possibleMoves) {
            Board nextBoard = new Board(boardState);
            if (nextBoard.applyMove(move)) {
                futureMobility += nextBoard.getPossibleMoves().size();
            }
        }

        // Average future mobility, weighted by its importance
        if (!possibleMoves.isEmpty()) {
            score += 0.8 * (futureMobility / possibleMoves.size());
        }

        // Board connectivity analysis (how connected the remaining cells are)
        score += 0.5 * analyzeConnectivity(boardState);

        // Position on the board (prefer center over edges)
        int boardSize = boardState.getSize();
        int playerRow = boardState.getPlayerRow();
        int playerCol = boardState.getPlayerCol();

        // Distance from center (lower is better)
        double centerRow = (boardSize - 1) / 2.0;
        double centerCol = (boardSize - 1) / 2.0;
        double distanceFromCenter = Math.sqrt(Math.pow(playerRow - centerRow, 2) +
                Math.pow(playerCol - centerCol, 2));

        // Normalize by board size and invert so higher values are better
        double normalizedCenterDistance = 1.0 - (distanceFromCenter / (Math.sqrt(2) * boardSize / 2));
        score += 0.3 * normalizedCenterDistance;

        return score;
    }

    // Analyze how well-connected the remaining unvisited cells are
    private double analyzeConnectivity(Board boardState) {
        int connectivity = 0;
        int boardSize = boardState.getSize();

        // Check connectivity of each unvisited cell
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (!boardState.isVisited(r, c)) {
                    int neighbors = countAccessibleNeighbors(boardState, r, c);
                    connectivity += neighbors;
                }
            }
        }

        // Normalize by the maximum possible connectivity
        int maxConnectivity = 8 * boardSize * boardSize; // Each cell can have up to 8 neighbors
        return (double) connectivity / maxConnectivity;
    }

    // Count accessible neighbors of a cell
    private int countAccessibleNeighbors(Board boardState, int row, int col) {
        int count = 0;
        int[][] directions = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1},
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };

        int boardSize = boardState.getSize();

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < boardSize &&
                    newCol >= 0 && newCol < boardSize &&
                    !boardState.isVisited(newRow, newCol)) {
                count++;
            }
        }

        return count;
    }

    // Generate a simple hash of the board state for caching
    private String getBoardHash(Board boardState) {
        StringBuilder hash = new StringBuilder();
        hash.append(boardState.getPlayerRow()).append(",");
        hash.append(boardState.getPlayerCol()).append(":");

        // Add a simplified representation of visited cells
        int boardSize = boardState.getSize();
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (boardState.isVisited(r, c)) {
                    hash.append(r).append(",").append(c).append(";");
                }
            }
        }

        return hash.toString();
    }

    // Detect potential move cycles to avoid getting stuck in loops
    private boolean detectCycle(Move currentMove) {
        if (moveHistory.size() < 4) return false;

        // Check for direct back-and-forth pattern
        int historySize = moveHistory.size();
        Move lastMove = moveHistory.get(historySize - 1);

        // If the new move is the reverse of the last move
        if (isReverse(currentMove, lastMove)) {
            return true;
        }

        // Check for longer patterns (ABAB, ABCABC, etc.)
        if (historySize >= 6) {
            // Check for 2-move cycle
            if (isReverse(moveHistory.get(historySize - 1), moveHistory.get(historySize - 3)) &&
                    isReverse(moveHistory.get(historySize - 2), moveHistory.get(historySize - 4))) {
                return true;
            }

            // Check for 3-move cycle
            if (historySize >= 8 &&
                    isSameMove(moveHistory.get(historySize - 1), moveHistory.get(historySize - 4)) &&
                    isSameMove(moveHistory.get(historySize - 2), moveHistory.get(historySize - 5)) &&
                    isSameMove(moveHistory.get(historySize - 3), moveHistory.get(historySize - 6))) {
                return true;
            }
        }

        return false;
    }

    // Check if two moves are reverse of each other
    private boolean isReverse(Move m1, Move m2) {
        return m1.getDRow() == -m2.getDRow() && m1.getDCol() == -m2.getDCol();
    }

    // Check if two moves are the same
    private boolean isSameMove(Move m1, Move m2) {
        return m1.getDRow() == m2.getDRow() && m1.getDCol() == m2.getDCol();
    }

    // Inner class for move evaluation
    private class MoveEvaluation implements Comparable<MoveEvaluation> {
        Move move;
        double score;

        public MoveEvaluation(Move move, double score) {
            this.move = move;
            this.score = score;
        }

        @Override
        public int compareTo(MoveEvaluation other) {
            // We want the highest scores first (descending order)
            return Double.compare(other.score, this.score);
        }
    }
}