package org.example.client;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class Controller {
    @FXML
    public Label username;
    @FXML
    public Label userScore;
    @FXML
    public Label opponentName;
    @FXML
    public Label opponentScore;

    @FXML
    private GridPane gameBoard;
    private Canvas canvas;

    public Runnable gameOverEventHandler;
    public Runnable updateUserScoreEventHandler;
    public Runnable updateOpponentScoreEventHandler;
    public Runnable deductUserScoreEventHandler;
    public Runnable deductOpponentScoreEventHandler;
    
    public Board board;
    int[] position = new int[3];
    int[] userButtonPress = new int[]{-1, -1};

    public GridPane getGameBoard() {
        return gameBoard;
    }

    public void createGameBoard() {
        gameBoard.getChildren().clear();

        canvas = new Canvas(51 * board.col, 44.44 * board.row); // 根据实际大小调整
        gameBoard.add(canvas, 0, 0, board.col, board.row); // 将Canvas添加到GridPane上，覆盖所有按钮

        for (int row = 0; row < board.row; row++) {
            for (int col = 0; col < board.col; col++) {
                Button button = new Button();
                button.setPrefSize(40, 40);
                ImageView imageView;
                if (board.content[row][col] < 0) {
                    imageView = addContent(0);
                }
                else {
                    imageView = addContent(board.content[row][col]);
                }
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
                button.setGraphic(imageView);
                if (board.content[row][col] < 0) {
                    button.setVisible(false);
                }

                int finalRow = row;
                int finalCol = col;
                button.setBackground(javafx.scene.layout.Background.fill(Color.WHITE));

                gameBoard.add(button, col, row);
            }
        }
    }

    public void drawLine(int startRow, int startCol, int endRow, int endCol) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(4);
        double buttonWidth = 51;
        double buttonHeight = 44.44;
        double startX = startCol * buttonWidth + buttonWidth / 2;
        double startY = startRow * buttonHeight + buttonHeight / 2;
        double endX = endCol * buttonWidth + buttonWidth / 2;
        double endY = endRow * buttonHeight + buttonHeight / 2;
        gc.strokeLine(startX, startY, endX, endY);
    }

    public void handleButtonPress(int row, int col, boolean myClick) {
        System.out.println(row + " " + col + " " + myClick);
        if (myClick) {
            userButtonPress = new int[]{row, col};
        }
        else {
            userButtonPress = new int[]{-1, -1};
        }
        if (position[0] == 0) {
            position[1] = row;
            position[2] = col;
            position[0] = 1;
            int idx = (col + row * board.col) + 1;
            Button button = (Button) gameBoard.getChildren().get(idx);
            button.setBackground(javafx.scene.layout.Background.fill(Color.AQUA));
        } else {
            Optional<ArrayList<Position>> route = board.judge(position[1], position[2], row, col);
            position[0] = 0;
            if (route.isPresent()) {
                int idx = (col + row * board.col) + 1;
                Button button = (Button) gameBoard.getChildren().get(idx);
                button.setBackground(javafx.scene.layout.Background.fill(Color.AQUA));

                ArrayList<Position> path = route.get();
                for (int i = 0; i < path.size() - 1; ++i) {
                    Position start = path.get(i);
                    Position end = path.get(i + 1);
                    drawLine(start.first, start.second, end.first, end.second);
                }

                PauseTransition pause = new PauseTransition(Duration.millis(300));
                pause.setOnFinished(event -> {
                    GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.clearRect(0, 0, 51 * board.col, 44.44 * board.row);
                });
                pause.play();
                SoundPlayer.playSound("valid.wav");

                int startIdx = (position[2] + position[1] * board.col) + 1;
                int endIdx = (col + row * board.col) + 1;
                gameBoard.getChildren().get(startIdx).setVisible(false);
                gameBoard.getChildren().get(endIdx).setVisible(false);
                board.setBoard(position[1], position[2]);
                board.setBoard(row, col);

                if (myClick) {
                    updateUserScoreEventHandler.run();
                }
                else {
                    Platform.runLater(() -> {
                        updateOpponentScoreEventHandler.run();
                    });
                }
            }
            else {
                int idx = (position[2] + position[1] * board.col) + 1;
                Button button = (Button) gameBoard.getChildren().get(idx);
                button.setBackground(javafx.scene.layout.Background.fill(Color.WHITE));
                SoundPlayer.playSound("invalid.wav");

                if (myClick) {
                    deductUserScoreEventHandler.run();
                }
                else {
                    Platform.runLater(() -> {
                        deductOpponentScoreEventHandler.run();
                    });
                }
            }
        }
    }

    public void handleInvalidPress() {
        Application.showAlert("Error", "Error", "It's not your turn, please wait.");
    }

    public ImageView addContent(int content) {
        return switch (content) {
            case 0 -> new ImageView(imageCarambola);
            case 1 -> new ImageView(imageApple);
            case 2 -> new ImageView(imageMango);
            case 3 -> new ImageView(imageBlueberry);
            case 4 -> new ImageView(imageCherry);
            case 5 -> new ImageView(imageGrape);
            case 6 -> new ImageView(imageKiwi);
            case 7 -> new ImageView(imageOrange);
            case 8 -> new ImageView(imagePeach);
            case 9 -> new ImageView(imagePear);
            case 10 -> new ImageView(imagePineapple);
            case 11 -> new ImageView(imageWatermelon);
            default -> null;
        };
    }

    public static Image imageApple = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/apple.png")).toExternalForm());
    public static Image imageMango = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/mango.png")).toExternalForm());
    public static Image imageBlueberry = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/blueberry.png")).toExternalForm());
    public static Image imageCherry = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/cherry.png")).toExternalForm());
    public static Image imageGrape = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/grape.png")).toExternalForm());
    public static Image imageCarambola = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/carambola.png")).toExternalForm());
    public static Image imageKiwi = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/kiwi.png")).toExternalForm());
    public static Image imageOrange = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/orange.png")).toExternalForm());
    public static Image imagePeach = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/peach.png")).toExternalForm());
    public static Image imagePear = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/pear.png")).toExternalForm());
    public static Image imagePineapple = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/pineapple.png")).toExternalForm());
    public static Image imageWatermelon = new Image(Objects.requireNonNull(Board.class.getResource("/org/example/client/watermelon.png")).toExternalForm());
}
