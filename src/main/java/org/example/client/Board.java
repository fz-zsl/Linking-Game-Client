package org.example.client;

import java.util.ArrayList;
import java.util.Optional;

public class Board {

    public static final int EMPTY = -1;

    public int row;
    public int col;
    public int[][] content;

    public Board(int row, int col, int[][] content) {
        this.row = row;
        this.col = col;
        this.content = content;
    }

    public Board(ArrayList<Integer> content) {
        this.row = content.get(0);
        this.col = content.get(1);
        this.content = new int[this.row][this.col];
        for (int i = 0; i < this.row; ++i) {
            for (int j = 0; j < this.col; ++j) {
                this.content[i][j] = content.get(i * this.col + j + 2);
            }
        }
    }

    public void setBoard(int row, int col, int val) {
        content[row][col] = val;
    }

    public void setBoard(int row, int col) {
        setBoard(row, col, EMPTY);
    }

    public Optional<ArrayList<Position>> judge(int row1, int col1, int row2, int col2) {
        if ((content[row1][col1] != content[row2][col2]) || (row1 == row2 && col1 == col2)) {
            return Optional.empty();
        }

        // one line
        if (isDirectlyConnected(row1, col1, row2, col2, content)) {
            ArrayList<Position> res = new ArrayList<>();
            res.add(new Position(row1, col1));
            res.add(new Position(row2, col2));
            return Optional.of(res);
        }

        // two lines
        if ((row1 != row2) && (col1 != col2)) {
            if (content[row1][col2] == EMPTY && isDirectlyConnected(row1, col1, row1, col2, content)
                    && isDirectlyConnected(row1, col2, row2, col2, content)) {
                ArrayList<Position> res = new ArrayList<>();
                res.add(new Position(row1, col1));
                res.add(new Position(row1, col2));
                res.add(new Position(row2, col2));
                return Optional.of(res);
            }

            if (content[row2][col1] == EMPTY && isDirectlyConnected(row2, col2, row2, col1, content)
                    && isDirectlyConnected(row2, col1, row1, col1, content)) {
                ArrayList<Position> res = new ArrayList<>();
                res.add(new Position(row1, col1));
                res.add(new Position(row2, col1));
                res.add(new Position(row2, col2));
                return Optional.of(res);
            }
        }

        // three lines
        if(row1 != row2) {
            for (int i = 0; i < content[0].length; i++) {
                if (content[row1][i] == EMPTY && content[row2][i] == EMPTY &&
                        isDirectlyConnected(row1, col1, row1, i, content) &&
                        isDirectlyConnected(row1, i, row2, i, content) &&
                        isDirectlyConnected(row2, col2, row2, i, content)) {
                    ArrayList<Position> res = new ArrayList<>();
                    res.add(new Position(row1, col1));
                    res.add(new Position(row1, i));
                    res.add(new Position(row2, i));
                    res.add(new Position(row2, col2));
                    return Optional.of(res);
                }
            }
        }
        if(col1 != col2) {
            for (int j = 0; j < content.length; j++) {
                if (content[j][col1] == EMPTY && content[j][col2] == EMPTY &&
                        isDirectlyConnected(row1, col1, j, col1, content) &&
                        isDirectlyConnected(j, col1, j, col2, content) &&
                        isDirectlyConnected(row2, col2, j, col2, content)) {
                    ArrayList<Position> res = new ArrayList<>();
                    res.add(new Position(row1, col1));
                    res.add(new Position(j, col1));
                    res.add(new Position(j, col2));
                    res.add(new Position(row2, col2));
                    return Optional.of(res);
                }
            }
        }

        return Optional.empty();
    }

    // judge whether
    private boolean isDirectlyConnected(int row1, int col1, int row2, int col2, int[][] board) {
        if (row1 == row2) {
            int minCol = Math.min(col1, col2);
            int maxCol = Math.max(col1, col2);
            for (int col = minCol + 1; col < maxCol; col++) {
                if (board[row1][col] != EMPTY) {
                    return false;
                }
            }
            return true;
        } else if (col1 == col2) {
            int minRow = Math.min(row1, row2);
            int maxRow = Math.max(row1, row2);
            for (int row = minRow + 1; row < maxRow; row++) {
                if (board[row][col1] != EMPTY) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // TODO: handle end game logic
    public boolean hasMoreMoves() {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (content[i][j] != EMPTY) {
                    for (int k = 0; k < row; k++) {
                        for (int l = 0; l < col; l++) {
                            if (i == k && j == l || content[k][l] == EMPTY) {
                                continue;
                            }
                            if (content[i][j] == content[k][l] && judge(i, j, k, l).isPresent()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
