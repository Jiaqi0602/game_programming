package pacman.entries.pacman.mcts;

import pacman.game.Game;
import pacman.controllers.Controller;
import pacman.controllers.examples.Legacy;
import pacman.game.Constants.DM;
import pacman.game.Constants.MOVE;
import pacman.game.Constants.GHOST;
import pacman.game.internal.Node;

import java.util.*;
import static java.lang.Float.NEGATIVE_INFINITY;

public class MonteCarloPacman extends Controller<MOVE> {

    Controller<EnumMap<GHOST, MOVE>> ghosts = new Legacy();
    // exploration coefficient
    float C = (float) (1.0 / Math.sqrt(2));

    // set guidance for each pacman rollout
    private static final int EXPLORATION_TIME = 36;
    private static final int SIMULATION_STEPS = 200;
    private static final int POWER_PILL_MISSED = -100;
    public static final int LIFE_LOST = -500;
    private static final int TREE_DEPTH = 40;
    private static final int GHOST_DISTANCE = 200;

    public static Set<Integer> junctions;
    int lvlLast = 1;

    @Override
    public MOVE getMove(Game game, long timeDue) {

        int currentLvl = game.getCurrentLevel();
        if (junctions == null || lvlLast != currentLvl) {
            junctions = getNextJunctions(game);
        }
        lvlLast = currentLvl;

        //MCTS Search
        long startTime = new Date().getTime();
        MonteCarloNode oriSearch = new MonteCarloNode(new MonteCarloState(true, game), null, game.getPacmanLastMoveMade(), 0);

        while (new Date().getTime() < startTime + EXPLORATION_TIME) {

            MonteCarloNode newSearch = tPolicy(oriSearch);
            if (newSearch == null)
                return MOVE.DOWN;

            int score = defaultGamePolicy(newSearch, oriSearch);
            gameSaveState(newSearch, score);
        }

        MonteCarloNode bestResultNode = getBestResultChild(oriSearch, 0);
        MOVE move = MOVE.UP;
        if (bestResultNode != null) {
            move = bestResultNode.getMove();
        }
        return move;
    }

    private MonteCarloNode tPolicy(MonteCarloNode node) {

        if (node.isExpandable()) {
            if (node.getTime() <= TREE_DEPTH)
                return node.expand();
            else
                return node;
        }
        if (node.getState().isAlive())
            return tPolicy(getBestResultChild(node, C));
        else
            return node;
    }

    private MonteCarloNode getBestResultChild(MonteCarloNode v, float c) {

        float bestValue = NEGATIVE_INFINITY;
        MonteCarloNode currentExpand = null;

        for (MonteCarloNode node : v.children) {
            float value = backpropagation(node, c);
            if (!node.getState().isAlive())
                value = -99999;

            if (value > bestValue) {
                if (c != 0 || checkIfDeath(v, node)) {
                    currentExpand = node;
                    bestValue = value;
                }
            }
        }
        return currentExpand;
    }

    // termination state
    private boolean checkIfDeath(MonteCarloNode v, MonteCarloNode node) {

        Controller<EnumMap<GHOST, MOVE>> ghostController = ghosts;
        Game game = v.getState().getGame().copy();
        int livesBefore = game.getPacmanNumberOfLivesRemaining();
        game.advanceGame(node.getMove(),
                ghostController.getMove(game.copy(), System.currentTimeMillis()));

        int livesAfter = game.getPacmanNumberOfLivesRemaining();
        if (livesAfter < livesBefore) {
            return false;
        }
        return true;
    }

    private float backpropagation(MonteCarloNode node, float c) {

        float rewardValue = node.getValue() / node.getVisited();
        //normalize
        float max = 2000;
        float min = -500;
        float range = max - min;
        float inZeroRange = (rewardValue - min);
        rewardValue = inZeroRange / range;

        float n = 0;
        if (node.getParent() != null) {
            n = node.getParent().getVisited();
        }
        float nj = node.getVisited();

        float uct = (float) (rewardValue + 2 * c * Math.sqrt((2 * Math.log(n)) / nj));
        return uct;
    }

    private int defaultGamePolicy(MonteCarloNode node, MonteCarloNode root) {
        // Terminal
        if (!node.getState().isAlive() ||
                node.getState().getGame().getPacmanNumberOfLivesRemaining() < root.getState().getGame().getPacmanNumberOfLivesRemaining())
            return LIFE_LOST;

        int result = rolloutSimulateExperiment(node, SIMULATION_STEPS);
        return (result - root.getState().getGame().getScore());
    }

    public int rolloutSimulateExperiment(MonteCarloNode node, int steps) { //run advance game to get avg score

        Controller<MOVE> pacManController = new ExplorationPacman();
        Controller<EnumMap<GHOST, MOVE>> ghostController = ghosts;

        Game game = node.getState().getGame().copy();

        int livesBefore = game.getPacmanNumberOfLivesRemaining();
        int powerPillsBefore = game.getNumberOfActivePowerPills();
        int s = 0;
        int boostUpScore = 0;
        while (!game.gameOver()) {
            if (s >= steps && game.getNeighbouringNodes(game.getPacmanCurrentNodeIndex()).length > 2)
                break;

            game.advanceGame(pacManController.getMove(game.copy(), System.currentTimeMillis()),
                    ghostController.getMove(game.copy(), System.currentTimeMillis()));
            s++;
            int powerPillsAfter = game.getNumberOfActivePowerPills();
            if (powerPillsAfter < powerPillsBefore && distanceToGhostAverage(game) > GHOST_DISTANCE) {
                boostUpScore += POWER_PILL_MISSED;
            }
            int livesAfter = game.getPacmanNumberOfLivesRemaining();
            if (livesAfter < livesBefore) {
                break;
            }
        }

        int score = game.getScore();

        int livesAfter = game.getPacmanNumberOfLivesRemaining();
        if (livesAfter > livesBefore) {  //get new life
            score += 0;
        } else if (livesAfter < livesBefore) {  //lose
            score += LIFE_LOST;
        }
        return score + boostUpScore;
    }

    private void gameSaveState(MonteCarloNode v, int score) {
        v.getSimulations().add(score);
        v.setValue(v.getValue() + score);
        v.setVisited(v.getVisited() + 1);
        if (v.getParent() != null)
            gameSaveState(v.getParent(), score);
    }

    public static Set<Integer> getNextJunctions(Game game) {
        Set<Integer> junctions = new HashSet<>();

        int[] junctionArr = game.getJunctionIndices();
        for (Integer i : junctionArr)
            junctions.add(i);
        junctions.addAll(getTurningDirection(game));

        return junctions;
    }

    private int distanceToGhostAverage(Game game) {
        int total = 0;
        for (GHOST ghost : GHOST.values()) {
            total += game.getDistance(game.getPacmanCurrentNodeIndex(),
                    game.getGhostCurrentNodeIndex(ghost), DM.MANHATTAN);
        }
        return total / GHOST.values().length;
    }

    private static Collection<? extends Integer> getTurningDirection(Game game) {

        List<Integer> turns = new ArrayList<>();
        for (Node n : game.getCurrentMaze().graph) {

            int down = game.getNeighbour(n.nodeIndex, MOVE.DOWN);
            int up = game.getNeighbour(n.nodeIndex, MOVE.UP);
            int left = game.getNeighbour(n.nodeIndex, MOVE.LEFT);
            int right = game.getNeighbour(n.nodeIndex, MOVE.RIGHT);

            if (((down != -1) != (up != -1)) || ((left != -1) != (right != -1))) {
                turns.add(n.nodeIndex);
            } else if (down != -1 && up != -1 && left != -1 && right != -1) {
                turns.add(n.nodeIndex);
            }
        }
        return turns;
    }

}
