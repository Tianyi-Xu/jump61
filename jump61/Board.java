// This file contains a SUGGESTION for the structure of your program.  You
// may change any of it, or add additional files to this directory (package),
// as long as you conform to the project specification.

// Comments that start with "//" are intended to be removed from your
// solutions.
package jump61;

import com.sun.tools.internal.xjc.model.CArrayInfo;
import static java.lang.System.arraycopy;

import java.util.*;

import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.INITIAL;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier--a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Tianyi Xu
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _size = N;
        _readonlyBoard = new ConstantBoard(this);

        _squares = new Square[N][N];
        for (Square[] ss : _squares) {
            for (int i = 0; i < N; i++) {
                ss[i] = INITIAL;
            }
        }

        _history = new ArrayDeque<>();
        markUndo();
        _numMoves = 0;
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        copy(board0);
    }

    /** Returns a read only version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _size = N;
        _squares = new Square[N][N];
        for (Square[] ss : _squares) {
            for (int i = 0; i < N; i++) {
                ss[i] = INITIAL;
            }
        }
        _history.clear();
        markUndo();
        _numMoves = 0;
        announce();
    }

    /** Copy the contents of BOARD into me.
     * clear the undo history and set number of moves back to zero */
    void copy(Board board) {
        _size = board.size();
        _squares = new Square[_size][_size];
        for (int i = 0; i < _size * _size; i++) {
            int r = row(i);
            int c = col(i);
            Side side = board.get(i).getSide();
            int numSpot = board.get(i).getSpots();
            internalSet(r, c, numSpot, side);
        }
        _history.clear();
        markUndo();
        _numMoves = 0;
        announce();
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        for (int i = 0; i < _size; i++) {
            arraycopy(board._squares[i], 0, this._squares[i],0, _size);
        }
        _history.add(new GameState());
        _history.getLast().saveState();
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size; // FIXME
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        if (exists(r, c)) {
            return get(sqNum(r, c));
        }
        return null;
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        if (exists(n)) {
            return _squares[n / size()][n % size()];
        }
        return null; // FIXME
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int totalSpots = 0;
        for (Square[] ss : _squares) {
            for (Square s : ss) {
                totalSpots += s.getSpots();
            }
        }
        return totalSpots; // FIXME
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return exists(r, c) && isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        if (!exists(n)) {
            return false;
        }
        if (get(n).getSpots() > neighbors(row(n), col(n))) {
            return false;
        }
        // If game is over
        if (getWinner() != null) {
            return false;
        }
        return isLegal(player) && get(n).getSide() != player.opposite(); // FIXME
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return whoseMove() == player; // FIXME
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        if (numOfSide(RED) == _size * _size)  {
            return RED;
        }
        if (numOfSide(BLUE) == _size * _size) {
            return BLUE;
        }
        return null;  // FIXME
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int num = 0;
        for (Square[] ss : _squares) {
            for (Square s : ss) {
                if (s.getSide() == side) {
                    num++;
                }
            }
        }
        return num; // FIXME
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        // FIXME
        set(r, c, get(r, c).getSpots() + 1, player);
        if (isOverFlow(sqNum(r,c))){
            jump(sqNum(r, c));
        }
        markUndo();
        _numMoves += 1;
        announce();
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        // FIXME
        addSpot(player, row(n), col(n));
        ;
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        if (num > 0) {
            internalSet(r, c, num, player);
        } else if (num == 0){
            internalSet(r, c, num, WHITE);
        }
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        if (exists(r, c)) {
            internalSet(sqNum(r, c), num, player);
        }

    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        if (exists(n)) {
            if (num == 0) {
                _squares[n / size()][n % size()] = square(WHITE, num);
            } else {
                _squares[n / size()][n % size()] = square(player, num);
            }

        }
    }

    // There are two obvious ways to conduct a game-tree search in the AI.
    //
    // First, you can explore the consequences of a possible move from
    // position A by making a copy of the Board in position A, and then
    // modifying that copy. Since you retain position A, you can return to
    // it to try other moves from that position.
    //
    // Second, you can explore the consequences of a possible move from
    // position A by making that move on your Board and then, when your
    // analysis of the move is complete, undoing the move to return you to
    // position A. This method is more complicated to implement, but has
    // the advantage that it can be considerably faster than making copies
    // of the Board (you will need one copy per move tried, which will very
    // quickly be thrown away).

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (_history.isEmpty()) {
            return;
        }
        _history.removeLast(); // pop?
        _history.getLast().restoreState(); // why can't use peek?
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        _history.add(new GameState());
        _history.getLast().saveState();
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();
    void forNeighbors(int S, Consumer<Integer> action) {
        int[] verticalDistance = new int[]{-1, 1, 0, 0};
        int[] horizontalDistance  = new int[]{0, 0, -1, 1};
        int row = row(S);
        int col = col(S);
        for (int i = 0; i < 4; i++) {
            int neighbour_r = row + verticalDistance[i];
            int neighbour_c = col + horizontalDistance[i];
            if (exists(neighbour_r, neighbour_c)) {
                action.accept(sqNum(neighbour_r, neighbour_c));
            }
        }
    }

    private boolean isOverFlow(int n) {
        if (get(n).getSpots() > neighbors(n)) {
            return true;
        }
        return false;
    }



    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        Side player = whoseMove().opposite();
        _workQueue.add(S);
        while(!_workQueue.isEmpty() && getWinner() == null) {
            int pos = _workQueue.pop();
            forNeighbors(pos, (neighbor_n) -> {
                if (!isOverFlow(neighbor_n)) {
                    internalSet(pos, get(pos).getSpots() - 1, player);
                    internalSet(neighbor_n, get(neighbor_n).getSpots() + 1, player);

                    if (isOverFlow(neighbor_n)) {
                        _workQueue.add(neighbor_n);
                    }
                }
            });
        }


        // FIXME
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===\n");
        for (int i = 0; i < size(); i++) {
            out.format("    ");
            for (int j = 0; j < size(); j++) {
                if (j == size() - 1) {
                    out.format(squareToString(_squares[i][j]));
                } else {
                    out.format("%-3s", squareToString(_squares[i][j]));
                }
            }
            out.format("\n");
        }
        out.format("===\n");
        return out.toString();
    }

    private String squareToString(Square s) {
        Side side = s.getSide();
        String spots = Integer.toString(s.getSpots());
        String result = spots;
        switch (side) {
            case RED: result += "r";
            break;
            case BLUE: result += "b";
            break;
            default: result += "-";
        }
        return result;
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            if (B.size() != this.size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                for (int j = 0; j < size(); j++) {
                    Square square = _squares[i][j];
                    Square Bsquare = B._squares[i][j];
                    if (!square.getSide().equals(Bsquare.getSide())) {
                        return false;
                    }

                    if (square.getSpots() != Bsquare.getSpots()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** Represents enough of the state of a game to allow undoing and
     *  redoing of moves. */
    private class GameState {
        GameState() {
            savedCells = new Square[size()][size()];
        }

        void saveState() {
            for (int i = 0; i < size(); i++) {
                arraycopy(_squares[i], 0, savedCells[i], 0, size());
            }
        }

        void restoreState() {
            for (int i = 0; i < size(); i++) {
                arraycopy(savedCells[i], 0, _squares[i], 0, size());
            }
        }
        private Square[][] savedCells;


    }


    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    // FIXME: other instance variables here.

    /** Size of the broard. */
    private int _size;

    /** The content of the board */
    private Square[][] _squares;

    /** Number of moves */
    private int _numMoves;

    /** History of the board */
    private ArrayDeque<GameState> _history;
//    /** The position of the current state in _history.  This is always
//     *  non-negative and <=_lastHistory.  */
//    private int _current;
//
//    /** The index of the last valid state in _history, including those
//     *  that can be redone (with indices >_current). */
//    private int _lastHistory;




}
