package breakout.app.View;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

import breakout.app.network.*;

public class MainWindow extends Application {
    public Server server = null;
    // Variables para IP y puerto
    private String ip_example = "192.168.1.1"; // Valor de ejemplo para IP
    private String port_example = "8080"; // Valor de ejemplo para Puerto
    private boolean activated = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ventana Principal");

        // Labels para IP y puerto
        Label ipLabel = new Label("IP:");
        ipLabel.setStyle("-fx-font-weight: bold;-fx-font-size: 14px;"); 
        
        TextField ipField = new TextField();
        ipField.setPromptText(ip_example);
        ipField.setPrefWidth(120.0);


        Label portLabel = new Label("Puerto:");
        portLabel.setStyle("-fx-font-weight: bold;-fx-font-size: 14px;"); 

        TextField portField = new TextField();
        portField.setPromptText(port_example);
        portField.setPrefWidth(120.0);
        

        // Botón ON/OFF
        Button activeButton = new Button("Encender");
        activeButton.setPrefWidth(80); // Ancho ajustado para el texto
        activeButton.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); // Aumentar tamaño y estilo de texto
        activeButton.setOnAction(event -> {
            if (!activated){
                String port = portField.getText();
                String ip = ipField.getText();
                if (port.equals("")){
                    port = portField.getPromptText();
                }
                if (ip.equals("")){
                    ip = ipField.getPromptText();
                }
                int port_num = Integer.parseInt(port.trim());
                if (this.server == null){
                    try {
                        this.server = new Server(port_num, ip);
                        this.server.turnON();
                    } catch (IOException e1) {
                        System.out.println(e1);
                        this.server = null;
                    }
                } else {
                    this.server.turnON();
                }
                this.startUpdaterThread();
                activeButton.setText("Apagar");
            } else {
                this.server.turnOFF();
                activeButton.setText("Encender");
            }
        });

        // HBox para IP y Puerto
        VBox ipPortLabels = new VBox(10,ipLabel,portLabel);
            ipPortLabels.setAlignment(Pos.CENTER_LEFT);
        VBox ipPortFields = new VBox(10,ipField,portField);
            ipPortFields.setAlignment(Pos.CENTER_LEFT);
        HBox ipPortLayout = new HBox(20, ipPortLabels, ipPortFields); // Más espacio entre IP y Puerto
        ipPortLayout.setAlignment(Pos.CENTER_LEFT);

        // HBox superior que contiene ipPortLayout y el botón a la derecha
        HBox topLayout = new HBox(10, ipPortLayout, activeButton);
        topLayout.setPadding(new Insets(10));
        topLayout.setAlignment(Pos.CENTER_LEFT); // Alinear contenido a la izquierda
        HBox.setHgrow(ipPortLayout, Priority.ALWAYS); // Hacer que ipPortLayout ocupe el espacio disponible


        // TabPane para "Incoming" y "Guests"
        TabPane tabPane = new TabPane();
        Tab incomingTab = new Tab("Incoming", createIncomingLayout());
        Tab guestsTab = new Tab("Guests", createGuestsLayout());
        tabPane.getTabs().addAll(incomingTab, guestsTab);

        // Layout principal
        VBox mainLayout = new VBox(5, topLayout, tabPane);
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
            if (this.server != null){
                if (this.server.pending.size > 0){
                    Client client = (Client) this.server.pending.get(0);
                    System.out.println("\nCliente["+client.identifier+":"+client.username+"} aceptado");
                    this.server.approveClient(0);
                    System.out.println("\nMensaje preparado: "+ client.checkOutput());
                    if (client.type.equals("player")){
                        ClientWindow SecondWindow= new ClientWindow((ClientPlayer)client);
                        SecondWindow.display();
                    }
                }
            }
        });

        rejectButton.setOnAction(event -> {
            if (this.server != null){
                try {
                    this.server.traffic_lock.acquire();
                    if (this.server.pending.size > 0){
                        System.out.println("Cliente rechazado");
                        this.server.rejectClient(0);
                    }
                    this.server.traffic_lock.release();
                } catch (InterruptedException e1) {
                    System.err.println("Mutex interrupted error:\n"+e1);
                }
            }
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

    public void startUpdaterThread(){
        Thread updater = new Thread(()->{

        });
        updater.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}