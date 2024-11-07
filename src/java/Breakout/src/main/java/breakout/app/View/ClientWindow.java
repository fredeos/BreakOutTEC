package breakout.app.View;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.List;

import breakout.app.network.ClientPlayer;
import breakout.app.Structures.Pair;
import breakout.app.Controller.SessionController;
import breakout.app.GameObjects.Brick;

public class ClientWindow {

    private List<Button> bricks = new ArrayList<>();
    private Rectangle paddle; // Raqueta
    private List< Pair<Integer,Circle> > balls = new ArrayList<>(); // Lista de bolas

    private SessionController client_session;
    private String client_name;
    private String client_id;
    private int[] selected_block = {0,0};
    
    public ClientWindow(ClientPlayer client){
        client.setClientWindow(this);
        this.client_id = client.identifier;
        this.client_name = client.username;
    }

    public synchronized void setSession(SessionController session){
        this.client_session = session;
        this.createOnScreenElements();
    }

    public synchronized void createOnScreenElements(){
        
    }

    public synchronized void updateOnScreenElements(){

    }

    public synchronized void close(){

    }

    private synchronized void setSelectedBlock(int i, int j){
        this.selected_block[0] = i;
        this.selected_block[1] = j;
    }

    public void display() {
        Stage window = new Stage();
        window.setTitle("Ventana del Cliente["+this.client_id+"]:>>"+this.client_name);
        window.initModality(Modality.APPLICATION_MODAL);

        // Crear el área de juego
        Pane gameArea = new Pane();
        gameArea.setPrefSize(600, 400); // Tamaño del área de juego
        gameArea.setStyle("-fx-background-color: lightgray;");

        // Creación de los ladrillos
        for (int row = 0; row < 4; row++) {
            String color = "red";
            if (row == 1){
                color = "orange";
            } else if (row == 2){
                color = "yellow";
            } else if (row == 3){
                color = "green";
            }
            for (int col = 0; col < 8; col++) {
                Button brickButton = createBrickButton("", color, row, col);
                brickButton.setLayoutX(10 + col * 70);
                brickButton.setLayoutY(10 + row * 35);
                gameArea.getChildren().add(brickButton);
            }
        }

        // HBox para los botones de poder
        HBox powerButtonsLayout = new HBox(15);
        powerButtonsLayout.setPadding(new Insets(10));
        powerButtonsLayout.setAlignment(Pos.CENTER);

        // Entrada de texto para valores de poder
        TextField valueField = new TextField();
        valueField.setEditable(false);
        valueField.setText("1");

        // Botón de poder 1
        Button addBallButton = new Button("BOLA++");
        addBallButton.setOnAction(e -> {
            System.out.println("Bola agregada!");
            String number = valueField.getText();
            int value = Integer.parseInt(number);
            this.client_session.setBrickPowerUp("additional-balls", this.selected_block[0], this.selected_block[1], value);
        });
        powerButtonsLayout.getChildren().add(addBallButton);

        // Botón de poder 2
        Button addLifeButton = new Button("LIFE++");
        addLifeButton.setOnAction(e -> {
            System.out.println("Vida agregada!");
            String number = valueField.getText();
            int value = Integer.parseInt(number);
            this.client_session.setBrickPowerUp("additional-life", this.selected_block[0], this.selected_block[1], value);
        });
        powerButtonsLayout.getChildren().add(addLifeButton);

        // Botón de poder 3
        Button moreBallSpeedButton = new Button("SPD++");
        moreBallSpeedButton.setOnAction(e -> {
            System.out.println("Bonus de velocidad agregado!");
            String number = valueField.getText();
            int value = Integer.parseInt(number);
            this.client_session.setBrickPowerUp("ball-speed", this.selected_block[0], this.selected_block[1], value);
        });
        powerButtonsLayout.getChildren().add(moreBallSpeedButton);

        // Botón de poder 4
        Button moreRacketSize = new Button("SIZE++");
        moreRacketSize.setOnAction(e -> {
            System.out.println("Bola agregada!");
            String number = valueField.getText();
            int value = Integer.parseInt(number);
            this.client_session.setBrickPowerUp("racket-size", this.selected_block[0], this.selected_block[1], value);
        });
        powerButtonsLayout.getChildren().add(moreRacketSize);

        // Boton de decremento
        Button decreaseButton = new Button("-");
        decreaseButton.setOnAction(e -> {
            String number = valueField.getText();
            int value = Integer.parseInt(number);
            value --;
            number = Integer.toString(value);
            valueField.setText(number);
        });

        // Boton de incremento
        Button increaseButton = new Button("+");
        increaseButton.setOnAction(e -> {
            String number = valueField.getText();
            int value = Integer.parseInt(number);
            value ++;
            number = Integer.toString(value);
            valueField.setText(number);
        });

        powerButtonsLayout.getChildren().add(decreaseButton);
        powerButtonsLayout.getChildren().add(valueField);
        powerButtonsLayout.getChildren().add(increaseButton);

        // Layout principal
        VBox mainLayout = new VBox(15, gameArea, powerButtonsLayout);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 600, 600);
        window.setScene(scene);

        window.showAndWait();
    }

    private Button createBrickButton(String name, String color, int i, int j) {
        Button brickButton = new Button(name);
        brickButton.setStyle("-fx-background-color: " + color + "; -fx-min-width: 60px; -fx-min-height: 30px;");
        brickButton.setOnAction(event -> {
            this.setSelectedBlock(i, j);
            brickButton.setStyle("-fx-background-color: grey; -fx-min-width: 60px; -fx-min-height: 30px;");
            System.out.println(i+","+j);
        });
        bricks.add(brickButton);
        return brickButton;
    }

    private Circle createBall() {
        Circle ball = new Circle(10, Color.BLUE);
        ball.setLayoutX(paddle.getLayoutX() + paddle.getWidth() / 2); // Centra la bola sobre la raqueta
        ball.setLayoutY(paddle.getLayoutY() - 15);
        return ball;
    }
    
}