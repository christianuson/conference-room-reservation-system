import controller.LoginController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Application;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("=== Conference Room Reservation Application ===");

        // Load FXML and controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
        Parent root = loader.load();
        LoginController login = loader.getController();
//wa
        // Show UI
        primaryStage.setTitle("Conference Room Reservation Login");
        primaryStage.setScene(new Scene(root, 540, 570));
        primaryStage.show();

        System.out.println("Login screen loaded...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
