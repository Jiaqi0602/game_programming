"""This file contains all the classes you must complete for this project.

You can use the test cases in agent_test.py to help during development, and
augment the test suite with your own test cases to further test your code.

You must test your agent's strength against a set of agents with known
relative strength using tournament.py and include the results in your report.
"""
import random

import numpy as np


class Timeout(Exception):
    """Subclass base exception for code clarity."""
    pass


def crash_walls(move, walls):
    for wall in walls:
        if move in wall:
            return True
    return False


def is_valid_move(move, MAX_ROW, MAX_COL):
    r, c = move
    if r < 0 or r >= MAX_ROW:
        return False
    if c < 0 or c >= MAX_COL:
        return False
    return True


def compute_score(row, col, MAX_VAL=10, discount=0.9,
                  possible_dir=[(-2, -1), (-2, 1), (-1, -2), (-1, 2),
                                (1, -2), (1, 2), (2, -1), (2, 1)]):
    # compute score of each space
    center_point = (row // 2, col // 2)
    value = [[0.0 for c in range(col)] for r in range(row)]  # 7x7 list
    change = True
    while change:
        change = False
        for r in range(len(value)):
            for c in range(len(value[0])):
                if (r, c) == center_point:
                    if value[r][c] != MAX_VAL:
                        value[r][c] = MAX_VAL
                        change = True
                else:
                    near_points = [(r + delta_r, c + delta_c) for delta_r,
                                                                  delta_c in possible_dir if
                                   is_valid_move((r + delta_r, c + delta_c), row, col)]
                    max_near = max(value[x][y] for x, y in near_points)
                    # nearest space will receive discount
                    if max_near * discount != value[r][c]:
                        value[r][c] = max_near * discount
                        change = True
    return value


MAX_ROW, MAX_COLUMN = 7, 7
VALUE = compute_score(row=MAX_ROW, col=MAX_COLUMN, MAX_VAL=10, discount=0.9)


### Wong Hao Shan
def point_of_view_heuristic(game, player):
    '''
    A heuristic that calculates from the point of view of the given player.
    It was configured by two types of rules, the progress of game(percentage of
    game board filled) and whether the player or opponent crash with the boundary.
    '''

    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    width = game.width
    height = game.height

    walls = [
        [(0, i) for i in range(width)],
        [(i, 0) for i in range(height)],
        [(width - 1, i) for i in range(width)],
        [(i, height - 1) for i in range(height)]
    ]

    my_moves = game.get_legal_moves(player)
    opponent_moves = game.get_legal_moves(game.get_opponent(player))

    my_score = 0
    opponent_score = 0

    percent = int(len(game.get_blank_spaces()) / (width * height)) * 100

    for move in my_moves:
        if percent < 40:
            my_score += 10
        elif percent > 40 and percent < 75 and crash_walls(move, walls):
            my_score -= 25
        elif percent > 75 and crash_walls(move, walls):
            my_score -= 35
        elif not crash_walls(move, walls):
            my_score += 10

    for move in opponent_moves:
        if percent < 40:
            opponent_score += 10
        elif percent > 40 and percent < 75 and crash_walls(move, walls):
            opponent_score -= 25
        elif percent > 75 and crash_walls(move, walls):
            opponent_score -= 35
        elif not crash_walls(move, walls):
            opponent_score += 10

    return float(my_score - opponent_score * 1.5)


### Lim Jia Qi
def scoring_heuristic(game, player):
    """
    A heuristic function that perform based on the pre-computed values of each spaces
    and the average scores of player and opponent agent.
    """

    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    global VALUE

    my_moves = game.get_legal_moves(player)
    opponent_moves = game.get_legal_moves(game.get_opponent(player))

    my_last_move = game.get_player_location(player=player)
    opponent_last_move = game.get_player_location(player=game.get_opponent(player))

    my_prev_x, my_prev_y = my_last_move
    opp_prev_x, opp_prev_y = opponent_last_move

    my_score = len(my_moves) + VALUE[my_prev_x][my_prev_y]
    opp_score = len(opponent_moves) + VALUE[opp_prev_x][opp_prev_y]

    # calculate average score for future moves
    if len(my_moves) > 0:
        my_score += np.mean([VALUE[r][c] for r, c in my_moves])
    if len(opponent_moves) > 0:
        opp_score += np.mean([VALUE[r][c] for r, c in opponent_moves])

    return my_score - opp_score


### Cheah Jo Yen
def min_max_heuristic(game, player):
    '''
    A heuristic that try to minimize the opponent moves, meanwhile maximize our moves.
    '''

    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    return (my_moves * my_moves) - 5.0 * (opponent_moves * opponent_moves)


### Chong Sin Mei
def weight_factor_heuristic(game, player):
    """
    A hueristic which takes the difference between player and opponent moves
    as a scoring metric. The method applies proportional weight factor for
    opponent moves based on the number of open spaces remaining in the game
    state.
    """
    player_factor = 1
    opp_factor = 2
    space_factor = 1

    player_moves = game.get_legal_moves(player)
    opp_moves = game.get_legal_moves(game.get_opponent(player))

    open_spaces = len(game.get_blank_spaces())
    total_spaces = game.width * game.height

    if not opp_moves:
        return float("inf")

    elif not player_moves:
        return float("-inf")

    else:
        return float(player_factor * len(player_moves) + space_factor * (total_spaces / open_spaces)
                     - opp_factor * len(opp_moves))


def offensive_to_defensive_heuristic(game, player):
    '''
    On the first half of the game, play offensively to exhaust the possible moves for opponent,
    then play defensively to prioritize the player available movement.
    '''

    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    ratio = game.move_count / (game.width * game.height)
    defensive = (my_moves * 2) - opponent_moves
    offensive = my_moves - (opponent_moves * 2)

    return offensive if ratio <= 0.5 else defensive


def blocking_opponent_heuristic(game, player):
    '''
    A completely aggressive heuristic, it lets player agent seeks
    to hunt the opponent agent.
    '''

    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    possible_moves_count = 0
    # get a count of possible player and opponent moves that are same
    for p in game.get_legal_moves(player):
        for o in game.get_legal_moves(game.get_opponent(player)):
            if p == o:
                possible_moves_count += 1

    return my_moves - (opponent_moves * 2) + possible_moves_count


def custom_score(game, player):
    """Calculate the heuristic value of a game state from the point of view
    of the given player.

    Note: this function should be called from within a Player instance as
    `self.score()` -- you should not need to call this function directly.

    Parameters
    ----------
    game : `isolation.Board`
        An instance of `isolation.Board` encoding the current state of the
        game (e.g., player locations and blocked cells).

    player : object
        A player instance in the current game (i.e., an object corresponding to
        one of the player objects `game.__player_1__` or `game.__player_2__`.)

    Returns
    -------
    float
        The heuristic value of the current game state to the specified player.
    """

    # TODO: finish this function!
    return weighted_chances_heuristic(game, player)


def aggressive_heuristic(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    return my_moves - 1.5 * opponent_moves


def defensive_heuristic(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    return 1.5 * my_moves - opponent_moves


def maximizing_win_chances_heuristic(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = 1.0 * len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    if my_moves == 0:
        return float("-inf")

    if opponent_moves == 0:
        return float("inf")

    return my_moves / opponent_moves


def minimizing_losing_chances_heuristic(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = 1.0 * len(game.get_legal_moves(game.get_opponent(player)))

    if my_moves == 0:
        return float("-inf")

    if opponent_moves == 0:
        return float("inf")

    return -opponent_moves / my_moves


def chances_heuristic(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    return my_moves * my_moves - opponent_moves * opponent_moves


def weighted_chances_heuristic(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    return my_moves * my_moves - 1.5 * opponent_moves * opponent_moves


def weighted_chances_heuristic_2(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))

    return 1.5 * my_moves * my_moves - opponent_moves * opponent_moves


def move_diff_with_space(game, player):
    if game.is_loser(player):
        return float("-inf")

    if game.is_winner(player):
        return float("inf")

    my_moves = len(game.get_legal_moves(player))
    opponent_moves = len(game.get_legal_moves(game.get_opponent(player)))
    available_space = len(game.get_blank_spaces())
    space_weight_factor = (game.width * game.height / available_space)
    player_coeff = 1
    opponent_coeff = 2

    return player_coeff * my_moves + space_weight_factor - opponent_coeff * opponent_moves


class CustomPlayer:
    """Game-playing agent that chooses a move using your evaluation function
    and a depth-limited minimax algorithm with alpha-beta pruning. You must
    finish and test this player to make sure it properly uses minimax and
    alpha-beta to return a good move before the search time limit expires.

    Parameters
    ----------
    search_depth : int (optional)
        A strictly positive integer (i.e., 1, 2, 3,...) for the number of
        layers in the game tree to explore for fixed-depth search. (i.e., a
        depth of one (1) would only explore the immediate sucessors of the
        current state.)

    score_fn : callable (optional)
        A function to use for heuristic evaluation of game states.

    iterative : boolean (optional)
        Flag indicating whether to perform fixed-depth search (False) or
        iterative deepening search (True).

    method : {'minimax', 'alphabeta'} (optional)
        The name of the search method to use in get_move().

    timeout : float (optional)
        Time remaining (in milliseconds) when search is aborted. Should be a
        positive value large enough to allow the function to return before the
        timer expires.
    """

    def __init__(self, search_depth=3, score_fn=custom_score,
                 iterative=True, method='minimax', timeout=10.):
        self.search_depth = search_depth
        self.iterative = iterative
        self.score = score_fn
        self.method = method
        self.time_left = None
        self.TIMER_THRESHOLD = timeout - 0.5

    def get_move(self, game, legal_moves, time_left):
        """Search for the best move from the available legal moves and return a
        result before the time limit expires.

        This function must perform iterative deepening if self.iterative=True,
        and it must use the search method (minimax or alphabeta) corresponding
        to the self.method value.

        **********************************************************************
        NOTE: If time_left < 0 when this function returns, the agent will
              forfeit the game due to timeout. You must return _before_ the
              timer reaches 0.
        **********************************************************************

        Parameters
        ----------
        game : `isolation.Board`
            An instance of `isolation.Board` encoding the current state of the
            game (e.g., player locations and blocked cells).

        legal_moves : list<(int, int)>
            A list containing legal moves. Moves are encoded as tuples of pairs
            of ints defining the next (row, col) for the agent to occupy.

        time_left : callable
            A function that returns the number of milliseconds left in the
            current turn. Returning with any less than 0 ms remaining forfeits
            the game.

        Returns
        -------
        (int, int)
            Board coordinates corresponding to a legal move; may return
            (-1, -1) if there are no available legal moves.
        """

        self.time_left = time_left

        # Perform any required initializations, including selecting an initial
        # move from the game board (i.e., an opening book), or returning
        # immediately if there are no legal moves

        move = (-1, -1)

        if not legal_moves:
            return move

        search_strategy = eval('self.' + self.method)

        try:
            # The search method call (alpha beta or minimax) should happen in
            # here in order to avoid timeout. The try/except block will
            # automatically catch the exception raised by the search method
            # when the timer gets close to expiring

            if self.iterative:
                for depth in range(1, game.width * game.height):
                    score, move = search_strategy(game, depth)
                    if score == float('inf'):
                        break
            else:
                score, move = search_strategy(game, self.search_depth)
            pass

        except Timeout:
            # Handle any actions required at timeout, if necessary
            pass

        if move == (-1, -1):
            move = legal_moves[random.randint(0, len(legal_moves) - 1)]

        # Return the best move from the last completed search iteration
        return move

    def minimax(self, game, depth, maximizing_player=True):
        """Implement the minimax search algorithm as described in the lectures.

        Parameters
        ----------
        game : isolation.Board
            An instance of the Isolation game `Board` class representing the
            current game state

        depth : int
            Depth is an integer representing the maximum number of plies to
            search in the game tree before aborting

        maximizing_player : bool
            Flag indicating whether the current search depth corresponds to a
            maximizing layer (True) or a minimizing layer (False)

        Returns
        -------
        float
            The score for the current search branch

        tuple(int, int)
            The best move for the current branch; (-1, -1) for no legal moves

        Notes
        -----
            (1) You MUST use the `self.score()` method for board evaluation
                to pass the project unit tests; you cannot call any other
                evaluation function directly.
        """
        if self.time_left() < self.TIMER_THRESHOLD:
            raise Timeout()

        if depth == 0:
            return self.score(game, self), game.get_player_location(self)

        ret_score = float('-inf') if maximizing_player else float('inf')
        ret_move = (-1, -1)
        player = self if maximizing_player else game.get_opponent(self)
        possible_moves = game.get_legal_moves(player)

        if len(possible_moves) == 0:
            return self.score(game, self), (-1, -1)

        for move in possible_moves:
            score, _move = self.minimax(game.forecast_move(move), depth - 1, not maximizing_player)

            if maximizing_player:
                if score >= ret_score:
                    ret_score = score
                    ret_move = move

            else:
                if score <= ret_score:
                    ret_score = score
                    ret_move = move

        return ret_score, ret_move

    def alphabeta(self, game, depth, alpha=float("-inf"), beta=float("inf"), maximizing_player=True):
        """Implement minimax search with alpha-beta pruning as described in the
        lectures.

        Parameters
        ----------
        game : isolation.Board
            An instance of the Isolation game `Board` class representing the
            current game state

        depth : int
            Depth is an integer representing the maximum number of plies to
            search in the game tree before aborting

        alpha : float
            Alpha limits the lower bound of search on minimizing layers

        beta : float
            Beta limits the upper bound of search on maximizing layers

        maximizing_player : bool
            Flag indicating whether the current search depth corresponds to a
            maximizing layer (True) or a minimizing layer (False)

        Returns
        -------
        float
            The score for the current search branch

        tuple(int, int)
            The best move for the current branch; (-1, -1) for no legal moves

        Notes
        -----
            (1) You MUST use the `self.score()` method for board evaluation
                to pass the project unit tests; you cannot call any other
                evaluation function directly.
        """
        if self.time_left() < self.TIMER_THRESHOLD:
            raise Timeout()

        if depth == 0:
            return self.score(game, self), game.get_player_location(self)

        ret_score = float('-inf') if maximizing_player else float('inf')
        ret_move = (-1, -1)
        possible_moves = game.get_legal_moves(self) if maximizing_player else game.get_legal_moves(
            game.get_opponent(self))

        if len(possible_moves) == 0:
            return self.score(game, self), (-1, -1)

        for move in possible_moves:
            score, _move = self.alphabeta(game.forecast_move(move), depth - 1, alpha, beta, not maximizing_player)

            if maximizing_player:
                if score >= ret_score:
                    ret_score = score
                    ret_move = move
                if ret_score >= beta:
                    return ret_score, ret_move
                alpha = max(ret_score, alpha)

            else:
                if score <= ret_score:
                    ret_score = score
                    ret_move = move
                if ret_score <= alpha:
                    return ret_score, ret_move
                beta = min(ret_score, beta)

        return ret_score, ret_move
