package com.github.kilianB.examples.googleImageMatcher;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

import com.github.kilianB.dataStrorage.tree.BinaryTree;
import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.examples.util.GoogleImageDownloaderMinimal;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTextField;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class GuiController {

	@FXML
	private JFXButton downloadImageBtn;

	@FXML
	private JFXSlider hammingDistance;

	@FXML
	private JFXTextField googleQuery;

	@FXML
	private JFXMasonryPane masonaryPane;

	@FXML
	private JFXComboBox<String> alogorithmCombobox;

	@FXML
	private VBox scrollPaneRoot;

	@FXML
	private Label imageId;

	@FXML
	private Label matchCount;

	@FXML
	private ImageView hashImage;

	@FXML
	private StackPane spinnerPane;

	private BufferedImage currentImage;

	// Prepare a data structure to easily compare hashes

	private HashingAlgorithm aHash = new AverageHash(64);
	private HashingAlgorithm dHash = new DifferenceHash(32, Precision.Double);
	private HashingAlgorithm pHash = new PerceptiveHash(64);

	private BinaryTree<Image> aBinTree = new BinaryTree<>(true);
	private BinaryTree<Image> dBinTree = new BinaryTree<>(true);
	private BinaryTree<Image> pBinTree = new BinaryTree<>(true);

	// Choose one of the many hashing algorithms
	HashingAlgorithm algo = new PerceptiveHash(32);

	@FXML
	private void initialize() {

//		temporaryImageFolder = Files.createTempDirectory("jImageHashFolder").toFile();
//		FileUtils.forceDeleteOnExit(temporaryImageFolder);
		
		
		ObservableList<String> algorithmToggle = FXCollections.observableArrayList(
				"Average Hash","Difference Hash","Perceptive Hash");
		alogorithmCombobox.setItems(algorithmToggle);
		
		alogorithmCombobox.getSelectionModel().select(2);
		
		alogorithmCombobox.valueProperty().addListener((o, old,newValue)->{
			if(currentImage != null) {
				updateMasonaryPane(currentImage);
			}
		});
		
		downloadImageBtn.setOnAction((event)->{
			
			String query = googleQuery.getText();
			
			if(!query.isEmpty()) {
				new Thread( ()->{
					
					spinnerPane.setVisible(true);
					
					ArrayList<BufferedImage> newImages = GoogleImageDownloaderMinimal.downloadThumbnailImagesInMemory(query);
					
					spinnerPane.setVisible(false);
					
					addedImages.addAll(newImages);
					//Done load images
					Platform.runLater(
							() ->{
						newImages.stream().forEach(image -> addImages(image));
					});
					newImages.stream().forEach(image -> computeHash(image));					
				}).start();
			}
		});
		
		hammingDistance.setOnMouseReleased( (event)->{
					updateMasonaryPane(currentImage);
		});
		
		
		
	}

	private void updateMasonaryPane(BufferedImage image) {
		currentImage = image;
		// Clear pane
		ObservableList<Node> children = masonaryPane.getChildren();
		children.clear();

		Hash needleHash = null;
		BinaryTree<Image> binTree = null;

		switch (alogorithmCombobox.getSelectionModel().getSelectedIndex()) {
			case 0:
				needleHash = aHash.hash(image);
				binTree = aBinTree;
				break;
			case 1:
				needleHash = dHash.hash(image);
				binTree = dBinTree;
				break;
			case 2:
				needleHash = pHash.hash(image);
				binTree = pBinTree;
				break;
		}

		PriorityQueue<Result<Image>> results = binTree.getElementsWithinHammingDistance(needleHash,
				(int) hammingDistance.getValue());

		for (Result<Image> result : results) {
			Image javaFxImage = result.getValue();
			ImageView view = new ImageView(javaFxImage);
			// view.setFitWidth(100);
			// view.setFitHeight(100);
			// They have to be wrapped or the masonary pane will not layout them correctly
			view.setOnMouseClicked((event) -> {
				imageId.setText(javaFxImage.toString());
			});

			children.add(new VBox(view, new Label("Distance: " + result.distance)));
		}

		hashImage.setImage(SwingFXUtils.toFXImage(needleHash.toImage(10), null));
		matchCount.setText("Matched: " + children.size());
		masonaryPane.layout();

	}

	HashSet<BufferedImage> addedImages = new HashSet<BufferedImage>();

	private void computeHash(BufferedImage image) {

		Image javafxImage = SwingFXUtils.toFXImage(image, null);

		aBinTree.addHash(aHash.hash(image), javafxImage);
		dBinTree.addHash(dHash.hash(image), javafxImage);
		pBinTree.addHash(pHash.hash(image), javafxImage);
	}

	private void addImages(BufferedImage image) {
		ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
		imageView.setFitWidth(248);
		// imageView.setFitHeight(250);
		imageView.setPreserveRatio(false);

		imageView.setOnMouseClicked((event) -> {
			updateMasonaryPane(image);
		});

		scrollPaneRoot.getChildren().add(imageView);
	}

}
