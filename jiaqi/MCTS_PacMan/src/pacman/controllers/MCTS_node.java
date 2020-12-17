package pacman.controllers;

import java.util.*;

import com.sun.security.jgss.GSSUtil;
import jdk.swing.interop.SwingInterOpUtils;
import pacman.controllers.MCTS;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class MCTS_node {

    public MCTS_node parent;
    public ArrayList<MCTS_node> children = new ArrayList<MCTS_node>();
    public MOVE move;
    public float nodeRewards;
    public ArrayList<MOVE> prevMoves = new ArrayList<MOVE>();
    public int junction = -1;
    public int timesVisited = 0;
    public Game game;

    public MCTS_node(MCTS_node parent, Game game, int junction) {
        this.parent = parent;
        this.move = MOVE.UP;
        this.nodeRewards = -1.0f;
        this.game = game;
        this.junction = junction;
        this.prevMoves.clear();
        this.children.clear();
    }
    /*
    expand node of the tree with conditions
    rewards given to the child node if simulation of the nodes match conditions
    pacman will always look for the nearest junctions to move
     */

    public MCTS_node expand() {
        MOVE childMove = nextMove(game);
        if((childMove != game.getPacmanLastMoveMade().opposite())) {
            MCTS_node expandedChild = getChildNode(childMove);
            expandedChild.move = childMove;
//            System.out.println("parent move: "+ expandedChild.parent.move);
//            System.out.println("expanded child: " + expandedChild + "parent :" + expandedChild.parent + " Junction :" + expandedChild.junction + " Move: " + expandedChild.move);
            MCTS.tree_length ++;
            this.children.add(expandedChild);
            expandedChild.parent = this;
            return expandedChild;
        }
        return this;
    }


    public MCTS_node getChildNode(MOVE move) {
        Game trial = game.copy();
        Controller<EnumMap<GHOST, MOVE>> ghostController = MCTS.ghosts;

        int from = trial.getPacmanCurrentNodeIndex();
        int current = from;
        MOVE currentMove = move;

        int pillsBefore = trial.getNumberOfActivePills();
        int powerPillsBefore = trial.getNumberOfActivePowerPills();
        int livesBefore = trial.getPacmanNumberOfLivesRemaining();
        float rewards = 0.0f;

//       current position is not a function, so only have 2 possible directions, forward or backward
//       but we dont want the pacman to move in reverse direction
//       get simulation rewards AFTER the turns

        while(!trial.isJunction(current) || current == from){

            currentMove = bestMove(trial, currentMove);

            trial.advanceGame(currentMove,
                    ghostController.getMove(trial,
                            System.currentTimeMillis()));

            current = trial.getPacmanCurrentNodeIndex();
        }

        rewards = rewardRules(trial, livesBefore, powerPillsBefore, pillsBefore);
//        System.out.println("rewards: " + rewards);

        //return the child node with updated state and junction number
        MCTS_node child = new MCTS_node(this,trial,current);
        child.nodeRewards = rewards;
        return child;
    }

//    rule set for simulation
    public float rewardRules(Game game, int livesBefore, int ppsBefore, int pillsBefore){
        int livesAfter = game.getPacmanNumberOfLivesRemaining();
        int ppAfter = game.getNumberOfActivePowerPills();
        int pillAfter = game.getNumberOfActivePills();
        float rewards = 0;

        if(livesBefore>livesAfter)
            rewards = 0.0f;
        else if(pillAfter == pillsBefore)
            rewards = 0.2f;
        else if(pillAfter<pillsBefore)
            rewards = 1.5f;
        else if(ppsBefore>ppAfter && AvgDistanceFromGhosts(game)>100)
            rewards = 0.0f;
        else if(ppsBefore>ppAfter && AvgDistanceFromGhosts(game)<20)
            rewards = 0.5f;
        return rewards;
    }
//
    public MOVE bestMove(Game game,MOVE direction){
        MOVE [] availableMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
        ArrayList<MOVE> avaiMoves = new ArrayList<MOVE>();
        Collections.addAll(avaiMoves, availableMoves);
        avaiMoves.remove(game.getPacmanLastMoveMade().opposite());
        if(avaiMoves.size()==0)
            System.out.println("Error: available move for one direction not found");
//        if(avaiMoves.size()>1)
//            System.out.println("Error: available move more than 1");
        if(avaiMoves.contains(direction))
            return direction;

        return avaiMoves.get(0);
    }

    public boolean reachTerminalState() {
        if (game.wasPacManEaten() || game.getActivePillsIndices().length == 0) {
            return true;
        }
        return false;
    }


//    get next move with conditions (especially at junction)
    public MOVE nextMove(Game game) {
        ArrayList<MOVE> childrenMoves = new ArrayList<MOVE>();
        MOVE childMove = null;
        int pacmanNode = game.getPacmanCurrentNodeIndex();
        List<MOVE> possibleMoves = new ArrayList<MOVE>();
        Collections.addAll(possibleMoves, game.getPossibleMoves(pacmanNode));

//        System.out.println("pacman last move made: " + game.getPacmanLastMoveMade() + " possible moves: " + possibleMoves);

        if (possibleMoves.contains(MOVE.UP) && !prevMoves.contains(MOVE.UP)) {
            childrenMoves.add(MOVE.UP);
        }
        else if (possibleMoves.contains(MOVE.RIGHT) && !prevMoves.contains(MOVE.RIGHT)) {
            childrenMoves.add(MOVE.RIGHT);
        }
        else if (possibleMoves.contains(MOVE.DOWN) && !prevMoves.contains(MOVE.DOWN)) {
            childrenMoves.add(MOVE.DOWN);
        }
        else if (possibleMoves.contains(MOVE.LEFT) && !prevMoves.contains(MOVE.LEFT)) {
            childrenMoves.add(MOVE.LEFT);
        }

        if(childrenMoves.size()>1 && childrenMoves.contains(game.getPacmanLastMoveMade().opposite())){
            childrenMoves.remove(game.getPacmanLastMoveMade().opposite());
        }

        childMove = childrenMoves.get(new Random().nextInt(childrenMoves.size()));
        prevMoves.add(childMove);
        return childMove;
    }

    public boolean noChildNode() {
        if (children.size() <= 0) {
            return false;
        }

        int current_node = game.getPacmanCurrentNodeIndex();
        MOVE[] possibleMoves = game.getPossibleMoves(current_node);

        if(possibleMoves.length == prevMoves.size()) return true;

        if (possibleMoves.length != children.size()) {
            return false;
        }
        else {
            return true;
        }
    }

    public static int AvgDistanceFromGhosts(Game state) {
        int sum = 0;
        for(GHOST ghost : GHOST.values())
            sum += state.getDistance(state.getPacmanCurrentNodeIndex(), state.getGhostCurrentNodeIndex(ghost), DM.PATH);
        return sum/4;
    }

}
			