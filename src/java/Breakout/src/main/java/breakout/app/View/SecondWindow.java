package breakout.app.View;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.List;

public class SecondWindow {
    private List<Button> bricks = new ArrayList<>();
    private Rectangle paddle; // Raqueta
    private List<Circle> balls = new ArrayList<>(); // Lista de bolas
    

    public void display() {
        Stage window = new Stage();
        window.setTitle("Ventana Secundaria");
        window.initModality(Modality.APPLICATION_MODAL);

        // Crear el área de juego
        Pane gameArea = new Pane();
        gameArea.setPrefSize(600, 400); // Tamaño del área de juego
        gameArea.setStyle("-fx-background-color: lightgray;");

        // Creación de los ladrillos
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 8; col++) {
                Button greenButton = createBrickButton("", "green");
                greenButton.setStyle("-fx-background-color: green; -fx-min-width: 60px; -fx-min-height: 1px;");
                greenButton.setLayoutX(10 + col * 70);
                greenButton.setLayoutY(10 + row * 35);
                gameArea.getChildren().add(greenButton);
            }
            for (int col = 0; col < 8; col++) {
                Button yellowButton = createBrickButton("", "yellow");
                yellowButton.setStyle("-fx-background-color: yellow; -fx-min-width: 60px; -fx-min-height: 1px;");
                yellowButton.setLayoutX(10 + col * 70);
                yellowButton.setLayoutY(80 + row * 35);
                gameArea.getChildren().add(yellowButton);
            }
            for (int col = 0; col < 8; col++) {
                Button orangeButton = createBrickButton("", "orange");
                orangeButton.setStyle("-fx-background-color: orange; -fx-min-width: 60px; -fx-min-height: 1px;");
                orangeButton.setLayoutX(10 + col * 70);
                orangeButton.setLayoutY(150 + row * 35);
                gameArea.getChildren().add(orangeButton);
            }
            for (int col = 0; col < 8; col++) {
                Button redButton = createBrickButton("", "red");
                redButton.setStyle("-fx-background-color: red; -fx-min-width: 60px; -fx-min-height: 1px;");
                redButton.setLayoutX(10 + col * 70);
                redButton.setLayoutY(220 + row * 35);
                gameArea.getChildren().add(redButton);
            }
        }

        // Crear la raqueta
        paddle = new Rectangle(100, 15);
        paddle.setFill(Color.BLACK);
        paddle.setLayoutX(250); // Posición inicial en el eje X
        paddle.setLayoutY(350); // Posición en el eje Y
        gameArea.getChildren().add(paddle);

        // Crear la bola inicial
        Circle initialBall = createBall();
        balls.add(initialBall);
        gameArea.getChildren().add(initialBall);

        // HBox para los botones de poder
        HBox powerButtonsLayout = new HBox(15);
        powerButtonsLayout.setPadding(new Insets(10));
        powerButtonsLayout.setAlignment(Pos.CENTER);

        // Botón de poder1
        Button addBallButton = new Button("Pwr 1");
        addBallButton.setOnAction(e -> {
            System.out.println("Botón de poder 1");
        });
        powerButtonsLayout.getChildren().add(addBallButton);

        // Otros botones de poder 
        for (int i = 2; i <= 4; i++) {
            Button powerButton = new Button("Pwr " + i);
            powerButton.setOnAction(e -> System.out.println("Botón " + powerButton.getText() + " presionado"));
            powerButtonsLayout.getChildren().add(powerButton);
        }

        // Layout principal
        VBox mainLayout = new VBox(15, gameArea, powerButtonsLayout);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 600, 600);
        window.setScene(scene);

        window.showAndWait();
    }

    private Button createBrickButton(String name, String color) {
        Button brickButton = new Button(name);
        brickButton.setStyle("-fx-background-color: " + color + "; -fx-min-width: 60px; -fx-min-height: 30px;");
        brickButton.setOnAction(e -> System.out.println("Botón de color " + name + " presionado"));
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