package GUI;

import Chess.AI.MiniMaxAI;
import Chess.ChessGame;
import Chess.Location;
import Chess.Move;
import Chess.Pieces.*;
import Data.Load;
import Data.Save;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;

import java.io.*;
import javax.swing.*;
import java.util.ArrayList;



import static GUI.GameBoard.GameType.OnePlayer;
import static GUI.GameBoard.GameType.PuzzleMode;
import static GUI.GameBoard.GameType.TwoPlayer;
import static Data.FileConstants.FILE_LOCATOR;


/**
 *
 */
public class GameBoard extends Application {


    private ChessGame game;
    public GameBoard(ChessGame g){
        game = g;
    }
    private int firstClickX = -1;
    private int firstClickY = -1;
    private int secondClickX = -1;
    private int secondClickY = -1;
    private GameType gameType = TwoPlayer ;

    @Override
    public void start(Stage primaryStage) throws Exception {
        for (ChessPiece chessPiece : game.getBoard().getBoardArrayList()) {
            chessPiece.setImage();
        }
    }

    public void setGameType(GameType type) {
        this.gameType = type;
    }

    public enum GameType{
        OnePlayer,
        TwoPlayer,
        PuzzleMode
    }

    public void setBoard (Stage stage) throws Exception {

        BorderPane borderPane = new BorderPane();
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0,0,25,0));

        //set color of tiles
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Rectangle rectangle = new Rectangle(80,80);
                setRectangleColor(rectangle,i,j);
                grid.add(rectangle,i,j);
            }
        }

        //set pieces
        ArrayList<ChessPiece> chessPieces = game.getBoard().getBoardArrayList();
        for (ChessPiece chessPiece : chessPieces) {
            chessPiece.setImage();
            ImageView tmpView = chessPiece.getImage();
            tmpView.setFitHeight(80);
            tmpView.setFitWidth(80);
            grid.add(chessPiece.getImage(), chessPiece.getLocation().x, chessPiece.getLocation().y);
        }

        HBox hBox = new HBox();
        Button backBtn = new Button("< Menu");
        backBtn.setMinHeight(25);
        Button saveBtn = new Button("Save");
        saveBtn.setMinHeight(25);
        Button loadBtn = new Button("Load");
        loadBtn.setMinHeight(25);
        Button undoBtn = new Button("Undo");
        undoBtn.setMinHeight(25);
        Button redoBtn = new Button("Redo");
        redoBtn.setMinHeight(25);
        hBox.getChildren().addAll(backBtn,saveBtn,loadBtn,undoBtn,redoBtn);


        borderPane.setTop(hBox);
        borderPane.setCenter(grid);
        Scene scene = new Scene(borderPane, 640, 665);
        stage.setTitle("Chess Game");
        stage.setScene(scene);
        stage.setMaxWidth(655);
        stage.setMaxHeight(700);
        stage.show();
        System.out.println(game.getBoard().toString());
        //highlight square when clicked
              grid.setOnMouseClicked( e -> {
            int col = (int)Math.floor((e.getSceneX())/ 80); //subtract to adjust for stroke size
            int row = (int)Math.floor((e.getSceneY()-25)/ 80);
            if (firstClickX == -1) {
                Location location = new Location(col, row);
                ChessPiece piece = game.getBoard().getPieceAtLocation(location);

                if (piece != null && piece.getColor().equals(game.getCurrentPlayer())){
                    firstClickX = col;
                    firstClickY = row;

                    highlightTile(grid, e, col, row);
                } else if (piece != null) {
                    displayAlert("Alert Message", "Can't move that piece, it's " + game.getCurrentPlayer().toString() + "'s turn");
                }

            }else{
                secondClickX = col;
                secondClickY = row;
                Location location = new Location(col, row);
                ChessPiece piece = game.getBoard().getPieceAtLocation(location);
                if (piece != null && piece.getColor().equals(game.getCurrentPlayer())) {
                    firstClickX = col;
                    firstClickY = row;
                    highlightTile(grid, e, col, row);
                } else {
                try {
                    Location from = new Location(firstClickX,firstClickY);
                    Location to = new Location(secondClickX,secondClickY);
                    //reset first click
                    firstClickX = -1;
                    firstClickY = -1;
                    //possibly place puzzle if statement here
                    if (gameType == PuzzleMode){
                        if (from.equals(new Location(1,1)) && to.equals(new Location(1,5)) ){
                            game.playMove(from,to);
                            game.playMove(new Location(3,3), new Location(3,4));
                           // repaint();
                            setBoard(stage);
                        } else if (from.equals(new Location(1,5)) && to.equals(new Location(3,5))){
                            game.playMove(from,to);
                            setBoard(stage);
                            displayAlert("Alert Message", "Solved");
                            Menu menu = new Menu();
                            menu.start(stage);
                        }
                        else{
                            displayAlert("Alert Message", "Bad Move");
                            setBoard(stage);
                        }
                    } else {
                        ChessGame previousGame = (ChessGame) game.clone();
                        if (game.playMove(from, to)) {
                            StatsPage.stats.updateFromGameState(previousGame, game, false);
                            System.out.println(game.getBoard().toString());
                            Save.autoSave(game);
                            game.incMoveCount();
                            Replay.clearRedo();
                            repaint();
                            boolean isEndOfGame = game.getAllValidMoves(game.getCurrentPlayer()).size() == 0;
                            if (gameType == OnePlayer && !isEndOfGame) {
                                MiniMaxAI miniMaxAI = new MiniMaxAI(game);
                                Move aiMove = miniMaxAI.getNextMove();
                                game.playMove(aiMove);
                                Save.autoSave(game);
                                game.incMoveCount();
                                Replay.clearRedo();
                                repaint();
                            } else if (isEndOfGame) {
                                repaint();
                                JOptionPane.showMessageDialog(null, game.getState().toString());
                                Save.clearAutoSave();
                                System.out.println(game.getState().toString());
                            } if (game.getState() == ChessGame.GameState.PLAY) {
                                setBoard(stage);
                            } else {
                                Menu menu = new Menu();
                                menu.start(stage);
                            }
                        } else {
                            if (game.isColorInCheck(game.getCurrentPlayer())) {
                                displayAlert("Alert Message", "Cannot move there, king is still in check");
                            } else {
                                displayAlert("Alert Message", "Invalid move!");
                            }
                            setBoard(stage);
                        }
                    }
                } catch (Exception e1) {
                        e1.printStackTrace();

                    }
                }}


            });
        backBtn.setOnAction(e -> {
            Menu menu = new Menu();
            try {
                menu.start(stage);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        saveBtn.setOnAction(e -> {
            try {
                Save.save("AutoSave","save");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        loadBtn.setOnAction(e -> {
            game = Load.Load("save", game);
            try {
                setBoard(stage);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        undoBtn.setOnAction((ActionEvent e) -> {
            ChessGame previousGame = (ChessGame) game.clone();
            StatsPage.stats.updateFromGameState(previousGame, game, true);
            if(game.getMoveCount() >= 2) {
                game.setMoveCount(game.getMoveCount() - 2);
                game = Replay.undoMove(game.getMoveCount(), game);
            }
            if(game.getMoveCount() < 2) {
                boolean players = game.getIsTwoPlayer();
                game = new ChessGame(players);
                game.setMoveCount(0);
                Save.clearAutoSave();
            }
            try {
                setBoard(stage);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        redoBtn.setOnAction((ActionEvent e) -> {
            ChessGame previousGame = (ChessGame) game.clone();
            StatsPage.stats.updateFromGameState(previousGame, game, false);
            File redoFile = new File(FILE_LOCATOR.toString() + "/resources/main/redo.txt");

            try {
                InputStream inputRedo = new FileInputStream(redoFile);
                String resultStr = "";
                int bytesRead;
                while((bytesRead = inputRedo.read(new byte[1024])) > 0) {
                    resultStr = resultStr + bytesRead;
                }

                if(!resultStr.equals("")) {
                    game = Replay.redoMove(game.getMoveCount(),game);
                }
                setBoard(stage);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
    }

    private void highlightTile(GridPane grid, MouseEvent e, int col, int row) {

        //set color of tiles
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Rectangle rectangle = new Rectangle(80,80);
                setRectangleColor(rectangle,i,j);
                grid.add(rectangle,i,j);
            }
        }

        //set pieces
        ArrayList<ChessPiece> chessPieces = game.getBoard().getBoardArrayList();
        for (ChessPiece chessPiece : chessPieces) {
            chessPiece.setImage();
            ImageView tmpView = chessPiece.getImage();
            tmpView.setFitHeight(80);
            tmpView.setFitWidth(80);
            grid.add(chessPiece.getImage(), chessPiece.getLocation().x, chessPiece.getLocation().y);
        }

        Rectangle rectangle = new Rectangle(80, 80);
        rectangle.setFill(Color.YELLOW);
        rectangle.setOpacity(.5);
        if (e.getSceneX() < 640 && e.getSceneY() < 665) {
            grid.add(rectangle, col, row);
        }
    }

    private void repaint() {
        System.out.println(game.getBoard());
    }

    private void setRectangleColor(Rectangle rectangle, int col, int row){

        if((col % 2 == 0) ^ (row % 2 == 0)){
            //set to green if col or row is even
            rectangle.setFill(Color.GREEN);
        }else{
            //set to tan
            rectangle.setFill(Color.TAN);
        }
    }


    public void setGame(ChessGame game){
        this.game = game;
    }

    public static void displayAlert(String title, String message) {
        Stage window = new Stage();

        //Block events to other windows
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setMinWidth(270);
        window.setMinHeight(100);


        Label label = new Label();
        label.setText(message);
        Button closeButton = new Button("Ok");
        closeButton.setMinWidth(75);
        closeButton.setOnAction(e -> window.close());

        VBox layout = new VBox(30);
        layout.setPadding(new Insets(25,0,25,0));

        layout.getChildren().addAll(label, closeButton);
        layout.setAlignment(Pos.CENTER);

        //Display window and wait for it to be closed before returning
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }
}
