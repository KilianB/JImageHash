package com.github.kilianB.examples.googleImageMatcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FindGoogleDuplicatesInMemory extends Application{

	public FindGoogleDuplicatesInMemory() {}
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		Parent root = FXMLLoader.load(getClass().getResource("FindGoogleDuplicatesMemory.fxml"));
		
		Scene scene = new Scene(root,1200,800);
		scene.getStylesheets().add(getClass().getResource("exampleGui.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
}
