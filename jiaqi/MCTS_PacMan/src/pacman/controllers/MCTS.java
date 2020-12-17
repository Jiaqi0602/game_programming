package pacman.controllers;

import java.util.*;

import pacman.controllers.MCTS_node.*;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
implementation of PACMAN from
https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6731713
http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.728.1232&rep=rep1&type=pdf
 */

public class MCTS extends Controller<MOVE> {

//    constant - hoeffding inequality
    public static final double constant = (1.0f/Math.sqrt(2.0f));

    //PROPERTIES
    public static Controller<EnumMap<GHOST,MOVE>> ghosts = new StarterGhosts();
    public static final int safeRange = 15;
    public static final int huntingRange = 30;
    public static final int TREE_LIMIT = 35;
    public static int tree_length = 0 ;

    @Override
    public MOVE getMove(Game game, long timeDue) {

//      use mcts to search the next move when a junction meet
        if(atJunction(game)) {
            tree_length = 0;
            return MCTS_search(game);
        }
//
////  hunt the nearest ghost if powerpill eaten, maximise scores
        for(GHOST ghost : GHOST.values()) {
            if(game.getGhostEdibleTime(ghost) > 0) {
                if(game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost)) < huntingRange) {
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost), DM.PATH);
                }
            }
        }

        return usualMovement(game.getPacmanLastMoveMade() , game);
        }


// if no junction meet & ghosts are not in fear mpde
    public MOVE usualMovement(MOVE move,Game game) {
        ArrayList<MOVE> moves = new ArrayList<MOVE>();
        Collections.addAll(moves, game.getPossibleMoves(game.getPacmanCurrentNodeIndex()));

        int current = game.getPacmanCurrentNodeIndex();
        for(GHOST ghost : GHOST.values()) {
            if(game.getGhostEdibleTime(ghost)==0 && game.getGhostLairTime(ghost)==0) {
//                manhattan distance to increase survival rate
                if(game.getManhattanDistance(current,game.getGhostCurrentNodeIndex(ghost)) < safeRange) {
                    return game.getNextMoveAwayFromTarget(current,game.getGhostCurrentNodeIndex(ghost), DM.PATH);
                }
        }
    }

    if(moves.contains(move))
        return move;

    moves.remove(game.getPacmanLastMoveMade().opposite());
    return moves.get(0);
}

    private boolean atJunction(Game game) {
        if(game.isJunction(game.getPacmanCurrentNodeIndex()))
            return true;
        return false;
    }

    public MOVE MCTS_search(Game game) {
        MCTS_node root = new MCTS_node(null,game,game.getPacmanCurrentNodeIndex());
        long start = new Date().getTime();

        while (new Date().getTime() < start + 30 && tree_length <= TREE_LIMIT) {
            MCTS_node nd = treePolicy(root);
            if(nd == null) return MOVE.DOWN;
            double reward = defaultPolicy(nd);
            Backpropagation(nd,reward);
        }

        MCTS_node bestChild = bestChild(root,0);

        if(bestChild == null) {
            return new RandomPacMan().getMove(game,-1);
        }

        return bestChild.move;
    }

// expand the tree if expandable until terminal state reach
    public MCTS_node treePolicy(MCTS_node node) {
        if (node==null){
//            System.out.println("node is null ");
            return node;
        }
        while(!node.reachTerminalState()){
            if(!node.noChildNode())
                return node.expand();
            else
                return treePolicy(bestChild(node, constant));
        }
        return node;
    }


    public double defaultPolicy(MCTS_node node) {

        double reward = 0;
        if(node == null)
            return 0;
        Game game = node.game.copy();

        if(node.nodeRewards == 0.0f) {
            return 0;
        }

        reward = simulationExperiment(game);
        return  reward;
    }

    public double simulationExperiment(Game game){

        int steps = 0;
        double rewards = 0 ;
        int liveBefore = game.getPacmanNumberOfLivesRemaining();
        int pillBefore = game.getNumberOfActivePills();
        int powerPillBefore = game.getNumberOfActivePowerPills();

        while(!game.gameOver()){
            Controller <MOVE> pacManController = new RandomPacMan();
            Controller <EnumMap<GHOST,MOVE>> ghostController = ghosts;

            game.advanceGame(pacManController.getMove(game, System.currentTimeMillis()), ghostController.getMove(game, System.currentTimeMillis()));

            steps++ ;
            if(steps>=15)
                break ;
        }

        int livesAfter = game.getPacmanNumberOfLivesRemaining();
        int pillAfter = game.getNumberOfActivePills();
        int powerPillAfter = game.getNumberOfActivePowerPills();

        if(livesAfter<livesAfter)
            return 0.0f;
        if(game.getNumberOfActivePills()==0)
            return 1.0f;
        if(powerPillAfter<powerPillBefore && MCTS_node.AvgDistanceFromGhosts(game)>100)
            return 0.0f;

//        number of pills left avg by initial number of pills
        rewards = 1.0f - game.getNumberOfActivePills() / pillBefore;

        return rewards;
    }

    public MCTS_node bestChild(MCTS_node node, double constant) {
        MCTS_node bestChild = null;
        double bestValue = -1.0f;
        for(MCTS_node child: node.children){
            double uctValue = UCTvalue(child, constant);
            if(uctValue>bestValue){
                bestValue = uctValue;
                bestChild = child;
            }
        }
        return bestChild;
    }
//   Discounted upper confidence bound applied to trees (UCT)
//   to explore tree for rewarding decisions
//   eqm - vi = reward obtained of current node, ni - timesVisited of current node, np - timesVisited of parent node
//   vi = score obtained avg by timesVisited
//   uct = vi + (contant * (log(np) / ni)^(1/2))

    private double UCTvalue(MCTS_node node, double C) {
        double vi = node.nodeRewards / node.timesVisited;
        double np = node.parent.timesVisited;
        double ni = node.timesVisited;

        double uctValue = (vi + (C* Math.sqrt(Math.log(np) / ni)));

//        double uctValue = ((node.nodeRewards / node.timesVisited) + C* Math.sqrt(2*Math.log(node.parent.timesVisited)/ node.timesVisited));
        return uctValue;
    }

    private void Backpropagation(MCTS_node currentNode, double reward) {
        while(currentNode != null) {
            currentNode.timesVisited ++;
            currentNode.nodeRewards += reward;
            currentNode = currentNode.parent;
        }
    }

}