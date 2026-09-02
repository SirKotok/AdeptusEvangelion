module eva.evangelion {
    requires javafx.fxml;


    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires com.almasb.fxgl.all;
    requires com.fasterxml.jackson.databind;
    requires javafx.media;

    opens eva.evangelion to javafx.fxml;
    exports eva.evangelion;
    exports eva.evangelion.gameboard;
    opens eva.evangelion.gameboard to javafx.fxml;
}