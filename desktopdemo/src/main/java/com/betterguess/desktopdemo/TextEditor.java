package com.betterguess.desktopdemo;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.input.MouseEvent;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import com.google.gson.*;

public class TextEditor {

    private BorderPane root;
    private TextArea textArea;
    private ListView<String> suggestionsList;
    private ContextMenu suggestionsPopup;

    private HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public TextEditor() {
        initializeComponents();
        addEventHandlers();
    }

    public BorderPane getRoot() {
        return root;
    }

    private void initializeComponents() {
        root = new BorderPane();
        textArea = new TextArea();
        textArea.setFont(Font.font("Verdana", 14));

        suggestionsList = new ListView<>();
        suggestionsPopup = new ContextMenu();

        suggestionsList.setOnMouseClicked(event -> {
            String selectedSuggestion = suggestionsList.getSelectionModel().getSelectedItem();
            if (selectedSuggestion != null) {
                handleSuggestionSelected(selectedSuggestion);
            }
        });

        root.setCenter(textArea);
    }

    private void addEventHandlers() {
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            handleTextChanged();
        });

        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            // Optional: handle caret movement
        });
    }

    private void handleTextChanged() {
        String text = textArea.getText();
        int caretPosition = textArea.getCaretPosition();
        String prompt = text.substring(0, caretPosition);

        if (!prompt.isEmpty()) {
            fetchSuggestions(prompt);
        } else {
            suggestionsPopup.hide();
        }
    }

    private void fetchSuggestions(String prompt) {
        // Build the request body
        JsonObject json = new JsonObject();
        json.addProperty("locale", "en_US");
        json.addProperty("prompt", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8080/continuations"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        // Send the request asynchronously
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    processSuggestionsResponse(response.body(), getCurrentWord(prompt));
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private void processSuggestionsResponse(String responseBody, String currentWord) {
        JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
        JsonArray continuations = responseJson.getAsJsonArray("continuations");

        Platform.runLater(() -> {
            suggestionsList.getItems().clear();
            for (JsonElement element : continuations) {
                String suggestion = element.getAsString();
                String processedSuggestion = matchCase(suggestion, currentWord);
                suggestionsList.getItems().add(processedSuggestion);
            }
            showSuggestionsPopup();
        });
    }

    private void showSuggestionsPopup() {
        if (suggestionsList.getItems().isEmpty()) {
            suggestionsPopup.hide();
            return;
        }

        suggestionsList.setPrefWidth(200);
        suggestionsList.setPrefHeight(150);

        suggestionsPopup.getItems().clear();
        CustomMenuItem customMenuItem = new CustomMenuItem(suggestionsList, false);
        suggestionsPopup.getItems().add(customMenuItem);

        int caretPosition = textArea.getCaretPosition();
        Point2D caretPoint = textArea.getInputMethodRequests().getTextLocation(caretPosition);

        if (caretPoint != null) {
            Point2D screenPosition = textArea.localToScreen(caretPoint);

            if (screenPosition != null) {
                suggestionsPopup.show(textArea, screenPosition.getX(), screenPosition.getY() + textArea.getFont().getSize());
            } else {
                suggestionsPopup.hide();
            }
        } else {
            suggestionsPopup.hide();
        }
    }


    private void handleSuggestionSelected(String suggestion) {
        String text = textArea.getText();
        int caretPosition = textArea.getCaretPosition();

        String textBeforeCaret = text.substring(0, caretPosition);
        String textAfterCaret = text.substring(caretPosition);

        // Replace the current word with the suggestion
        int wordStart = Math.max(Math.max(textBeforeCaret.lastIndexOf(' '), textBeforeCaret.lastIndexOf('\n')), 0);
        String newText = textBeforeCaret.substring(0, wordStart) + suggestion + " " + textAfterCaret;

        textArea.setText(newText);
        int newCaretPosition = wordStart + suggestion.length() + 1; // +1 for the space

        textArea.positionCaret(newCaretPosition);

        suggestionsPopup.hide();
    }

    private String getCurrentWord(String text) {
        String[] words = text.split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "";
    }

    private String matchCase(String suggestion, String wordToReplace) {
        if (wordToReplace != null && !wordToReplace.isEmpty() && Character.isUpperCase(wordToReplace.charAt(0))) {
            return suggestion.substring(0, 1).toUpperCase() + suggestion.substring(1);
        } else {
            return suggestion.toLowerCase();
        }
    }
}