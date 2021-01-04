package pacman.entries.pacman.mcts;

import pacman.game.Game;
import pacman.controllers.Controller;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class MonteCarloNode {

    private MonteCarloState state;
    private MonteCarloNode parent;
    private int directions;
    private float value;
    private MOVE move;
    private int visited;
    private int time;
    private List<Integer> simulations;
    protected List<MonteCarloNode> children;

    private boolean up = false, down = false, right = false, left = false;

    public MonteCarloNode(MonteCarloState state, MonteCarloNode parent, MOVE move, int time) {
        super();
        this.state = state;
        this.parent = parent;
        this.move = move;
        this.time = time;
        this.visited = 0;
        this.value = 0;
        this.directions = getDirections();
        this.simulations = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    private int getDirections() {
        Game gameAccess = state.getGame();
        if (!state.isAlive())
            return 0;
        int currentPosition = gameAccess.getPacmanCurrentNodeIndex();
        int[] neighbors = gameAccess.getNeighbouringNodes(currentPosition);
        int ctn = 0;
        for (Integer i : neighbors) {
            MOVE movePosition = gameAccess.getMoveToMakeToReachDirectNeighbour(currentPosition, i);
            if (parent == null || movePosition != move.opposite()) {
                ctn++;
            }
        }
        return ctn;
    }

    public MonteCarloNode expand() {
        int pacmanPosition = state.getGame().getPacmanCurrentNodeIndex();
        int junctionPosition = -1;
        MOVE nextMove = null;
        if (!state.isAlive()) {
            return this;
        }
        // Closest junctions
        if (!up && state.getGame().getNeighbour(pacmanPosition, MOVE.UP) != -1
                && (parent == null || move.opposite() != MOVE.UP)) {
            junctionPosition = getClosestJunction(MOVE.UP);
            nextMove = MOVE.UP;
        } else if (!right && state.getGame().getNeighbour(pacmanPosition, MOVE.RIGHT) != -1
                && (parent == null || move.opposite() != MOVE.RIGHT)) {
            junctionPosition = getClosestJunction(MOVE.RIGHT);
            nextMove = MOVE.RIGHT;
        } else if (!down && state.getGame().getNeighbour(pacmanPosition, MOVE.DOWN) != -1
                && (parent == null || move.opposite() != MOVE.DOWN)) {
            junctionPosition = getClosestJunction(MOVE.DOWN);
            nextMove = MOVE.DOWN;
        } else if (!left && state.getGame().getNeighbour(pacmanPosition, MOVE.LEFT) != -1
                && (parent == null || move.opposite() != MOVE.LEFT)) {
            junctionPosition = getClosestJunction(MOVE.LEFT);
            nextMove = MOVE.LEFT;
        }
        if (junctionPosition == -1) {
            return this;
        }else if (junctionPosition != -1) {
            updateDirection(nextMove);

            MonteCarloState childState = tryUntilJunction(new AggressiveGhosts(), state.getGame(), junctionPosition, nextMove);
            if (childState == null || childState.getGame() == null) {
                return this;
            }

            int to = childState.getGame().getPacmanCurrentNodeIndex();
            int distance = (int) state.getGame().getDistance(pacmanPosition, to, DM.MANHATTAN);

            MonteCarloNode child = new MonteCarloNode(childState, this, nextMove, time + distance);
            children.add(child);
            return child;
        }
        return this;
    }

    private MonteCarloState tryUntilJunction(Controller<EnumMap<GHOST, MOVE>> ghostController, Game game, int junction, MOVE move) {

        Game gameCopy = game.copy();
        int livesBefore = gameCopy.getPacmanNumberOfLivesRemaining();
        int now = gameCopy.getPacmanCurrentNodeIndex();
        while (now != junction) {

            int last = now;
            gameCopy.advanceGame(move,
                    ghostController.getMove(gameCopy.copy(),
                            System.currentTimeMillis()));

            now = gameCopy.getPacmanCurrentNodeIndex();
            int livesNow = gameCopy.getPacmanNumberOfLivesRemaining();
            if (livesNow < livesBefore) {
                return new MonteCarloState(false, gameCopy);
            }
            if (now == last) {
                break;
            }
        }
        return new MonteCarloState(true, gameCopy);
    }

    private void updateDirection(MOVE move) {
        switch (move) {
            case UP:
                up = true;
                break;
            case DOWN:
                down = true;
                break;
            case RIGHT:
                right = true;
                break;
            case LEFT:
                left = true;
                break;
        }
    }

    private int getClosestJunction(MOVE move) {

        int fromPosition = state.getGame().getPacmanCurrentNodeIndex();
        int currentPosition = fromPosition;
        if (currentPosition == -1)
            return -1;

        while (!MonteCarloPacman.junctions.contains(currentPosition) || currentPosition == fromPosition) {
            int nextPosition = state.getGame().getNeighbour(currentPosition, move);
            if (nextPosition == fromPosition) {
                return -1;
            }
            currentPosition = nextPosition;
            if (currentPosition == -1) {
                return -1;
            }
        }
        return currentPosition;
    }

    public MOVE getMove() {
        return move;
    }

    public void setMove(MOVE move) {
        this.move = move;
    }

    public boolean isExpandable() {
        return directions != children.size() && state.isAlive();
    }

    public MonteCarloNode getParent() {
        return parent;
    }

    public MonteCarloState getState() {
        return state;
    }

    public void setState(MonteCarloState state) {
        this.state = state;
    }

    public int getVisited() {
        return visited;
    }

    public void setVisited(int visited) {
        this.visited = visited;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public List<Integer> getSimulations() {
        return simulations;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

}

class MonteCarloState {

    boolean isAlive;
    Game game;

    public MonteCarloState(boolean alive, Game game) {
        super();
        this.isAlive = alive;
        this.game = game;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

}
