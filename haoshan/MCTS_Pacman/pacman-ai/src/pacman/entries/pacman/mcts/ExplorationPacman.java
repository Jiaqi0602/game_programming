package pacman.entries.pacman.mcts;

import pacman.controllers.Controller;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import static pacman.game.Constants.*;

import java.util.Random;

public class ExplorationPacman extends Controller<MOVE> {

	public static Random r = new Random();

	public MOVE getMove(Game game, long timeDue) {
		int exploiteRate = r.nextInt(100)+1;
		int exploreRate = r.nextInt(100)+1;

		if (MonteCarloPacman.junctions == null) {
			MonteCarloPacman.junctions = MonteCarloPacman.getNextJunctions(game);
		}
		MOVE lastMove = game.getPacmanLastMoveMade();
		int current = game.getPacmanCurrentNodeIndex();

		int minDist = Integer.MAX_VALUE;
		GHOST minGhost=null;

		for(Constants.GHOST ghost : Constants.GHOST.values())
			if(game.getGhostEdibleTime(ghost)>0)
			{
				int distance = game.getShortestPathDistance(current,game.getGhostCurrentNodeIndex(ghost));

				if(distance < minDist)
				{
					minDist = distance;
					minGhost = ghost;
				}
			}

		if(minGhost != null && game.getShortestPathDistance(current,game.getGhostCurrentNodeIndex(minGhost)) < 30) {    //we found an edible ghost
			System.out.println("found an edible ghost and distance < 30");
			return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(minGhost), Constants.DM.PATH);

		}else if(MonteCarloPacman.junctions.contains(game.getPacmanCurrentNodeIndex())) { // check if at junction
			if(exploreRate > 10){
				System.out.println("getRandDirection");
				return getRandDirection(lastMove);
			}else{
				return lastMove;
			}
		}else if(exploiteRate > 95){
			System.out.println("exploitRate > 95");
			int currentNodeIndex=game.getPacmanCurrentNodeIndex();

			//access all active pills
			int[] activePills = game.getActivePillsIndices();
			//access all active power pills
			int[] activePowerPills = game.getActivePowerPillsIndices();
			//create a target array for all ACTIVE and power pills
			int[] targetNodePositions = new int[activePills.length+activePowerPills.length];

			for(int i=0; i<activePills.length; i++)
				targetNodePositions[i] = activePills[i];

			for(int i=0; i<activePowerPills.length; i++)
				targetNodePositions[activePills.length+i] = activePowerPills[i];

			//return the move of next direction once the closest target has been identified
			return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),game.getClosestNodeIndexFromNodeIndex(currentNodeIndex,targetNodePositions, Constants.DM.PATH), Constants.DM.PATH);
		}else {
			System.out.println("keep last move");
			return lastMove;
		}
	}

	private MOVE getRandDirection(MOVE except) {
		MOVE move = null;
		while(move == null){
			int rand = r.nextInt(4);
			switch(rand){
				case 0:
					move = MOVE.UP;
					break;
				case 1:
					move = MOVE.DOWN;
					break;
				case 2:
					move = MOVE.LEFT;
					break;
				case 3:
					move = MOVE.RIGHT;
					break;
			}
		}
		return move;
	}

}