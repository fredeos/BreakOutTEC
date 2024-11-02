package breakout.App;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainWindow extends Application {
    // Variables para IP y puerto
    private String ipValue = "192.168.1.1"; // Valor de ejemplo para IP
    private String portValue = "8080"; // Valor de ejemplo para Puerto

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ventana Principal");

        // Sección superior con nombre
        Label nameLabel = new Label("Nombre:");
        nameLabel.setStyle("-fx-font-weight: bold;-fx-font-size: 14px;");
        HBox nameLayout = new HBox(nameLabel);
        nameLayout.setAlignment(Pos.CENTER_LEFT);
        nameLayout.setPadding(new Insets(10, 0, 0, 0)); // Más espacio superior

        // Labels para IP y puerto
        Label ipLabel = new Label("IP:");
        ipLabel.setStyle("-fx-font-weight: bold;-fx-font-size: 14px;"); // Aumentar tamaño de letra de "IP"
        
        
        Label ipValueLabel = new Label(ipValue); // Etiqueta para el valor de la IP
        ipValueLabel.setStyle("-fx-font-size: 14px;"); // Aumentar tamaño de letra del valor de la IP
        
        Label portLabel = new Label("Port:");
        portLabel.setStyle("-fx-font-weight: bold;-fx-font-size: 14px;"); // Aumentar tamaño de letra de "Port"
        
        Label portValueLabel = new Label(portValue); // Etiqueta para el valor del puerto
        portValueLabel.setStyle("-fx-font-size: 14px;"); // Aumentar tamaño de letra del valor del puerto

        // Botón ON/OFF
        Button onOffButton = new Button("ON/OFF");
        onOffButton.setPrefWidth(80); // Ancho ajustado para el texto
        onOffButton.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); // Aumentar tamaño y estilo de texto

        // HBox para IP y Puerto
        HBox ipPortLayout = new HBox(30, ipLabel, ipValueLabel, portLabel, portValueLabel); // Más espacio entre IP y Puerto
        ipPortLayout.setAlignment(Pos.CENTER_LEFT);

        // HBox superior que contiene ipPortLayout y el botón a la derecha
        HBox topLayout = new HBox(10, ipPortLayout, onOffButton);
        topLayout.setPadding(new Insets(10));
        topLayout.setAlignment(Pos.CENTER_LEFT); // Alinear contenido a la izquierda
        HBox.setHgrow(ipPortLayout, Priority.ALWAYS); // Hacer que ipPortLayout ocupe el espacio disponible


        // TabPane para "Incoming" y "Guests"
        TabPane tabPane = new TabPane();
        Tab incomingTab = new Tab("Incoming", createIncomingLayout());
        Tab guestsTab = new Tab("Guests", createGuestsLayout());
        tabPane.getTabs().addAll(incomingTab, guestsTab);

        // Layout principal
        VBox mainLayout = new VBox(5, nameLayout, topLayout, tabPane);
        mainLayout.setPadding(new Insets(10));

        Scene scene = new Scene(mainLayout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createIncomingLayout() {
        // Layout para Incoming con CheckBoxes
        VBox incomingLayout = new VBox(40);
        incomingLayout.setPadding(new Insets(20));

        Button acceptButton = new Button("Aceptar");
        Button rejectButton = new Button("Rechazar");

        // Estilos opcionales para los botones
        acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        rejectButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px;");

        // Acciones para los botones
        acceptButton.setOnAction(event -> {
            System.out.println("Cliente aceptado");
            // Lo que de los sockets
        });

        rejectButton.setOnAction(event -> {
            System.out.println("Cliente rechazado");
            // Lo de los sockets
        });

        incomingLayout.getChildren().addAll(acceptButton, rejectButton);

        return incomingLayout;
    }

    private VBox createGuestsLayout() {
        // Layout para Guests con RadioButtons para seleccionar espectador o jugador
        VBox guestsLayout = new VBox(5);
        guestsLayout.setPadding(new Insets(10));

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton spectatorButton = new RadioButton("Spectator");
        spectatorButton.setToggleGroup(roleGroup);
        RadioButton playerButton = new RadioButton("Player");
        playerButton.setToggleGroup(roleGroup);

        guestsLayout.getChildren().addAll(spectatorButton, playerButton);
        return guestsLayout;
    }

    public static void main(String[] args) {
        launch(args);
    }
}