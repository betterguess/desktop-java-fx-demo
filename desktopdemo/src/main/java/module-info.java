module com.betterguess.desktopdemo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;


    opens com.betterguess.desktopdemo to javafx.fxml;
    exports com.betterguess.desktopdemo;
}