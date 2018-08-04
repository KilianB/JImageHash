package com.github.kilianB.examples.googleImageMatcher;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.PriorityQueue;

import com.github.kilianB.dataStrorage.tree.BinaryTree;
import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.Hash;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FindGoogleDuplicatesInMemory extends Application{

	
	
	public FindGoogleDuplicatesInMemory() {
//		
//		//Create a subdirectory imagesJImageHash in the current folder
//		// CAUTION: all files! currently present in the imageDirectory wil be deleted.
//		File imageDirectory = new File("imagesJImageHash");
//		
//		/*
//		 * Download the first 100 images found on the google image. for each query
//		 */
//		//downloadImagesFromGoogle(imageDirectory,"ballon","candle","tree");
//		
//		//Prepare a data structure to easily compare hashes  
//		BinaryTree<String> binTree = new BinaryTree<>(true);
//		
//		//Choose one of the many hashing algorithms
//		HashingAlgorithm algo = new PerceptiveHash(32);
//		
//		long startHashing = System.currentTimeMillis();
//		
//		ArrayList<Hash> storedHashes = new ArrayList<Hash>();
//	
//		File[] imageFiles = imageDirectory.listFiles((File dir, String fileName) -> {return fileName.endsWith(".jpg");});
////		//Binary tree is not thread safe. So work non concurrently.
//		for(File imageFile : imageFiles) {
//			try {
//				Hash imageHash = algo.hash(imageFile);
//				//Save the name as a reference in the tree
//				storedHashes.add(imageHash);
//				binTree.addHash(imageHash, imageFile.getName());
//			} catch (IOException e) {
//				System.out.println("Error loading the image: " + e.getMessage());
//			}
//		}
//		
//		System.out.println("Finished hashing and adding " + imageFiles.length + " images to tree: " +  df.format((System.currentTimeMillis() - startHashing)/1000d) + "s elapsed");
//
//		
//		//Retrieve a random hash 
//		Hash randomHash = storedHashes.get((int)Math.random() * imageFiles.length);
//		
//		PriorityQueue<Result<String>> results =  binTree.getElementsWithinHemmingDistance(randomHash, 5);
//		
//		System.out.println(randomHash);
//		for(Result<String> result : results) {
//			System.out.println(result);
//		}
	}
	
	private DecimalFormat df = new DecimalFormat("0.000");
	
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
