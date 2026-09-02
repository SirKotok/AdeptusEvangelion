package eva.evangelion;

import eva.evangelion.view.MainMenu;
import javafx.application.Application;
import javafx.stage.Stage;


import java.io.IOException;


public class EvaApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        MainMenu manager = new MainMenu();
        stage = manager.getMainStage();
        stage.setTitle("Adeptus Evangelion");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);

    }



}

