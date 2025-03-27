package com.healthcare.aarogyanidaan;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.healthcare.aarogyanidaan.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class chatbot extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int GALLERY_REQUEST_CODE = 103;
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton, backbutton;
    private ScrollView scrollView;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;

    // Health knowledge base
    private Map<String, List<String>> healthResponses;
    private List<String> generalGreetings;
    private List<String> unknownResponses;
    private List<String> symptomQuestions;
    private Map<String, List<String>> medicalConditions;
    private ImageButton camera, upload;

    private String currentPhotoPath;
    private ChatDatabaseHelper dbHelper;

    // Chatbot state variables
    private boolean inHealthConsultation = false;
    private String currentSymptom = null;
    private List<String> reportedSymptoms = new ArrayList<>();
    private int consultationStage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        checkPermissions();

        // Initialize UI components
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backbutton = findViewById(R.id.backbutton);

        // Setup RecyclerView
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        camera = findViewById(R.id.camera);
        upload = findViewById(R.id.upload);

        // Initialize the database helper
        dbHelper = new ChatDatabaseHelper(this);

        // Load previous messages
        loadChatHistory();

        camera.setOnClickListener(v -> openCamera());
        upload.setOnClickListener(v -> openGallery());

        // Initialize knowledge bases
        initializeKnowledgeBase();
        addDateMessage();

        // Send welcome message
//        addBotMessage("Hello! I'm Aarogya Assist, your personal health assistant. I was created by Nayan Pote to help you with health-related questions and general discussions. How can I help you today?");

        // Setup send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString().trim();
                if (!message.isEmpty()) {
                    sendMessage(message);
                }
            }
        });
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(this::showPopupMenu);
        backbutton.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });
    }

    private void loadChatHistory() {
        chatMessages.clear(); // Clear existing chat list

        // Load all messages from the database
        List<ChatMessage> messages = dbHelper.getAllMessages();

        //  Get today's date in the correct format
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        boolean todayDateExists = dbHelper.todayDateSeparatorExists(todayDate);

        //  If no messages or no date separator for today exists, add one
        if (messages.isEmpty() || !todayDateExists) {
            ChatMessage dateMessage = new ChatMessage("", todayDate, "");
            dateMessage.sentBy = ChatMessage.DATE_SEPARATOR;

            //  Save to database only if it doesn't exist
            if (!todayDateExists) {
                dbHelper.addDateSeparator(dateMessage);
            }
        }

        //  Add all messages from database to our chat list (including date separator)
        chatMessages.addAll(messages);
        chatAdapter.notifyDataSetChanged();

        //  Check if there are any non-separator messages
        boolean onlyDateSeparator = true;
        for (ChatMessage msg : chatMessages) {
            if (msg.sentBy != ChatMessage.DATE_SEPARATOR) {
                onlyDateSeparator = false;
                break;
            }
        }

        if (onlyDateSeparator) {
            //  Send welcome message if only date separator exists
            addBotMessage("Hello! I'm Aarogya Assist, your personal health assistant. " +
                    "I was created by Nayan Pote to help you with health-related questions and general discussions. " +
                    "How can I help you today?");
        } else {
            //  Scroll to the latest message if messages exist
            chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(chatMessages.size() - 1));
        }
    }

    // Update the addUserMessage method to save to database
    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage("user", message, getCurrentTime());
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();

        // Save to database
        dbHelper.addMessage(chatMessage);
    }

    // Update the addBotMessage method to save to database
    private void addBotMessage(String message) {
        // Simulate typing delay
        sendButton.setEnabled(false);

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ChatMessage chatMessage = new ChatMessage("bot", message, getCurrentTime());
                chatMessages.add(chatMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                scrollToBottom();
                sendButton.setEnabled(true);

                // Save to database
                dbHelper.addMessage(chatMessage);
            }
        }, 500); // 500ms delay to simulate typing
    }


    private void addDateMessage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateString = dateFormat.format(new Date());

        // Create a date separator message
        ChatMessage dateMessage = new ChatMessage("", dateString, "");
        dateMessage.sentBy = ChatMessage.DATE_SEPARATOR;

        // Add to chat list and notify adapter
        chatMessages.add(dateMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);

        // Save to database with special type
        dbHelper.addDateSeparator(dateMessage);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.healthcare.aarogyanidaan.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this
                    , new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                sendMediaMessage(currentPhotoPath, "📸 Photo");
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                Uri selectedImage = data.getData();
                String imagePath = getPathFromUri(selectedImage);
                if (imagePath != null) {
                    sendMediaMessage(imagePath, "🖼️ Image");
                }
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
    }

    private void sendMediaMessage(String mediaPath, String caption) {
        ChatMessage chatMessage = new ChatMessage("user", caption, getCurrentTime(), mediaPath);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();

        // Save to database
        dbHelper.addMessage(chatMessage);

        // Generate a response for media
        String response = "I received your " + (caption.contains("Photo") ? "photo" : "image") +
                ". Would you like to discuss anything about it?";

        // Add bot response
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ChatMessage botMessage = new ChatMessage("bot", response, getCurrentTime());
                chatMessages.add(botMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                scrollToBottom();

                // Save to database
                dbHelper.addMessage(botMessage);
            }
        }, 500);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.top_app_bar, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_clear_chat) {
                showClearChatConfirmation();
                return true;
            }
            else if (id == R.id.menu_settings) {
                openSettings();
                return true;
            }
            else if (id == R.id.menu_help) {
                showHelpDialog();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void openSettings() {
        Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chatbot Assistant")
                .setIcon(R.drawable.chatbot)
                .setMessage("This chatbot can help with:\n\n" +
                        "• 💡 Answering general questions\n\n" +
                        "• 💬 Having friendly conversations\n\n" +
                        "• 🧠 Providing basic health information\n\n" +
                        "• 🌟 Offering daily wellness tips\n\n" +
                        "Try asking about exercise, diet, sleep, or stress management.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showClearChatConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat History")
                .setMessage("Are you sure you want to clear all chat history? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    // Clear the database
                    dbHelper.clearAllMessages();

                    // Clear the list
                    chatMessages.clear();
                    chatAdapter.notifyDataSetChanged();

                    // Add welcome message again
                    addDateMessage();
                    addBotMessage("Hello! I'm Aarogya Assist, your personal health assistant. I was created by Nayan Pote to help you with health-related questions and general discussions. How can I help you today?");

                    Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initializeKnowledgeBase() {
        // Initialize health responses
        healthResponses = new HashMap<>();

        // Greetings
        generalGreetings = new ArrayList<>();
        generalGreetings.add("Hello! How can I assist you with your health today?");
        generalGreetings.add("Hi there! I'm Aarogya Assist. What health questions do you have?");
        generalGreetings.add("Greetings! I'm here to help with any health concerns or questions.");
        generalGreetings.add("Welcome! How can I help you stay healthy today?");

        // Unknown responses
        unknownResponses = new ArrayList<>();
        unknownResponses.add("I'm not sure I understand. Could you rephrase that?");
        unknownResponses.add("I'm still learning. Can you ask that in a different way?");
        unknownResponses.add("I don't have information on that yet. Can I help with something else?");
        unknownResponses.add("I'm not programmed to understand that query. Could you try another question?");

        // Common health topics
        List<String> exerciseResponses = new ArrayList<>();
        exerciseResponses.add("Regular exercise is crucial for maintaining good health. Aim for at least 150 minutes of moderate activity per week.");
        exerciseResponses.add("Exercise benefits include improved cardiovascular health, better mood, and reduced risk of chronic diseases.");
        exerciseResponses.add("Good exercises for beginners include walking, swimming, and cycling. Always start slow and gradually increase intensity.");
        healthResponses.put("exercise", exerciseResponses);

        List<String> nutritionResponses = new ArrayList<>();
        nutritionResponses.add("A balanced diet should include fruits, vegetables, whole grains, lean proteins, and healthy fats.");
        nutritionResponses.add("Try to limit processed foods, excessive sugar, and salt in your diet for better health outcomes.");
        nutritionResponses.add("Staying hydrated is important. Aim to drink about 8 glasses of water daily, but needs vary by individual.");
        healthResponses.put("nutrition", nutritionResponses);

        List<String> sleepResponses = new ArrayList<>();
        sleepResponses.add("Adults should aim for 7-9 hours of quality sleep per night for optimal health.");
        sleepResponses.add("Poor sleep can impact immune function, metabolism, and mental health.");
        sleepResponses.add("Improving sleep hygiene includes maintaining a regular sleep schedule and creating a restful environment.");
        healthResponses.put("sleep", sleepResponses);

        List<String> stressResponses = new ArrayList<>();
        stressResponses.add("Chronic stress can negatively impact both physical and mental health.");
        stressResponses.add("Stress management techniques include meditation, deep breathing, physical activity, and maintaining social connections.");
        stressResponses.add("If stress is severely impacting your daily life, consider speaking with a healthcare professional.");
        healthResponses.put("stress", stressResponses);

        // Symptom questions for consultation
        symptomQuestions = new ArrayList<>();
        symptomQuestions.add("How long have you been experiencing these symptoms?");
        symptomQuestions.add("On a scale of 1-10, how would you rate any pain associated with this?");
        symptomQuestions.add("Have you noticed any triggers that worsen your symptoms?");
        symptomQuestions.add("Are you currently taking any medications?");
        symptomQuestions.add("Have you made any recent lifestyle changes?");

        // Common medical conditions info
        medicalConditions = new HashMap<>();

        List<String> feverInfo = new ArrayList<>();
        feverInfo.add("Fever is usually a sign that your body is fighting an infection. Rest, stay hydrated, and take appropriate fever reducers if needed.");
        feverInfo.add("See a doctor if your fever is very high (above 103°F/39.4°C), lasts more than three days, or is accompanied by severe symptoms.");
        medicalConditions.put("fever", feverInfo);

        List<String> headacheInfo = new ArrayList<>();
        headacheInfo.add("Headaches can be caused by stress, dehydration, lack of sleep, or underlying medical conditions.");
        headacheInfo.add("Try rest, hydration, and over-the-counter pain relievers. See a doctor if headaches are severe or persistent.");
        medicalConditions.put("headache", headacheInfo);

        List<String> coldInfo = new ArrayList<>();
        coldInfo.add("Common colds are viral infections affecting the upper respiratory tract. Rest, stay hydrated, and manage symptoms with over-the-counter medications.");
        coldInfo.add("Most colds resolve within 7-10 days. See a doctor if symptoms are severe or persist longer.");
        medicalConditions.put("cold", coldInfo);

        List<String> diabetesInfo = new ArrayList<>();
        diabetesInfo.add("Diabetes is a condition affecting how your body processes blood sugar. It requires proper management including medication, diet, and exercise.");
        diabetesInfo.add("Regular check-ups and monitoring blood sugar levels are essential for managing diabetes effectively.");
        medicalConditions.put("diabetes", diabetesInfo);

        List<String> hypertensionInfo = new ArrayList<>();
        hypertensionInfo.add("Hypertension (high blood pressure) often has no symptoms but can lead to serious health problems if untreated.");
        hypertensionInfo.add("Management includes regular monitoring, medication if prescribed, reducing sodium intake, exercising regularly, and maintaining a healthy weight.");
        medicalConditions.put("hypertension", hypertensionInfo);
    }

    private void sendMessage(String message) {
        // Add user message to chat
        addUserMessage(message);

        // Clear input field
        messageEditText.setText("");

        // Process message and generate response
        String response = processUserMessage(message);

        // Add bot response to chat
        addBotMessage(response);
    }

    private String processUserMessage(String message) {
        String lowerCaseMessage = message.toLowerCase();

        // Check if in health consultation mode
        if (inHealthConsultation) {
            return handleHealthConsultation(lowerCaseMessage);
        }

        // Check for health consultation request
        if (lowerCaseMessage.contains("not feeling well") ||
                lowerCaseMessage.contains("feel sick") ||
                lowerCaseMessage.contains("have symptoms") ||
                lowerCaseMessage.contains("health concern") ||
                lowerCaseMessage.contains("medical advice")) {

            inHealthConsultation = true;
            consultationStage = 0;
            return "I can help assess your symptoms. Please describe what you're experiencing.";
        }

        // Check for information about Aarogya Assist
        if (lowerCaseMessage.contains("who are you") ||
                lowerCaseMessage.contains("about you") ||
                lowerCaseMessage.contains("made you") ||
                lowerCaseMessage.contains("created you")) {
            return "I'm Aarogya Assist, a health assistant chatbot created by Nayan Pote. I'm designed to provide general health information and engage in conversations. I'm not a replacement for professional medical advice.";
        }

        // Check for greetings
        if (lowerCaseMessage.contains("hi") ||
                lowerCaseMessage.contains("hello") ||
                lowerCaseMessage.contains("hey") ||
                lowerCaseMessage.contains("greetings")) {
            return getRandomResponse(generalGreetings);
        }

        // Check for gratitude
        if (lowerCaseMessage.contains("thank") ||
                lowerCaseMessage.contains("thanks") ||
                lowerCaseMessage.contains("appreciate")) {
            return "You're welcome! I'm happy to help with your health questions.";
        }

        // Check for farewell
        if (lowerCaseMessage.contains("bye") ||
                lowerCaseMessage.contains("goodbye") ||
                lowerCaseMessage.contains("see you") ||
                lowerCaseMessage.contains("talk later")) {
            return "Take care! Remember to prioritize your health. Feel free to chat again if you have more questions.";
        }

        // Check for common health topics
        for (Map.Entry<String, List<String>> entry : healthResponses.entrySet()) {
            if (lowerCaseMessage.contains(entry.getKey())) {
                return getRandomResponse(entry.getValue());
            }
        }

        // Check for medical conditions
        for (Map.Entry<String, List<String>> entry : medicalConditions.entrySet()) {
            if (lowerCaseMessage.contains(entry.getKey())) {
                return getRandomResponse(entry.getValue()) + "\n\nPlease note that I provide general information only. Consult a healthcare professional for personalized advice.";
            }
        }

        // Check for emergency keywords
        if (lowerCaseMessage.contains("emergency") ||
                lowerCaseMessage.contains("severe pain") ||
                lowerCaseMessage.contains("heart attack") ||
                lowerCaseMessage.contains("stroke") ||
                lowerCaseMessage.contains("can't breathe")) {
            return "This sounds like a medical emergency. Please call emergency services (911/112/102) immediately or go to the nearest emergency room. Don't wait for online advice in emergency situations.";
        }

        // General health advice
        if (lowerCaseMessage.contains("health tip") ||
                lowerCaseMessage.contains("healthy habit") ||
                lowerCaseMessage.contains("wellness") ||
                lowerCaseMessage.contains("stay healthy")) {
            List<String> healthTips = new ArrayList<>();
            healthTips.add("Stay physically active - aim for at least 30 minutes of moderate exercise most days.");
            healthTips.add("Maintain a balanced diet rich in fruits, vegetables, whole grains, and lean proteins.");
            healthTips.add("Stay hydrated by drinking plenty of water throughout the day.");
            healthTips.add("Prioritize sleep - most adults need 7-9 hours of quality sleep each night.");
            healthTips.add("Manage stress through techniques like meditation, deep breathing, or enjoyable activities.");
            return getRandomResponse(healthTips);
        }

        // Search for possible symptoms
        Pattern symptomPattern = Pattern.compile("(headache|fever|cough|nausea|pain|fatigue|dizzy|vomiting|rash)");
        Matcher matcher = symptomPattern.matcher(lowerCaseMessage);
        if (matcher.find()) {
            String symptom = matcher.group(1);
            inHealthConsultation = true;
            currentSymptom = symptom;
            reportedSymptoms.add(symptom);
            consultationStage = 1;
            return "I see you mentioned " + symptom + ". " + getRandomResponse(symptomQuestions);
        }

        // If nothing specific is detected, provide a general response
        return getRandomResponse(unknownResponses);
    }

    private String handleHealthConsultation(String message) {
        // Progress through consultation stages
        consultationStage++;

        // After gathering enough information
        if (consultationStage >= 4) {
            inHealthConsultation = false;
            consultationStage = 0;

            // Build response based on gathered symptoms
            StringBuilder response = new StringBuilder();
            response.append("Based on what you've shared about ");

            if (reportedSymptoms.isEmpty()) {
                response.append("your symptoms");
            } else {
                for (int i = 0; i < reportedSymptoms.size(); i++) {
                    if (i > 0) {
                        response.append(i == reportedSymptoms.size() - 1 ? " and " : ", ");
                    }
                    response.append(reportedSymptoms.get(i));
                }
            }

            response.append(", here are some general suggestions:\n\n");

            if (reportedSymptoms.contains("fever")) {
                response.append("• For fever: Rest, stay hydrated, and consider fever reducers like acetaminophen if appropriate.\n");
            }
            if (reportedSymptoms.contains("headache")) {
                response.append("• For headache: Rest in a quiet, dark room. Stay hydrated and consider appropriate pain relievers.\n");
            }
            if (reportedSymptoms.contains("cough")) {
                response.append("• For cough: Stay hydrated, use cough drops if needed, and consider honey (if over 1 year old).\n");
            }
            if (reportedSymptoms.contains("nausea") || reportedSymptoms.contains("vomiting")) {
                response.append("• For nausea/vomiting: Stay hydrated with small sips of clear fluids. Try bland foods when able to eat.\n");
            }
            if (reportedSymptoms.contains("pain")) {
                response.append("• For pain: Rest the affected area, consider appropriate over-the-counter pain relievers.\n");
            }
            if (reportedSymptoms.contains("fatigue")) {
                response.append("• For fatigue: Ensure adequate rest and sleep. Stay hydrated and maintain proper nutrition.\n");
            }
            if (reportedSymptoms.contains("dizzy")) {
                response.append("• For dizziness: Sit or lie down immediately. Stay hydrated and avoid sudden movements.\n");
            }
            if (reportedSymptoms.contains("rash")) {
                response.append("• For rash: Avoid scratching, use mild soap, apply cool compresses if itchy.\n");
            }

            response.append("\nImportant note: These are general suggestions only. If symptoms are severe, persistent, or concerning, please consult a healthcare professional for proper diagnosis and treatment.");

            // Clear consultation data
            reportedSymptoms.clear();
            currentSymptom = null;

            return response.toString();
        } else {
            // Continue gathering information
            return getRandomResponse(symptomQuestions);
        }
    }

    private String getRandomResponse(List<String> responses) {
        if (responses == null || responses.isEmpty()) {
            return "I don't have information on that yet.";
        }
        Random random = new Random();
        return responses.get(random.nextInt(responses.size()));
    }

    private void scrollToBottom() {
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ChatMessage class to store message data
    public static class ChatMessage {
        public static final int SENT_BY_USER = 0;
        public static final int SENT_BY_BOT = 1;
        public static final int DATE_SEPARATOR = 2;

        private String sender;
        private String message;
        private String time;
        private String mediaPath;
        private long timestamp;
        public int sentBy;

        // Constructor for regular messages
        public ChatMessage(String sender, String message, String time) {
            this.sender = sender;
            this.message = message;
            this.time = time;
            this.mediaPath = null;
            this.timestamp = System.currentTimeMillis();

            if ("user".equals(sender)) {
                this.sentBy = SENT_BY_USER;
            } else if ("bot".equals(sender)) {
                this.sentBy = SENT_BY_BOT;
            }
        }

        // Constructor for messages with media
        public ChatMessage(String sender, String message, String time, String mediaPath) {
            this.sender = sender;
            this.message = message;
            this.time = time;
            this.mediaPath = mediaPath;
            this.timestamp = System.currentTimeMillis();

            if ("user".equals(sender)) {
                this.sentBy = SENT_BY_USER;
            } else if ("bot".equals(sender)) {
                this.sentBy = SENT_BY_BOT;
            }
        }

        // Constructor for date separator
        public ChatMessage(String message) {
            this.sender = "";
            this.message = message;
            this.time = "";
            this.mediaPath = null;
            this.timestamp = System.currentTimeMillis();
            this.sentBy = DATE_SEPARATOR;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }

        public String getTime() {
            return time;
        }

        public String getMediaPath() {
            return mediaPath;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isDateSeparator() {
            return sentBy == DATE_SEPARATOR;
        }
    }
}