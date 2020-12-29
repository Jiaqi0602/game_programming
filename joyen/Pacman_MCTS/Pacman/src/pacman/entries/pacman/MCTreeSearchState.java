
package pacman.entries.pacman;

import pacman.game.Game;

/**
 *
 * @author Joyen
 */
public class MCTreeSearchState {
    boolean alive;
	Game game;
	
	public MCTreeSearchState(boolean alive, Game game) {
		super();
		this.alive = alive;
		this.game = game;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}
	
}