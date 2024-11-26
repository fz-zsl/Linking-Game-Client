package org.example.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.collections.FXCollections;

public class Application extends javafx.application.Application {
    private String username;
    private String opponentName;
    private int userScore;
    private int opponentScore;
    private int turn;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Controller controller;
    private Thread communicationThread;

    private Stage matchmakingStage;
    private ListView<String> usersListView;
    private ObservableList<String> usersObservableList;
    private ExecutorService executorService, matchService, echoService;
    private boolean matchmakingFlag;

    private Stage gameStage;

    @Override
    public void start(Stage gameStage) {
        this.gameStage = gameStage;
        username = getUsername();
        connectToServer("127.0.0.1", 1268);
        chooseOpponent();
    }

    private String getUsername() {
        Stage usernameStage = new Stage();
        usernameStage.setTitle("Enter Username");
        VBox vBox = new VBox(10);
        vBox.setSpacing(10);
        vBox.setPrefSize(200, 100);

        javafx.scene.control.Label usernameLabel = new javafx.scene.control.Label("Enter Username:");
        javafx.scene.control.TextField usernameField = new javafx.scene.control.TextField();
        usernameField.setPrefColumnCount(10);

        javafx.scene.control.Button okButton = new javafx.scene.control.Button("OK");
        okButton.setOnAction(event -> {
            String username = usernameField.getText();
            if (username.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Username cannot be empty.");
                alert.showAndWait();
            } else if (!username.matches("\\w+")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Username can only contain letters and numbers.");
                alert.showAndWait();
                usernameField.clear();
            } else {
                usernameStage.close();
            }
        });

        vBox.getChildren().addAll(usernameLabel, usernameField, okButton);
        usernameStage.setScene(new Scene(vBox));
        usernameStage.showAndWait();

        try {
            return usernameField.getText();
        } catch (NumberFormatException e) {
            return "Anonymous";
        }
    }

    private void chooseOpponent() {
        matchmakingStage = new Stage();
        matchmakingStage.setTitle("Select User");
        VBox vBox = new VBox(10);
        vBox.setSpacing(10);
        vBox.setPrefSize(200, 300);

        Label userLabel = new Label("Select User to Pair With:");
        usersListView = new ListView<>();
        usersObservableList = FXCollections.observableArrayList();
        usersListView.setItems(usersObservableList);

        Button refreshButton = new Button("Refresh List");
        refreshButton.setOnAction(event -> refreshUserList());

        vBox.getChildren().addAll(userLabel, usersListView, refreshButton);
        matchmakingStage.setScene(new Scene(vBox));
        matchmakingStage.show();

        matchmakingFlag = true;
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                try {
                    out.write("PAIR;" + newValue.split(" ")[0]);
                    out.newLine();
                    out.flush();

                    if (in.readLine().equals("MATCH;VALID") && matchmakingFlag) {
                        matchmakingFlag = false;
                        matchService.close();
                        matchmakingStage.close();
                        turn = 1;
                        System.out.println("Enter game ...");
                        enterGame();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        refreshUserList();
        matchService = Executors.newSingleThreadExecutor();
        matchService.submit(this::matchmakingProcess);
    }

    private void matchmakingProcess() {
        try {
            while (matchmakingFlag) {
                System.out.println("Keep sending signals");
                out.write("CHECK;MATCH");
                out.newLine();
                out.flush();

                String response = in.readLine();
                System.out.println("Receive: " + response);
                if ("MATCH;VALID".equals(response)) {
                    matchmakingFlag = true;
                    Platform.runLater(() -> {
                        matchmakingStage.close();
                    });
                    System.out.println("Enter game automatically ...");
                    Platform.runLater(() -> {
                        enterGame();
                    });
                    matchService.close();
                    break;
                }
                else {
                    Thread.sleep(300);
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("Match invalid.");
        }
    }

    private void enterGame() {
        try {
            out.write("RETRIEVE;GAMEINFO");
            out.newLine();
            out.flush();
            opponentName = in.readLine().split(";")[1];  // "USERNAME;[username]"
            turn = Integer.parseInt(in.readLine().split(";")[1]);  // "TURN; [turn]"
            String score = in.readLine().split(";")[1];
            userScore = Integer.parseInt(score.split(",")[0]);
            opponentScore = Integer.parseInt(score.split(",")[1]);
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("board.fxml"));
            VBox root = fxmlLoader.load();
            controller = fxmlLoader.getController();
            controller.board = new Board(deserializeBase64ToList(in.readLine()));
            controller.createGameBoard();
            setupGameEventHandlers(controller);
            Scene scene = new Scene(root);
            gameStage.setTitle("Linking Game!");
            gameStage.setScene(scene);
            gameStage.show();
            controller.username.setText(username);
            controller.userScore.setText(userScore + "");
            controller.opponentName.setText(opponentName);
            controller.opponentScore.setText(opponentScore + "");
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(this::communicationLoop);
            echoService = Executors.newSingleThreadExecutor();
            echoService.submit(this::checkServerStatus);
        } catch (IOException e) {
//            showAlert("Disconnected from server.", "Error", "Please check your network connection.");
            System.out.println("Disconnected from server, please check your network connection.");
        }
    }

    private void setupGameEventHandlers(Controller controller) {
        controller.gameOverEventHandler = () -> {
            String msg = "";
            if (userScore > opponentScore) {
                msg = "Congratulations, you win.";
            }
            else if (userScore == opponentScore) {
                msg = "Tie! ovo";
            }
            else {
                msg = "You lose ... Keep trying!";
            }
            String finalMsg = msg;
            Platform.runLater(() -> {
                showAlert("Game Over", "Game Over", finalMsg);
                try {
                    out.write("OFFLINE;CLIENT");
                    out.newLine();
                    out.flush();
                    disconnect();
                } catch (IOException e) {
                    System.out.println("Error sending offline message.");
                }
                System.exit(0);
            });
        };

        controller.updateUserScoreEventHandler = () -> {
            ++userScore;
            controller.userScore.setText(userScore + "");
        };

        controller.updateOpponentScoreEventHandler = () -> {
            ++opponentScore;
            controller.opponentScore.setText(opponentScore + "");
        };

        controller.deductUserScoreEventHandler = () -> {
            --userScore;
            controller.userScore.setText(userScore + "");
        };

        controller.deductOpponentScoreEventHandler = () -> {
            --opponentScore;
            controller.opponentScore.setText(opponentScore + "");
        };
    }

    private void refreshUserList() {
        try {
            out.write("REFRESH;USERLIST");
            out.newLine();
            out.flush();

            String encodedList = in.readLine();
            if (encodedList.equals("MATCH;VALID")) {
                matchmakingStage.close();
                System.out.println("Enter game ...");
                enterGame();
            }
            else {
                ArrayList<String> users = deserializeBase64ToList(encodedList);
                System.out.println(users);
                usersObservableList.clear();
                usersObservableList.addAll(users);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private<T> ArrayList<T> deserializeBase64ToList(String base64Encoded) {
        byte[] bytes = Base64.getDecoder().decode(base64Encoded);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ArrayList<T>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void connectToServer(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            out.write("USERNAME;" + username);  // "USERNAME;[username]"
            out.newLine();
            out.flush();
        } catch (IOException e) {
//            e.printStackTrace();
            showAlert(
                    "Connection Error", "Connection Error",
                    "Cannot connect to server, please check IP address and port."
            );
        }
    }

    private void myTurn() throws IOException {
        for (int row = 0; row < controller.board.row; ++row) {
            for (int col = 0; col < controller.board.col; ++col) {
                int idx = (col + row * controller.board.col) + 1;
                Button button = (Button) controller.getGameBoard().getChildren().get(idx);
                int finalRow = row, finalCol = col;
                button.setOnAction(event -> controller.handleButtonPress(finalRow, finalCol, true));
            }
        }
        while (true) {
            if (controller.userButtonPress[0] >= 0) {
                System.out.println("I click" + controller.userButtonPress[0] + "," + controller.userButtonPress[1]);
                out.write("CLICK;" + controller.userButtonPress[0] + "," + controller.userButtonPress[1]);
                out.newLine();
                out.flush();
                controller.userButtonPress = new int[]{-1, -1};
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void oppTurn() throws IOException {
        for (int row = 0; row < controller.board.row; ++row) {
            for (int col = 0; col < controller.board.col; ++col) {
                int idx = (col + row * controller.board.col) + 1;
                Button button = (Button) controller.getGameBoard().getChildren().get(idx);
                button.setOnAction(event -> controller.handleInvalidPress());
            }
        }
        String message = in.readLine();
        System.out.println(message);
        while (message.equals("OFFLINE;OPPONENT")) {
            Platform.runLater(() ->
                showAlert("Notification", "Notification", "Your opponent is offline, please wait.")
            );
            message = in.readLine();
        }
        if (message.startsWith("OPPOFFLINE")) {
            Platform.runLater(() ->
                showAlert(
                        "Connection Error", "Connection Error",
                        "Your opponent has been disconnected from server."
                )
            );
            throw new OfflineException("Opponent offline");
        }
        message = message.split(";")[1];
        int row = Integer.parseInt(message.split(",")[0]);
        int col = Integer.parseInt(message.split(",")[1]);
        controller.handleButtonPress(row, col, false);
    }

    private void communicationLoop() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (turn == 1) {
                System.out.println("My turn");
                myTurn();
                myTurn();
            }
            while (true) {
                if (!controller.board.hasMoreMoves()) {
                    controller.gameOverEventHandler.run();
                    break;
                }
                System.out.println("Opp turn");
                oppTurn();
                oppTurn();
                if (!controller.board.hasMoreMoves()) {
                    controller.gameOverEventHandler.run();
                    break;
                }
                System.out.println("My turn");
                myTurn();
                myTurn();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Connection Error", "Error", "Cannot connect to server."));
        } finally {
//            echoService.shutdown();
//            executorService.shutdown();
            disconnect();
//            System.exit(1);
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkServerStatus() {
        try {
            while (true) {
                if (!socket.isConnected()) {
                    break;
                }
                else {
                    Thread.sleep(400);
                }
            }
        } catch (InterruptedException e) {
            showAlert("Connection Error", "Error", "Server is unreachable.");
            disconnect();
            System.exit(2);
        }
    }

    public static void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}