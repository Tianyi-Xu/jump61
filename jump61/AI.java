// This file contains definitions for an OPTIONAL part of your project.  If you
// choose not to do the optional point, you can delete this file from your
// project.

// This file contains a SUGGESTION for the structure of your program.  You
// may change any of it, or add additional files to this directory (package),
// as long as you conform to the project specification.

// Comments that start with "//" are intended to be removed from your
// solutions.
package jump61;

import afu.org.checkerframework.checker.igj.qual.I;

import javax.swing.text.Position;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.PrimitiveIterator;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger, Tianyi Xu
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
        foundMoves = new ArrayList<>();
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        int value;
        ArrayList<Integer> foundMoves = new ArrayList<>();
        assert getSide() == work.whoseMove();
        _foundMove = -1;

        if (getSide() == RED) {
            value = minMax(work, 4, true, 1, -Integer.MAX_VALUE, Integer.MAX_VALUE);
            } else {
            value = minMax(work, 4, true, -1, -Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        return _foundMove;
    }


    private ArrayList<Integer> findVaildPos(Board work, Side side) {
        ArrayList<Integer> validPos = new ArrayList<>();

        for (int i = 0; i < work.size() * work.size(); i++)  {
            if ( work.get(i).getSide() == WHITE) {
                validPos.add(i);
            } else if (work.get(i).getSide() == side && work.get(i).getSpots() <= work.neighbors(i)) {
                validPos.add(i);
            }
        }
        return validPos;
    }


    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove.
     *
     *   return the heuristic value of a move and record that move in _foundMove.
     *  */


    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        Side side = sense == 1? RED : BLUE;

        if (board.getWinner() != null || depth == 0) {
            return staticEval(board, WINNNING_VAl);
        }

        int bestSoFar = - sense * Integer.MAX_VALUE;
        ArrayList<Integer> validPos = findVaildPos(board, side);
//        System.out.println(validPos);
        for (int n : validPos) {
            /** Add one validPos */
            board.addSpot(side, n);
            int response = minMax(board, depth - 1, false, - sense, alpha, beta);
            /** Backtracking, undo the add */
            board.undo();

            if((sense == -1 && response <= bestSoFar) || (sense == 1 && response >= bestSoFar)) {
                /**Update the bestSoFar */
                if (response != bestSoFar) {
                    bestSoFar = response;
                    if (saveMove) {
                        foundMoves.clear();
                    }
                }
                if (saveMove) {
                    foundMoves.add(n);
                }


                switch (sense) {
                    /** If find maximum, update alpha to be the bestSofar */
                    case 1 : alpha = Math.max(alpha, bestSoFar); break;
                    /** If find minimum, update beta to be the bestSofar */
                    case -1 : beta = Math.min(beta, bestSoFar); break;
                }
                /** Pruning if alpha >= beta */
                if (alpha >= beta) {
                    return bestSoFar;
                }
            }
        }
        if(saveMove) {
            System.out.println(foundMoves);
            if (foundMoves.size() > 0){
                _foundMove = foundMoves.get(_random.nextInt(foundMoves.size()));
            }
        }
        return bestSoFar;
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        Side side = getSide();
        if (b.getWinner() != null) {
            if (b.getWinner() == BLUE) {
                return -winningValue;
            }
            if (b.getWinner() == RED) {
                return winningValue;
            }
        }
        return side == RED? b.numOfSide(side) + b.numOfSide(WHITE) : -(b.numOfSide(side) + b.numOfSide(WHITE));
    }

    /** A random-number generator used for move selection.
     * The idea of _random is that if there are multiple best moves to chose from you have a way to tie break
     * */

    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;

    private ArrayList<Integer> foundMoves;

    private final int WINNNING_VAl = 1000000;
}
