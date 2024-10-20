package com.betterguess.desktopdemo;

import com.betterguess.desktopdemo.TextEditor;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        TextEditor textEditor = new TextEditor();

        Scene scene = new Scene(textEditor.getRoot(), 800, 600);
        primaryStage.setTitle("Word Prediction Text Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}