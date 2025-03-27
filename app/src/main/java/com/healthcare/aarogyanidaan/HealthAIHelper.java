package com.healthcare.aarogyanidaan;

import android.content.Context;
import java.util.*;

public class HealthAIHelper {

    private Context context;
    private Map<String, String> responses;
    private ConversationStyle conversationStyle;
    private String lastTopic = null;

    public enum ConversationStyle {
        FORMAL,
        INFORMAL
    }

    // Common Responses
    private static final Map<String, String> COMMON_RESPONSES = new HashMap<String, String>() {{
        put("greeting", "Hello! How can I assist you today?");
        put("how_are_you", "I'm functioning well, thank you for asking. How are you feeling today?");
        put("goodbye", "Goodbye! Take care and stay healthy.");
        put("thanks", "You're welcome! Let me know if you need more help.");
        put("what_can_you_do", "I can answer health questions, track your health metrics, and give lifestyle tips. How can I assist you?");
        put("help", "You can ask me about health topics like exercise, diet, sleep, and stress. Just type your question!");
        put("unknown", "I'm not sure I understand. Could you clarify?");
        put("creator", "I was built by Nayan Pote. He's pretty smart, right?");
    }};

    // Health-related Responses
    private static final Map<String, String> HEALTH_RESPONSES = new HashMap<String, String>() {{
        put("exercise", "Regular exercise is important for your health. Aim for 30 minutes of moderate activity most days of the week. What type of exercise do you prefer?");
        put("diet", "A balanced diet with fruits, vegetables, lean proteins, and whole grains is key to good health. Do you need advice on meal planning?");
        put("sleep", "Adults need 7-9 hours of quality sleep per night. Establishing a bedtime routine can help improve sleep quality. Are you having trouble sleeping?");
        put("stress", "Managing stress with deep breathing, exercise, or mindfulness can improve your mental health. Would you like to try a relaxation exercise?");
        put("water", "Staying hydrated is essential. Aim for about 2 liters (8 glasses) of water daily. Are you tracking your water intake?");
    }};

    // Constructor
    public HealthAIHelper(Context context, ConversationStyle style) {
        this.context = context;
        this.conversationStyle = style;
        initializeResponses();
    }

    private void initializeResponses() {
        responses = new HashMap<>();
        responses.putAll(COMMON_RESPONSES);
        responses.putAll(HEALTH_RESPONSES);
    }

    // Process Messages
    public String processMessage(String patientId, String message) {
        message = message.toLowerCase();

        // Check for greetings
        if (containsAny(message, "hello", "hi", "hey", "greetings")) {
            lastTopic = "greeting";
            return responses.get("greeting");
        }

        // Check for how are you
        if (containsAny(message, "how are you", "how're you", "how do you feel")) {
            lastTopic = "how_are_you";
            return responses.get("how_are_you");
        }

        // Check for creator
        if (containsAny(message, "who built you", "who made you", "who created you", "who created you","whose your creator", "your creator")) {
            lastTopic = "creator";
            return responses.get("creator");
        }

        // Check for goodbye
        if (containsAny(message, "bye", "goodbye", "see you", "talk later")) {
            lastTopic = "goodbye";
            return responses.get("goodbye");
        }

        // Check for thanks
        if (containsAny(message, "thanks", "thank you", "appreciate")) {
            lastTopic = "thanks";
            return responses.get("thanks");
        }

        // Check for help
        if (containsAny(message, "help", "assist", "support")) {
            lastTopic = "help";
            return responses.get("help");
        }

        // Check for what can you do
        if (containsAny(message, "what can you do", "how can you help")) {
            lastTopic = "what_can_you_do";
            return responses.get("what_can_you_do");
        }

        // Health-related questions
        if (containsAny(message, "exercise", "workout", "fitness", "gym")) {
            lastTopic = "exercise";
            return responses.get("exercise");
        }

        if (containsAny(message, "diet", "nutrition", "food", "eat")) {
            lastTopic = "diet";
            return responses.get("diet");
        }

        if (containsAny(message, "sleep", "rest", "insomnia", "tired")) {
            lastTopic = "sleep";
            return responses.get("sleep");
        }

        if (containsAny(message, "stress", "anxiety", "worried", "relax")) {
            lastTopic = "stress";
            return responses.get("stress");
        }

        if (containsAny(message, "water", "hydration", "thirsty", "drink")) {
            lastTopic = "water";
            return responses.get("water");
        }

        // **Follow-up handling** based on the last known topic
        if (lastTopic != null) {
            switch (lastTopic) {
                case "exercise":
                    return "Do you prefer cardio or strength training?";
                case "diet":
                    return "Would you like me to suggest a meal plan?";
                case "sleep":
                    return "Have you tried adjusting your bedtime routine?";
                case "stress":
                    return "Would you like me to guide you through a relaxation exercise?";
                case "water":
                    return "Are you finding it hard to stay hydrated?";
            }
        }

        // Default response for unknown input
        return responses.get("unknown");
    }

    // Check if the message contains any of the keywords
    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Add a custom response
    public void addCustomResponse(String key, String response) {
        responses.put(key, response);
    }
}


//package com.healthcare.aarogyanidaan;
//
//import android.content.Context;
//
//import java.time.LocalTime;
//import java.util.*;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class HealthAIHelper {
//
//    private Context context;
//    private Map<String, String> responses;
//    private Map<String, List<String>> followUpQuestions;
//    private Map<String, HealthTopic> healthTopics;
//    private Map<String, UserHealthProfile> userProfiles;
//    private List<String> conversationHistory;
//    private String currentUserId;
//    private String lastTopic = null;
//    private int consecutiveSameTopic = 0;
//    private Random random = new Random();
//    private ConversationStyle conversationStyle;
//    private SentimentAnalyzer sentimentAnalyzer;
//    private HealthMetricsTracker metricsTracker;
//    private EmergencyDetector emergencyDetector;
//    private LanguageProcessor languageProcessor;
//
//    // Maximum history entries to maintain context
//    private static final int MAX_HISTORY = 10;
//
//    // Minimum confidence threshold for intent detection
//    private static final double CONFIDENCE_THRESHOLD = 0.65;
//
//    public enum ConversationStyle {
//        FORMAL,
//        INFORMAL,
//        PROFESSIONAL,
//        FRIENDLY,
//        EMPATHETIC
//    }
//
//    public enum Sentiment {
//        POSITIVE,
//        NEUTRAL,
//        NEGATIVE,
//        ANXIOUS,
//        DISTRESSED
//    }
//
//    /**
//     * Represents a complete health topic with detailed information and advice
//     */
//    private class HealthTopic {
//        String name;
//        String overview;
//        List<String> facts;
//        List<String> tips;
//        Map<String, String> subtopics;
//        List<String> followUpQuestions;
//
//        public HealthTopic(String name, String overview) {
//            this.name = name;
//            this.overview = overview;
//            this.facts = new ArrayList<>();
//            this.tips = new ArrayList<>();
//            this.subtopics = new HashMap<>();
//            this.followUpQuestions = new ArrayList<>();
//        }
//
//        public String getRandomFact() {
//            return facts.isEmpty() ? "" : facts.get(random.nextInt(facts.size()));
//        }
//
//        public String getRandomTip() {
//            return tips.isEmpty() ? "" : tips.get(random.nextInt(tips.size()));
//        }
//
//        public String getRandomFollowUp() {
//            return followUpQuestions.isEmpty() ? "" : followUpQuestions.get(random.nextInt(followUpQuestions.size()));
//        }
//    }
//
//    private class UserHealthProfile {
//        String userId;
//        String name;
//        int age;
//        String gender;
//        Map<String, String> healthConditions;
//        List<String> medications;
//        Map<String, Float> metrics; // weight, height, etc.
//        Map<String, Integer> preferences; // topic interests (0-10)
//        List<String> recentTopics;
//        ConversationStyle preferredStyle;
//
//        public UserHealthProfile(String userId) {
//            this.userId = userId;
//            this.healthConditions = new HashMap<>();
//            this.medications = new ArrayList<>();
//            this.metrics = new HashMap<>();
//            this.preferences = new HashMap<>();
//            this.recentTopics = new ArrayList<>();
//            this.preferredStyle = ConversationStyle.FRIENDLY;
//        }
//
//        public void addRecentTopic(String topic) {
//            if (recentTopics.size() >= 5) {
//                recentTopics.remove(0);
//            }
//            recentTopics.add(topic);
//        }
//
//        public boolean hasCondition(String condition) {
//            return healthConditions.containsKey(condition.toLowerCase());
//        }
//    }
//
//    private class SentimentAnalyzer {
//        public Sentiment analyzeSentiment(String message) {
//            message = message.toLowerCase();
//
//            // Simple keyword-based sentiment analysis
//            if (containsAny(message, "happy", "great", "excellent", "good", "wonderful", "amazing")) {
//                return Sentiment.POSITIVE;
//            } else if (containsAny(message, "sad", "unhappy", "depressed", "miserable", "terrible")) {
//                return Sentiment.NEGATIVE;
//            } else if (containsAny(message, "worried", "anxious", "nervous", "concerned", "fear")) {
//                return Sentiment.ANXIOUS;
//            } else if (containsAny(message, "help", "emergency", "pain", "severe", "critical")) {
//                return Sentiment.DISTRESSED;
//            }
//
//            return Sentiment.NEUTRAL;
//        }
//    }
//
//    private class HealthMetricsTracker {
//        private Map<String, List<MetricEntry>> userMetrics;
//
//        public HealthMetricsTracker() {
//            userMetrics = new HashMap<>();
//        }
//
//        public void recordMetric(String userId, String metricType, float value) {
//            if (!userMetrics.containsKey(userId)) {
//                userMetrics.put(userId, new ArrayList<>());
//            }
//
//            MetricEntry entry = new MetricEntry(metricType, value, LocalDateTime.now());
//            userMetrics.get(userId).add(entry);
//        }
//
//        public String getMetricTrend(String userId, String metricType, int days) {
//            if (!userMetrics.containsKey(userId)) {
//                return "No data available for this metric.";
//            }
//
//            List<MetricEntry> metrics = userMetrics.get(userId);
//            List<MetricEntry> relevantMetrics = new ArrayList<>();
//
//            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
//
//            for (MetricEntry entry : metrics) {
//                if (entry.metricType.equals(metricType) && entry.timestamp.isAfter(cutoff)) {
//                    relevantMetrics.add(entry);
//                }
//            }
//
//            if (relevantMetrics.isEmpty() || relevantMetrics.size() < 2) {
//                return "Not enough data to determine a trend for " + metricType + ".";
//            }
//
//            // Sort by timestamp
//            Collections.sort(relevantMetrics, Comparator.comparing(m -> m.timestamp));
//
//            float first = relevantMetrics.get(0).value;
//            float last = relevantMetrics.get(relevantMetrics.size() - 1).value;
//            float change = last - first;
//
//            if (Math.abs(change) < 0.01) {
//                return "Your " + metricType + " has remained stable over the past " + days + " days.";
//            } else if (change > 0) {
//                return "Your " + metricType + " has increased by " + String.format("%.1f", change) +
//                        " over the past " + days + " days.";
//            } else {
//                return "Your " + metricType + " has decreased by " + String.format("%.1f", Math.abs(change)) +
//                        " over the past " + days + " days.";
//            }
//        }
//
//        private class MetricEntry {
//            String metricType;
//            float value;
//            LocalDateTime timestamp;
//
//            public MetricEntry(String metricType, float value, LocalDateTime timestamp) {
//                this.metricType = metricType;
//                this.value = value;
//                this.timestamp = timestamp;
//            }
//        }
//    }
//
//    private class EmergencyDetector {
//        private Set<String> emergencyKeywords;
//
//        public EmergencyDetector() {
//            emergencyKeywords = new HashSet<>(Arrays.asList(
//                    "heart attack", "stroke", "not breathing", "seizure", "severe bleeding",
//                    "unconscious", "choking", "poisoning", "suicide", "overdose", "anaphylaxis",
//                    "severe pain", "chest pain", "can't breathe", "difficulty breathing"
//            ));
//        }
//
//        public boolean isEmergency(String message) {
//            message = message.toLowerCase();
//            for (String keyword : emergencyKeywords) {
//                if (message.contains(keyword)) {
//                    return true;
//                }
//            }
//            return false;
//        }
//
//        public String getEmergencyResponse() {
//            return "This sounds like a medical emergency. Please call emergency services (911 or your local emergency number) immediately. "
//                    + "Don't wait - seek professional medical help now.";
//        }
//    }
//
//    private class LanguageProcessor {
//        private Map<String, List<String>> intents;
//
//        public LanguageProcessor() {
//            intents = new HashMap<>();
//            initializeIntents();
//        }
//
//        private void initializeIntents() {
//            // Add common intents and their keywords
//            intents.put("greeting", Arrays.asList("hello", "hi", "hey", "greetings", "good morning", "good afternoon", "good evening"));
//            intents.put("farewell", Arrays.asList("bye", "goodbye", "see you", "talk later", "catch you later"));
//            intents.put("thanks", Arrays.asList("thanks", "thank you", "appreciate", "grateful"));
//            intents.put("help", Arrays.asList("help", "assist", "guide", "support", "advice"));
//            intents.put("confused", Arrays.asList("don't understand", "confused", "what do you mean", "unclear"));
//            intents.put("capabilities", Arrays.asList("what can you do", "how can you help", "your abilities", "your features"));
//
//            // Health-specific intents
//            intents.put("symptoms", Arrays.asList("symptom", "not feeling well", "sick", "ill", "feeling bad"));
//            intents.put("diagnosis", Arrays.asList("what do i have", "diagnose", "what is wrong", "condition", "disease"));
//            intents.put("medication", Arrays.asList("medicine", "drug", "pill", "prescription", "dose", "medication"));
//            intents.put("side_effects", Arrays.asList("side effect", "reaction", "adverse", "complication"));
//            intents.put("prevention", Arrays.asList("prevent", "avoid", "reduce risk", "lower chance", "protect against"));
//        }
//
//        public Map.Entry<String, Double> detectIntent(String message) {
//            message = message.toLowerCase();
//            String bestIntent = "unknown";
//            double bestScore = 0.0;
//
//            for (Map.Entry<String, List<String>> entry : intents.entrySet()) {
//                String intent = entry.getKey();
//                List<String> keywords = entry.getValue();
//
//                int matches = 0;
//                for (String keyword : keywords) {
//                    if (message.contains(keyword)) {
//                        matches++;
//                    }
//                }
//
//                double score = (double) matches / keywords.size();
//                if (score > bestScore) {
//                    bestScore = score;
//                    bestIntent = intent;
//                }
//            }
//
//            return new AbstractMap.SimpleEntry<>(bestScore > CONFIDENCE_THRESHOLD ? bestIntent : "unknown", bestScore);
//        }
//
//        public String extractEntity(String message, String entityType) {
//            Pattern pattern = null;
//
//            switch (entityType) {
//                case "age":
//                    pattern = Pattern.compile("\\b(\\d+)\\s+(years?\\s+old|yo|y\\.o\\.|years?|yr|y)\\b");
//                    break;
//                case "weight":
//                    pattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\s*(kg|kgs|kilograms?|pounds?|lbs|lb)\\b");
//                    break;
//                case "height":
//                    pattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\s*(cm|centimeters?|m|meters?|ft|feet|foot|inches?|in)\\b");
//                    break;
//                case "date":
//                    pattern = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
//                    break;
//                case "time":
//                    pattern = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?\\b");
//                    break;
//            }
//
//            if (pattern != null) {
//                Matcher matcher = pattern.matcher(message);
//                if (matcher.find()) {
//                    return matcher.group(0);
//                }
//            }
//
//            return null;
//        }
//    }
//
//    // Constructor
//    public HealthAIHelper(Context context, ConversationStyle style) {
//        this.context = context;
//        this.conversationStyle = style;
//        this.conversationHistory = new ArrayList<>();
//        this.userProfiles = new HashMap<>();
//        this.sentimentAnalyzer = new SentimentAnalyzer();
//        this.metricsTracker = new HealthMetricsTracker();
//        this.emergencyDetector = new EmergencyDetector();
//        this.languageProcessor = new LanguageProcessor();
//
//        initializeResponses();
//        initializeFollowUps();
//        initializeHealthTopics();
//    }
//
//    private void initializeResponses() {
//        responses = new HashMap<>();
//
//        // Common responses
//        responses.put("greeting_formal", "Greetings. How may I be of assistance with your health concerns today?");
//        responses.put("greeting_informal", "Hey there! How can I help you with your health today?");
//        responses.put("greeting_professional", "Welcome to Aarogya Assist. I'm here to provide health guidance. How may I help you?");
//        responses.put("greeting_friendly", "Hi! I'm Aarogya Assist, your friendly health companion. What's on your mind today?");
//        responses.put("greeting_empathetic", "Hello. I'm here to listen and support you with any health concerns. How are you feeling today?");
//
//        responses.put("how_are_you_formal", "I am functioning optimally. More importantly, how is your health status today?");
//        responses.put("how_are_you_informal", "I'm doing great! But enough about me - how are YOU feeling today?");
//        responses.put("how_are_you_professional", "I'm operational and ready to assist. May I inquire about your current health status?");
//        responses.put("how_are_you_friendly", "I'm fantastic, thanks for asking! But I'm much more interested in how you're doing today?");
//        responses.put("how_are_you_empathetic", "I'm here and ready to listen. How are you feeling today - both physically and emotionally?");
//
//        responses.put("goodbye_formal", "Farewell. Please do not hesitate to return if you require further health information.");
//        responses.put("goodbye_informal", "Bye! Take care and stay healthy! Chat again soon!");
//        responses.put("goodbye_professional", "Thank you for consulting Aarogya Assist. Remember to prioritize your health. Goodbye.");
//        responses.put("goodbye_friendly", "See you soon! Remember, I'm always here when you need health advice or just want to chat!");
//        responses.put("goodbye_empathetic", "Take good care of yourself. I'll be here whenever you need support or guidance. Goodbye for now.");
//
//        responses.put("thanks_formal", "You are most welcome. It is my purpose to provide health assistance.");
//        responses.put("thanks_informal", "No problem at all! Happy to help anytime!");
//        responses.put("thanks_professional", "You're welcome. Aarogya Assist is committed to supporting your health journey.");
//        responses.put("thanks_friendly", "Anytime! That's what friends are for. Let me know if you need anything else!");
//        responses.put("thanks_empathetic", "I'm glad I could be of help. Your wellbeing matters, and I'm here to support you.");
//
//        responses.put("unknown_formal", "I apologize, but I require additional clarification on your query.");
//        responses.put("unknown_informal", "Hmm, not quite sure what you mean. Mind explaining a bit more?");
//        responses.put("unknown_professional", "I'm unable to determine your specific health question. Could you please provide more details?");
//        responses.put("unknown_friendly", "I'm a bit confused! Could you tell me more about what you're looking for?");
//        responses.put("unknown_empathetic", "I want to help you properly, but I'm not quite understanding. Could you share more about your concern?");
//
//        responses.put("capabilities_formal", "I am programmed to provide health information on topics such as nutrition, exercise, sleep, mental wellness, and common health conditions. I can track health metrics and offer personalized wellness recommendations.");
//        responses.put("capabilities_informal", "I can chat about tons of health stuff like food, workouts, sleep tips, stress busting, and common health problems. I can also keep track of your health stats and give you personalized tips!");
//        responses.put("capabilities_professional", "Aarogya Assist provides evidence-based health information across multiple domains including nutrition, physical activity, sleep hygiene, mental health, and disease management. I can monitor health metrics and deliver personalized health recommendations.");
//        responses.put("capabilities_friendly", "I'm your go-to health buddy! I can talk about healthy eating, fun ways to exercise, sleep better, feel happier, and understand health conditions. I'll remember your health info to give you personalized advice!");
//        responses.put("capabilities_empathetic", "I'm here to support your health journey by providing information on nutrition, exercise, sleep, emotional wellness, and health conditions. I can help you track your progress and offer guidance tailored to your unique needs.");
//
//        // Health-related responses
//        responses.put("exercise_formal", "Regular physical activity is essential for optimal health. Current guidelines recommend 150 minutes of moderate-intensity exercise per week, along with muscle-strengthening activities twice weekly.");
//        responses.put("exercise_informal", "Moving your body regularly is super important! Try to get about 150 minutes of moderate exercise each week, plus some strength training twice a week. What kind of workouts do you enjoy?");
//        responses.put("exercise_professional", "Physical activity is a cornerstone of preventive healthcare. The current evidence-based recommendation is 150-300 minutes of moderate-intensity aerobic activity weekly, complemented by muscle-strengthening activities on 2 or more days per week.");
//        responses.put("exercise_friendly", "Exercise is amazing for your body and mind! Aim for about 30 minutes of fun activity most days - dancing, walking, swimming - whatever makes you happy! Don't forget to mix in some strength exercises twice a week. What's your favorite way to move?");
//        responses.put("exercise_empathetic", "Finding movement that you enjoy is so important for both physical and emotional wellbeing. The goal is around 150 minutes weekly of activity that raises your heart rate, plus some strength exercises. How does movement fit into your life currently?");
//
//        responses.put("diet_formal", "A nutritionally balanced diet encompasses a variety of fruits, vegetables, whole grains, lean proteins, and healthy fats. Recommended proportions include filling half your plate with vegetables and fruits, one quarter with lean proteins, and one quarter with whole grains.");
//        responses.put("diet_informal", "Eating healthy is pretty simple! Load up on colorful veggies and fruits, add some lean proteins like chicken or beans, throw in whole grains, and don't forget healthy fats from avocados or nuts. What's your favorite healthy meal?");
//        responses.put("diet_professional", "Optimal nutrition requires diverse intake of micronutrient-dense foods. The evidence supports a dietary pattern rich in plant foods, adequate protein from quality sources, whole grain carbohydrates, and essential fatty acids, while minimizing ultra-processed foods and added sugars.");
//        responses.put("diet_friendly", "Food is fuel AND fun! Try filling half your plate with colorful veggies, add a palm-sized portion of protein, a cupped handful of whole grains, and a thumb of healthy fats. Small changes add up! What's one healthy food you absolutely love?");
//        responses.put("diet_empathetic", "Nourishing your body is an act of self-care. Finding a balanced approach that includes plenty of vegetables, fruits, quality proteins, whole grains and healthy fats can support not just physical health but emotional wellbeing too. How do you feel about your current eating patterns?");
//
//        responses.put("sleep_formal", "Sufficient sleep is critical for cognitive function, immune health, and metabolic regulation. Adults require 7-9 hours nightly. Establishing consistent sleep and wake times enhances sleep quality.");
//        responses.put("sleep_informal", "Sleep is super important! Most adults need 7-9 hours each night. Going to bed and waking up at the same time really helps, even on weekends. Having trouble sleeping?");
//        responses.put("sleep_professional", "Sleep is a fundamental biological process essential for multiple physiological functions. The recommended duration for adults is 7-9 hours of quality sleep per night, with consistent circadian timing. Sleep hygiene practices significantly impact sleep architecture and quality.");
//        responses.put("sleep_friendly", "Sleep is like a superpower! When you get those 7-9 hours, your brain works better, you get sick less often, and you're in a better mood! Try to go to bed and wake up at the same time each day - your body loves routine. What's your bedtime routine like?");
//        responses.put("sleep_empathetic", "Rest is so important for both your body and mind. Those 7-9 hours help your body heal and your mind process emotions. Creating a peaceful bedtime routine can be a wonderful form of self-care. How have you been sleeping lately?");
//
//        responses.put("stress_formal", "Chronic stress has detrimental effects on physiological systems. Evidence-based stress management techniques include deep breathing exercises, physical activity, adequate sleep, and mindfulness meditation.");
//        responses.put("stress_informal", "Stress happens to everyone! Some quick fixes: take deep breaths, go for a walk, chat with friends, or try a quick meditation. What helps you chill out when you're stressed?");
//        responses.put("stress_professional", "Psychological stress triggers a cascade of physiological responses including hypothalamic-pituitary-adrenal axis activation and sympathetic nervous system stimulation. Evidence-based interventions include diaphragmatic breathing, progressive muscle relaxation, regular physical activity, cognitive behavioral techniques, and mindfulness practices.");
//        responses.put("stress_friendly", "Life gets overwhelming sometimes! Your body actually has a built-in relaxation button - your breath! Try breathing in slowly for 4 counts, hold briefly, then exhale for 6. Nature walks, giggling with friends, and moving your body are also stress-busting superstars. What's your go-to stress reliever?");
//        responses.put("stress_empathetic", "Feeling stressed is a completely normal part of being human. Your body and mind are just trying to protect you. Gentle practices like deep breathing, connecting with loved ones, movement, or quiet moments in nature can help your nervous system find balance again. How has stress been showing up for you lately?");
//
//        responses.put("water_formal", "Adequate hydration is essential for physiological functions. Requirements vary by individual, but approximately 2.7 liters for women and 3.7 liters for men daily from all sources is recommended.");
//        responses.put("water_informal", "Staying hydrated is super important! While 8 glasses a day is a good start, your needs depend on your size, activity level, and even the weather. What helps you remember to drink enough?");
//        responses.put("water_professional", "Optimal hydration status is critical for cellular function, thermoregulation, circulatory efficiency, and metabolic processes. Individual requirements vary based on body mass, activity level, environmental conditions, and health status, but general guidelines suggest 2.7-3.7 liters daily from all sources.");
//        responses.put("water_friendly", "Water is like magic for your body! It helps your brain think clearly, keeps your skin glowing, and helps everything in your body work better. Try keeping a water bottle with you as a friendly reminder to sip throughout the day. Do you have a favorite water bottle?");
//        responses.put("water_empathetic", "Staying hydrated is one of the simplest yet most powerful ways to care for yourself. Your body uses water for nearly every function. Finding gentle ways to incorporate more fluids - like herbal teas or water with a splash of fruit - can make a big difference in how you feel. How do you feel when you're well-hydrated versus dehydrated?");
//
//        // Mental health responses
//        responses.put("anxiety_formal", "Anxiety disorders are characterized by excessive worry and physiological manifestations. Evidence-based treatments include cognitive-behavioral therapy, mindfulness practices, and in some cases, pharmacological interventions.");
//        responses.put("anxiety_informal", "Feeling anxious is totally normal sometimes! Deep breathing, getting outside, talking to supportive people, and mindfulness can all help. For ongoing anxiety, talking to a healthcare provider is a great idea.");
//        responses.put("anxiety_professional", "Anxiety disorders represent dysregulation of normal stress response systems. First-line interventions include psychotherapeutic approaches such as cognitive-behavioral therapy, acceptance and commitment therapy, mindfulness-based stress reduction, lifestyle modifications, and when indicated, pharmacotherapy with SSRIs or SNRIs.");
//        responses.put("anxiety_friendly", "Our minds are designed to protect us, but sometimes they go into overdrive! When anxiety visits, remember you're not alone - millions experience this. Try box breathing (4 counts in, hold 4, out 4, hold 4), grounding exercises, or a quick body scan. For persistent anxiety, chatting with a mental health pro can be super helpful!");
//        responses.put("anxiety_empathetic", "Anxiety can feel overwhelming, but please know that what you're experiencing is valid and there is support available. Your nervous system is trying to protect you, even if it feels uncomfortable. Gentle approaches like breath awareness, connecting with nature, and self-compassion can help. Would you like to explore some simple techniques that might help in difficult moments?");
//
//        responses.put("depression_formal", "Depression is a complex condition affecting mood, cognition, and physical health. Recommended interventions include psychotherapy, regular physical activity, social connection, proper sleep hygiene, and when appropriate, medication.");
//        responses.put("depression_informal", "Depression is more than just feeling sad - and it's never your fault. Talking to supportive people, getting outside, moving your body, and working with healthcare providers can all be part of feeling better.");
//        responses.put("depression_professional", "Major depressive disorder involves neurobiological changes affecting multiple systems. Evidence-based treatment approaches include psychotherapeutic modalities such as cognitive-behavioral therapy and interpersonal therapy, lifestyle interventions targeting sleep, nutrition, and physical activity, social support enhancement, and pharmacologic management when indicated.");
//        responses.put("depression_friendly", "Depression can make even simple things feel really hard, and that's not your fault at all. Small steps matter - maybe just stepping outside for 5 minutes of sunshine, sending a text to someone who cares about you, or doing one tiny self-care thing. Professional support from therapists or doctors can make a huge difference too. You deserve to feel better!");
//        responses.put("depression_empathetic", "Living with depression can be incredibly difficult, and I want you to know that you're not alone in this experience. Your feelings are valid, and depression is never a personal failing. While I'm here to listen, connecting with healthcare providers who can offer personalized support is really important. Would it be helpful to talk about small, gentle steps that might support your wellbeing?");
//
//        // Disclaimer responses
//        responses.put("disclaimer", "I'm here to provide general health information, but I'm not a replacement for professional medical advice. Always consult qualified healthcare providers for personal medical concerns.");
//        responses.put("emergency_disclaimer", "This sounds like a medical emergency. Please call emergency services (911 or your local emergency number) immediately. Don't wait - seek professional medical help now.");
//    }
//
//    private void initializeFollowUps() {
//        followUpQuestions = new HashMap<>();
//
//        followUpQuestions.put("exercise", Arrays.asList(
//                "What types of physical activity do you enjoy most?",
//                "How many days per week do you currently exercise?",
//                "Do you prefer cardio, strength training, or flexibility exercises?",
//                "Have you noticed any benefits from regular exercise?",
//                "What's your biggest challenge when it comes to staying active?",
//                "Would you like some ideas for exercises you can do at home?",
//                "Have you considered tracking your daily steps?",
//                "Do you have any physical limitations I should consider when offering exercise advice?"
//        ));
//
//        followUpQuestions.put("diet", Arrays.asList(
//                "How would you describe your current eating habits?",
//                "Do you have any dietary restrictions or preferences?",
//                "How many servings of fruits and vegetables do you typically eat daily?",
//                "Do you prepare most of your meals at home or eat out frequently?",
//                "Would you like some simple, healthy recipe ideas?",
//                "Are there specific nutritional concerns you'd like to address?",
//                "How do you feel after eating certain foods?",
//                "Have you tried meal planning or prepping?"
//        ));
//
//        followUpQuestions.put("sleep", Arrays.asList(
//                "How many hours of sleep do you typically get each night?",
//                "Do you have trouble falling asleep or staying asleep?",
//                "What does your bedtime routine look like?",
//                "Do you use electronic devices before bed?",
//                "How do you feel when you wake up in the morning?",
//                "Have you noticed any patterns that affect your sleep quality?",
//                "Would you like some tips for creating a sleep-friendly environment?",
//                "Do you wake up at roughly the same time each day?"
//        ));
//
//        followUpQuestions.put("stress", Arrays.asList(
//                "Where do you typically feel stress in your body?",
//                "What activities help you relax and unwind?",
//                "Have you tried any breathing exercises or meditation?",
//                "How does stress typically affect your daily life?",
//                "Would you like to try a quick relaxation technique right now?",
//                "Do you have supportive people you can talk to when stressed?",
//                "How do you usually cope with stressful situations?",
//                "Have you identified your main sources of stress?"
//        ));
//
//        followUpQuestions.put("water", Arrays.asList(
//                "How much water do you typically drink in a day?",
//                "Do you carry a water bottle with you?",
//                "Do you notice any differences when you're well-hydrated versus dehydrated?",
//                "What helps you remember to drink water throughout the day?",
//                "Do you enjoy other hydrating beverages like herbal tea?",
//                "Would you like some tips for increasing your water intake?",
//                "Do you find plain water appealing, or do you prefer flavored options?",
//                "Have you tried using a water tracking app?"
//        ));
//
//        followUpQuestions.put("mental_health", Arrays.asList(
//                "How would you describe your mood over the past two weeks?",
//                "What activities bring you joy or a sense of accomplishment?",
//                "Do you have practices that support your emotional wellbeing?",
//                "How do you typically handle difficult emotions?",
//                "Would you like to learn about some simple mindfulness practices?",
//                "Do you feel comfortable reaching out for support when needed?",
//                "Have you noticed any patterns or triggers that affect your mood?",
//                "What does self-care look like for you?"
//        ));
//    }
//
//    private void initializeHealthTopics() {
//        healthTopics = new HashMap<>();
//
//        // Exercise topic
//        HealthTopic exerciseTopic = new HealthTopic("exercise",
//                "Regular physical activity is one of the most important things you can do for your health. It can help control weight, reduce risk of diseases, strengthen bones and muscles, and improve mental health and mood.");
//
//        exerciseTopic.facts.addAll(Arrays.asList(
//                "Just 150 minutes of moderate physical activity per week can reduce your risk of heart disease by up to 30%.",
//                "Exercise stimulates the release of endorphins, which are natural mood elevators.",
//                "Resistance training can help maintain muscle mass, which naturally declines with age.",
//                "Regular exercise can improve cognitive function and reduce risk of dementia.",
//                "Exercise can be as effective as medication for treating mild to moderate depression.",
//                "Physical activity improves sleep quality and reduces the time it takes to fall asleep.",
//                "Regular exercise can add years to your life expectancy.",
//                "Even small amounts of activity are better than none - every minute counts!"
//        ));
//
//        exerciseTopic.tips.addAll(Arrays.asList(
//                "Start with just 10 minutes of activity and gradually increase over time.",
//                "Find activities you enjoy so exercise feels like fun, not a chore.",
//                "Schedule physical activity into your day like any other important appointment.",
//                "Mix up your routine to prevent boredom and work different muscle groups.",
//                "Consider wearable fitness trackers to monitor your progress and stay motivated.",
//                "Exercise with friends or join a class for social connection and accountability.",
//                "Incorporate movement into daily activities by taking stairs or walking during phone calls.",
//                "Listen to your body and adjust intensity based on how you feel each day."
//        ));
//
//        exerciseTopic.subtopics.put("cardiovascular", "Aerobic exercises like walking, jogging, swimming, and cycling improve heart and lung health. Aim for at least 150 minutes of moderate-intensity activity weekly.");
//        exerciseTopic.subtopics.put("strength", "Resistance training with weights, bands, or bodyweight exercises helps maintain muscle mass and bone density. Include at least 2 days of strength training weekly.");
//        exerciseTopic.subtopics.put("flexibility", "Stretching and activities like yoga improve range of motion and may reduce injury risk. Hold each stretch for 30-60 seconds and breathe deeply.");
//        exerciseTopic.subtopics.put("balance", "Balance exercises like tai chi can reduce fall risk, especially important as we age. Practice standing on one foot or heel-to-toe walking.");
//
//        exerciseTopic.followUpQuestions.addAll(Arrays.asList(
//                "What type of exercise do you enjoy most?",
//                "How many days per week do you currently engage in physical activity?",
//                "Would you like some suggestions for beginner-friendly exercises?",
//                "Do you have any health conditions that affect your ability to exercise?",
//                "Have you considered working with a fitness professional to develop a personalized plan?"
//        ));
//
//        healthTopics.put("exercise", exerciseTopic);
//
//        // Diet topic
//        HealthTopic dietTopic = new HealthTopic("diet",
//                "Nutrition plays a crucial role in health and wellbeing. A balanced diet provides the nutrients your body needs to function properly and helps prevent chronic diseases.");
//
//        dietTopic.facts.addAll(Arrays.asList(
//                "Eating a variety of colorful fruits and vegetables exposes your body to a wide range of beneficial nutrients.",
//                "Dietary fiber can help maintain bowel health, lower cholesterol, and control blood sugar levels.",
//                "Omega-3 fatty acids found in fish, nuts, and seeds have anti-inflammatory properties.",
//                "Ultra-processed foods are often high in salt, sugar, and unhealthy fats, contributing to chronic health issues.",
//                "Adequate protein intake is essential for muscle maintenance, immune function, and enzyme production.",
//                "Gut health is closely linked to overall health, including mental wellbeing.",
//                "Your body needs over 40 different nutrients that no single food can provide.",
//                "Hydration is a key component of good nutrition - even mild dehydration affects cognitive function."
//        ));
//
//        dietTopic.tips.addAll(Arrays.asList(
//                "Aim for at least 5 servings of fruits and vegetables daily.",
//                "Choose whole grains over refined grains for more fiber and nutrients.",
//                "Include plant-based protein sources like legumes and nuts regularly.",
//                "Prepare meals at home more often to control ingredients and portion sizes.",
//                "Read nutrition labels to identify added sugars and sodium.",
//                "Practice mindful eating by slowing down and paying attention to hunger and fullness cues.",
//                "Plan meals ahead to make healthier choices more convenient.",
//                "Start small - replace one less healthy food with a more nutritious option each week."
//        ));
//
//        dietTopic.subtopics.put("plantBased", "Plant-based diets emphasize fruits, vegetables, whole grains, legumes, nuts, and seeds. They're associated with lower risk of heart disease, certain cancers, and type 2 diabetes.");
//        dietTopic.subtopics.put("protein", "Protein is essential for muscle maintenance, immune function, and enzyme production. Sources include lean meats, fish, eggs, dairy, legumes, tofu, and nuts.");
//        dietTopic.subtopics.put("healthyFats", "Healthy fats from sources like avocados, olive oil, nuts, and fish support brain function and hormone production. Limit saturated and trans fats.");
//        dietTopic.subtopics.put("hydration", "Water is essential for nearly every bodily function. Aim for about 2-3 liters daily, adjusting for activity level and climate.");
//
//        dietTopic.followUpQuestions.addAll(Arrays.asList(
//                "How would you describe your current eating habits?",
//                "Do you have any dietary restrictions or preferences?",
//                "Would you like some ideas for quick, healthy meals?",
//                "Have you noticed any foods that seem to affect how you feel?",
//                "What's your biggest challenge when it comes to eating healthfully?"
//        ));
//
//        healthTopics.put("diet", dietTopic);
//
//        // Sleep topic
//        HealthTopic sleepTopic = new HealthTopic("sleep",
//                "Quality sleep is essential for physical health, cognitive function, and emotional wellbeing. During sleep, your body repairs tissues, consolidates memories, and regulates hormones.");
//
//        sleepTopic.facts.addAll(Arrays.asList(
//                "Adults typically need 7-9 hours of sleep per night for optimal health.",
//                "REM sleep is critical for memory consolidation and learning.",
//                "Poor sleep is linked to increased risk of heart disease, diabetes, and depression.",
//                "Blue light from screens can suppress melatonin production, making it harder to fall asleep.",
//                "Consistent sleep and wake times help regulate your body's internal clock.",
//                "During deep sleep, your body repairs tissues and strengthens the immune system.",
//                "Sleep deprivation impairs cognitive function similar to alcohol intoxication.",
//                "Sleep quality is as important as quantity for overall health."
//        ));
//
//        sleepTopic.tips.addAll(Arrays.asList(
//                "Create a consistent sleep schedule, even on weekends.",
//                "Design a relaxing bedtime routine to signal your body it's time to wind down.",
//                "Keep your bedroom cool, dark, and quiet for optimal sleep conditions.",
//                "Limit caffeine after noon and alcohol close to bedtime.",
//                "Avoid screens for at least 30 minutes before sleep.",
//                "Exercise regularly, but try to finish vigorous workouts several hours before bedtime.",
//                "Use your bed only for sleep and intimacy to strengthen mental association.",
//                "If you can't fall asleep after 20 minutes, get up and do something relaxing until you feel drowsy."
//        ));
//
//        sleepTopic.subtopics.put("sleepHygiene", "Sleep hygiene refers to habits and practices that promote good sleep quality. This includes consistent sleep schedules, a relaxing bedtime routine, and a sleep-friendly environment.");
//        sleepTopic.subtopics.put("sleepCycles", "Sleep cycles include light sleep, deep sleep, and REM sleep. A complete cycle lasts about 90 minutes, and adults typically need 4-6 cycles per night.");
//        sleepTopic.subtopics.put("insomnia", "Insomnia is difficulty falling or staying asleep. Cognitive behavioral therapy for insomnia (CBT-I) is an effective non-medication treatment.");
//        sleepTopic.subtopics.put("circadianRhythm", "Your circadian rhythm is an internal 24-hour clock that regulates sleep-wake cycles. Exposure to natural light helps calibrate this rhythm.");
//
//        sleepTopic.followUpQuestions.addAll(Arrays.asList(
//                "How would you rate your sleep quality on a scale of 1-10?",
//                "Do you have trouble falling asleep or staying asleep?",
//                "What does your bedtime routine currently look like?",
//                "Have you noticed any patterns that affect your sleep quality?",
//                "Would you like some suggestions for creating a sleep-friendly environment?"
//        ));
//
//        healthTopics.put("sleep", sleepTopic);
//
//        // Mental health topic
//        HealthTopic mentalHealthTopic = new HealthTopic("mental_health",
//                "Mental health is an integral part of overall wellbeing. It includes emotional, psychological, and social wellness, affecting how we think, feel, and act in daily life.");
//
//        mentalHealthTopic.facts.addAll(Arrays.asList(
//                "Mental health conditions are common - about 1 in 5 adults experience a mental illness each year.",
//                "Stress has physical effects on the body, including increased inflammation and elevated cortisol levels.",
//                "Regular exercise can reduce symptoms of depression and anxiety.",
//                "Social connection is strongly linked to mental wellbeing and longevity.",
//                "Mindfulness practices can change brain structure and function over time.",
//                "Sleep quality significantly impacts mental health and emotional regulation.",
//                "Genetics, environment, and life experiences all contribute to mental health.",
//                "Seeking help for mental health concerns is a sign of strength, not weakness."
//        ));
//
//        mentalHealthTopic.tips.addAll(Arrays.asList(
//                "Practice self-compassion - treat yourself with the same kindness you'd offer a friend.",
//                "Take short mindfulness breaks throughout the day to reduce stress.",
//                "Maintain social connections, even brief interactions can boost mood.",
//                "Set boundaries to protect your energy and emotional wellbeing.",
//                "Engage in activities that bring you joy and a sense of accomplishment.",
//                "Spend time in nature, which has been shown to reduce stress and improve mood.",
//                "Limit media consumption, especially news that increases anxiety.",
//                "Seek professional support if you're struggling - effective treatments are available."
//        ));
//
//        mentalHealthTopic.subtopics.put("stress", "Stress is the body's response to demands or threats. While acute stress can be motivating, chronic stress negatively impacts physical and mental health.");
//        mentalHealthTopic.subtopics.put("anxiety", "Anxiety involves excessive worry and physical symptoms like increased heart rate. It ranges from mild to severe and can be managed with various techniques.");
//        mentalHealthTopic.subtopics.put("depression", "Depression is more than sadness - it involves persistent low mood, loss of interest, and changes in sleep, appetite, and energy levels.");
//        mentalHealthTopic.subtopics.put("mindfulness", "Mindfulness is the practice of present-moment awareness without judgment. Regular practice can reduce stress and improve emotional regulation.");
//
//        mentalHealthTopic.followUpQuestions.addAll(Arrays.asList(
//                "How would you describe your overall mental wellbeing currently?",
//                "What activities help you feel calm and centered?",
//                "Have you tried any mindfulness or relaxation practices?",
//                "Do you have supportive people you can talk to when needed?",
//                "Would you like some simple techniques for managing stress?"
//        ));
//
//        healthTopics.put("mental_health", mentalHealthTopic);
//
//        // Preventive health topic
//        HealthTopic preventiveHealthTopic = new HealthTopic("preventive_health",
//                "Preventive healthcare focuses on maintaining wellbeing and preventing disease before it occurs through screenings, vaccinations, and lifestyle choices.");
//
//        preventiveHealthTopic.facts.addAll(Arrays.asList(
//                "Regular health screenings can detect problems before symptoms appear.",
//                "Vaccinations prevent millions of illnesses and deaths worldwide each year.",
//                "Hand washing is one of the most effective ways to prevent infectious disease spread.",
//                "Dental checkups every 6 months help prevent serious oral health issues.",
//                "Sunscreen use reduces skin cancer risk and prevents premature aging.",
//                "Regular vision checks can detect not only eye problems but also conditions like diabetes.",
//                "Preventive healthcare is more cost-effective than treating advanced disease.",
//                "Family history is an important factor in determining appropriate preventive measures."
//        ));
//
//        preventiveHealthTopic.tips.addAll(Arrays.asList(
//                "Schedule regular check-ups with your healthcare provider.",
//                "Stay up-to-date on recommended vaccinations for your age and risk factors.",
//                "Know your family health history and share it with your healthcare provider.",
//                "Learn the warning signs of common serious conditions like heart attack and stroke.",
//                "Practice good hygiene, including regular hand washing.",
//                "Use sunscreen daily and avoid excessive sun exposure.",
//                "Maintain a healthy weight through balanced nutrition and regular activity.",
//                "Avoid tobacco and limit alcohol consumption."
//        ));
//
//        preventiveHealthTopic.subtopics.put("screenings", "Age-appropriate health screenings can detect problems early when they're most treatable. These include blood pressure, cholesterol, cancer screenings, and more.");
//        preventiveHealthTopic.subtopics.put("vaccinations", "Vaccines are safe, effective tools for preventing serious infectious diseases. Recommended vaccinations vary by age, health status, and other factors.");
//        preventiveHealthTopic.subtopics.put("lifestyle", "Lifestyle choices like nutrition, physical activity, sleep, and stress management play a crucial role in disease prevention.");
//        preventiveHealthTopic.subtopics.put("riskFactors", "Understanding and managing risk factors such as family history, environment, and personal behaviors can help prevent disease development.");
//
//        preventiveHealthTopic.followUpQuestions.addAll(Arrays.asList(
//                "When was your last comprehensive health check-up?",
//                "Are your vaccinations up-to-date?",
//                "Do you know your key health numbers like blood pressure and cholesterol?",
//                "Is there a particular health condition you're concerned about preventing?",
//                "Would you like information on recommended health screenings for your age and gender?"
//        ));
//
//        healthTopics.put("preventive_health", preventiveHealthTopic);
//    }
//
//    // Process incoming messages
//    public String processMessage(String patientId, String message) {
//        // Check for emergencies first
//        if (emergencyDetector.isEmergency(message)) {
//            return responses.get("emergency_disclaimer");
//        }
//
//        // Update or create user profile
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//
//        UserHealthProfile profile = userProfiles.get(patientId);
//        currentUserId = patientId;
//
//        // Update conversation history
//        if (conversationHistory.size() >= MAX_HISTORY) {
//            conversationHistory.remove(0);
//        }
//        conversationHistory.add(message);
//
//        // Analyze message sentiment
//        Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(message);
//
//        // Detect user intent
//        Map.Entry<String, Double> intentResult = languageProcessor.detectIntent(message);
//        String intent = intentResult.getKey();
//
//        // Process based on intent
//        String response = generateResponse(intent, message, profile, sentiment);
//
//        // Update last topic if appropriate
//        if (!intent.equals("unknown") && !intent.equals("greeting") &&
//                !intent.equals("farewell") && !intent.equals("thanks")) {
//
//            if (intent.equals(lastTopic)) {
//                consecutiveSameTopic++;
//            } else {
//                consecutiveSameTopic = 0;
//            }
//
//            lastTopic = intent;
//            profile.addRecentTopic(intent);
//        }
//
//        // Check for health metrics in the message
//        processHealthMetrics(message, profile);
//
//        return response;
//    }
//
//    private String generateResponse(String intent, String message, UserHealthProfile profile, Sentiment sentiment) {
//        // Get the style suffix based on current conversation style
//        String styleSuffix = "_" + conversationStyle.toString().toLowerCase();
//
//        // Adjust style based on sentiment if needed
//        if (sentiment == Sentiment.ANXIOUS || sentiment == Sentiment.DISTRESSED) {
//            styleSuffix = "_empathetic";  // Override to empathetic style for distressed users
//        }
//
//        // Check for specific intents
//        switch (intent) {
//            case "greeting":
//                return responses.get("greeting" + styleSuffix);
//
//            case "farewell":
//                return responses.get("goodbye" + styleSuffix);
//
//            case "thanks":
//                return responses.get("thanks" + styleSuffix);
//
//            case "capabilities":
//                return responses.get("capabilities" + styleSuffix);
//
//            case "help":
//                return "I'm here to help with your health questions. You can ask me about topics like exercise, diet, sleep, stress management, or preventive health. What would you like to know about?";
//
//            case "confused":
//                return responses.get("unknown" + styleSuffix);
//        }
//
//        // Check for health topics
//        for (String topicKey : healthTopics.keySet()) {
//            if (message.toLowerCase().contains(topicKey) ||
//                    (lastTopic != null && lastTopic.equals(topicKey))) {
//
//                HealthTopic topic = healthTopics.get(topicKey);
//
//                // If we've been on this topic for a while, provide a follow-up question
//                if (consecutiveSameTopic > 1) {
//                    return topic.getRandomFollowUp();
//                }
//
//                // Build a response based on the topic
//                StringBuilder topicResponse = new StringBuilder();
//
//                // Add the overview
//                topicResponse.append(topic.overview).append(" ");
//
//                // Add a random fact
//                topicResponse.append(topic.getRandomFact()).append(" ");
//
//                // Add a tip
//                topicResponse.append(topic.getRandomTip()).append(" ");
//
//                // Add a follow-up question
//                topicResponse.append(topic.getRandomFollowUp());
//
//                return topicResponse.toString();
//            }
//        }
//
//        // Check for health-related keywords
//        if (message.toLowerCase().contains("exercise") || message.toLowerCase().contains("workout") ||
//                message.toLowerCase().contains("fitness") || message.toLowerCase().contains("gym")) {
//            return responses.get("exercise" + styleSuffix);
//        }
//
//        if (message.toLowerCase().contains("diet") || message.toLowerCase().contains("nutrition") ||
//                message.toLowerCase().contains("food") || message.toLowerCase().contains("eat")) {
//            return responses.get("diet" + styleSuffix);
//        }
//
//        if (message.toLowerCase().contains("sleep") || message.toLowerCase().contains("tired") ||
//                message.toLowerCase().contains("insomnia") || message.toLowerCase().contains("rest")) {
//            return responses.get("sleep" + styleSuffix);
//        }
//
//        if (message.toLowerCase().contains("stress") || message.toLowerCase().contains("anxiety") ||
//                message.toLowerCase().contains("worried") || message.toLowerCase().contains("relax")) {
//            return responses.get("stress" + styleSuffix);
//        }
//
//        if (message.toLowerCase().contains("water") || message.toLowerCase().contains("hydration") ||
//                message.toLowerCase().contains("thirsty") || message.toLowerCase().contains("drink")) {
//            return responses.get("water" + styleSuffix);
//        }
//
//        if (message.toLowerCase().contains("depress") || message.toLowerCase().contains("sad") ||
//                message.toLowerCase().contains("unhappy") || message.toLowerCase().contains("low mood")) {
//            return responses.get("depression" + styleSuffix);
//        }
//
//        if (message.toLowerCase().contains("anxious") || message.toLowerCase().contains("anxiety") ||
//                message.toLowerCase().contains("panic") || message.toLowerCase().contains("worry")) {
//            return responses.get("anxiety" + styleSuffix);
//        }
//
//        // Follow-up handling based on last topic
//        if (lastTopic != null && followUpQuestions.containsKey(lastTopic)) {
//            List<String> questions = followUpQuestions.get(lastTopic);
//            return questions.get(random.nextInt(questions.size()));
//        }
//
//        // If we can't determine the intent, provide a general response or ask for clarification
//        return "I'm not sure I understand what you're asking. Would you like to know about exercise, diet, sleep, stress management, or another health topic?";
//    }
//
//    private void processHealthMetrics(String message, UserHealthProfile profile) {
//        // Extract potential health metrics from message
//        String weightStr = languageProcessor.extractEntity(message, "weight");
//        if (weightStr != null) {
//            // Extract numerical value
//            Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
//            Matcher matcher = pattern.matcher(weightStr);
//            if (matcher.find()) {
//                float weight = Float.parseFloat(matcher.group(1));
//                // Convert to kg if in pounds
//                if (weightStr.contains("lb") || weightStr.contains("pound")) {
//                    weight *= 0.453592f;
//                }
//                profile.metrics.put("weight", weight);
//                metricsTracker.recordMetric(profile.userId, "weight", weight);
//            }
//        }
//
//        String heightStr = languageProcessor.extractEntity(message, "height");
//        if (heightStr != null) {
//            // Extract numerical value and convert to meters
//            Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
//            Matcher matcher = pattern.matcher(heightStr);
//            if (matcher.find()) {
//                float height = Float.parseFloat(matcher.group(1));
//                // Convert to meters if in feet or inches
//                if (heightStr.contains("ft") || heightStr.contains("foot") || heightStr.contains("feet")) {
//                    height *= 0.3048f;
//                } else if (heightStr.contains("in") || heightStr.contains("inch")) {
//                    height *= 0.0254f;
//                } else if (heightStr.contains("cm")) {
//                    height *= 0.01f;
//                }
//                profile.metrics.put("height", height);
//                metricsTracker.recordMetric(profile.userId, "height", height);
//            }
//        }
//
//        // Parse age
//        String ageStr = languageProcessor.extractEntity(message, "age");
//        if (ageStr != null) {
//            Pattern pattern = Pattern.compile("(\\d+)");
//            Matcher matcher = pattern.matcher(ageStr);
//            if (matcher.find()) {
//                int age = Integer.parseInt(matcher.group(1));
//                profile.age = age;
//            }
//        }
//    }
//
//    // Helper method to check if a message contains any of the keywords
//    private boolean containsAny(String message, String... keywords) {
//        for (String keyword : keywords) {
//            if (message.contains(keyword)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    // Personalization methods
//    public void setPreferredConversationStyle(String patientId, ConversationStyle style) {
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//        userProfiles.get(patientId).preferredStyle = style;
//    }
//
//    public void addHealthCondition(String patientId, String condition, String details) {
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//        userProfiles.get(patientId).healthConditions.put(condition.toLowerCase(), details);
//    }
//
//    public void addMedication(String patientId, String medication) {
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//        userProfiles.get(patientId).medications.add(medication);
//    }
//
//    public void setMetric(String patientId, String metricName, float value) {
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//        userProfiles.get(patientId).metrics.put(metricName, value);
//        metricsTracker.recordMetric(patientId, metricName, value);
//    }
//
//    public void setTopicPreference(String patientId, String topic, int preference) {
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//        userProfiles.get(patientId).preferences.put(topic, preference);
//    }
//
//    public void setUserName(String patientId, String name) {
//        if (!userProfiles.containsKey(patientId)) {
//            userProfiles.put(patientId, new UserHealthProfile(patientId));
//        }
//        userProfiles.get(patientId).name = name;
//    }
//
//    // Advanced Features
//    public String getPersonalizedHealthTip(String patientId) {
//        if (!userProfiles.containsKey(patientId)) {
//            return "Stay hydrated and aim for at least 30 minutes of physical activity daily.";
//        }
//
//        UserHealthProfile profile = userProfiles.get(patientId);
//
//        // Check if there are any health conditions to consider
//        if (!profile.healthConditions.isEmpty()) {
//            String condition = profile.healthConditions.keySet().iterator().next();
//
//            if (condition.contains("diabet")) {
//                return "Remember to monitor your blood glucose levels regularly and stay active. Even short walks after meals can help manage blood sugar levels.";
//            } else if (condition.contains("hypertension") || condition.contains("blood pressure")) {
//                return "Consider monitoring your blood pressure at home. Reducing sodium intake and practicing relaxation techniques can help manage blood pressure.";
//            } else if (condition.contains("heart")) {
//                return "Heart-healthy habits include regular physical activity, a diet rich in fruits, vegetables, and whole grains, and stress management.";
//            }
//        }
//
//        // Check recent topics for personalization
//        if (!profile.recentTopics.isEmpty()) {
//            String recentTopic = profile.recentTopics.get(profile.recentTopics.size() - 1);
//
//            if (recentTopic.equals("exercise")) {
//                return "Consistency is key with exercise. Find activities you enjoy and aim for at least 150 minutes of moderate activity each week.";
//            } else if (recentTopic.equals("diet")) {
//                return "Small dietary changes add up. Try adding one extra serving of vegetables to your meals today.";
//            } else if (recentTopic.equals("sleep")) {
//                return "Quality sleep is essential for health. Try to maintain a consistent sleep schedule, even on weekends.";
//            } else if (recentTopic.equals("stress")) {
//                return "Take a few minutes today for a mindfulness break. Even brief moments of present awareness can help reduce stress.";
//            } else if (recentTopic.equals("mental_health")) {
//                return "Self-compassion is a powerful tool for wellbeing. Treat yourself with the same kindness you'd offer a friend.";
//            }
//        }
//
//        // Default tips
//        String[] generalTips = {
//                "Staying hydrated supports nearly every system in your body. Aim for 8 glasses of water daily.",
//                "Adding movement to your day doesn't require a gym membership. Try walking meetings, standing while on the phone, or stretching breaks.",
//                "Eating a variety of colorful fruits and vegetables ensures you get a wide range of nutrients.",
//                "Prioritize sleep as you would any other important activity. Good sleep supports mental clarity and physical health.",
//                "Regular health check-ups can catch potential issues early. Schedule your annual physical if you haven't already.",
//                "Taking a few deep breaths when you feel stressed activates your body's relaxation response.",
//                "Social connections are powerful for health. Reach out to someone you care about today.",
//                "Small self-care practices add up. What's one thing you can do for your wellbeing today?"
//        };
//
//        return generalTips[random.nextInt(generalTips.length)];
//    }
//
//    public String getMetricInsight(String patientId, String metricType) {
//        if (!userProfiles.containsKey(patientId)) {
//            return "No health metrics data available. Would you like to start tracking your " + metricType + "?";
//        }
//
//        UserHealthProfile profile = userProfiles.get(patientId);
//
//        if (!profile.metrics.containsKey(metricType)) {
//            return "I don't have any " + metricType + " data for you yet. Would you like to record your " + metricType + " now?";
//        }
//
//        float value = profile.metrics.get(metricType);
//
//        if (metricType.equals("weight")) {
//            if (profile.metrics.containsKey("height")) {
//                float height = profile.metrics.get("height");
//                float bmi = value / (height * height);
//
//                String bmiCategory;
//                if (bmi < 18.5) {
//                    bmiCategory = "underweight";
//                } else if (bmi < 25) {
//                    bmiCategory = "healthy weight";
//                } else if (bmi < 30) {
//                    bmiCategory = "overweight";
//                } else {
//                    bmiCategory = "obese";
//                }
//
//                return String.format("Your BMI is %.1f, which falls in the %s category. %s",
//                        bmi, bmiCategory, metricsTracker.getMetricTrend(patientId, "weight", 30));
//            } else {
//                return String.format("Your current weight is %.1f kg. %s",
//                        value, metricsTracker.getMetricTrend(patientId, "weight", 30));
//            }
//        }
//
//        return "Your current " + metricType + " is " + value + ". " +
//                metricsTracker.getMetricTrend(patientId, metricType, 30);
//    }
//
//    public String getPersonalizedExercisePlan(String patientId) {
//        if (!userProfiles.containsKey(patientId)) {
//            return "I'd need to know a bit more about you to create a personalized exercise plan. " +
//                    "What types of physical activity do you enjoy, and do you have any health conditions I should consider?";
//        }
//
//        UserHealthProfile profile = userProfiles.get(patientId);
//        StringBuilder plan = new StringBuilder();
//
//        plan.append("Here's a personalized exercise suggestion based on your profile:\n\n");
//
//        // Adjust based on health conditions
//        boolean hasConditions = !profile.healthConditions.isEmpty();
//        boolean hasCardiacCondition = false;
//        boolean hasJointIssues = false;
//
//        for (String condition : profile.healthConditions.keySet()) {
//            if (condition.contains("heart") || condition.contains("cardiac") || condition.contains("blood pressure")) {
//                hasCardiacCondition = true;
//            }
//            if (condition.contains("arthritis") || condition.contains("joint") || condition.contains("back pain")) {
//                hasJointIssues = true;
//            }
//        }
//
//        if (hasCardiacCondition) {
//            plan.append("Since you have a heart-related condition, it's important to start gently and progress gradually. " +
//                    "Begin with 5-10 minutes of walking or swimming, eventually building up to 30 minutes most days of the week. " +
//                    "Always warm up and cool down properly.\n\n");
//        } else if (hasJointIssues) {
//            plan.append("For joint protection, focus on low-impact activities like swimming, cycling, or elliptical training. " +
//                    "Gentle strength training with proper form can also help support your joints.\n\n");
//        } else {
//            plan.append("A balanced exercise routine includes:\n" +
//                    "- Cardiovascular activity: 150 minutes of moderate activity weekly (like brisk walking, cycling, or swimming)\n" +
//                    "- Strength training: 2-3 sessions per week targeting major muscle groups\n" +
//                    "- Flexibility: Daily stretching or yoga to maintain range of motion\n" +
//                    "- Balance exercises: 2-3 times weekly, especially important as we age\n\n");
//        }
//
//// Add personalized recommendations based on preferences
//        if (profile.preferences.containsKey("exercise_type")) {
//            int preferenceValue = profile.preferences.get("exercise_type");
//            if (preferenceValue == 1) { // Assuming 1 means they prefer outdoor activities
//                plan.append("Since you enjoy outdoor activities, consider trail walking, hiking, or outdoor swimming when weather permits.\n\n");
//            } else if (preferenceValue == 2) { // Assuming 2 means they prefer group activities
//                plan.append("Group fitness classes might be motivating for you - consider joining a local class or online community.\n\n");
//            } else if (preferenceValue == 3) { // Assuming 3 means they prefer solo activities
//                plan.append("For your preference for independent exercise, consider setting up a home workout space or trying audio-guided workouts.\n\n");
//            }
//        }
//
//// Add time-based recommendations
//        LocalTime currentTime = LocalTime.now();
//        if (currentTime.getHour() < 12) {
//            plan.append("Morning exercise tip: A quick 10-minute routine can energize your day. Try gentle stretching followed by a brief walk.\n\n");
//        } else if (currentTime.getHour() < 17) {
//            plan.append("Afternoon exercise tip: If you're experiencing the mid-day slump, a brief activity break can boost your energy and focus.\n\n");
//        } else {
//            plan.append("Evening exercise tip: Gentle yoga or stretching in the evening can help prepare your body for restful sleep.\n\n");
//        }
//
//        plan.append("Remember to check with your healthcare provider before starting any new exercise program, especially if you have health concerns.");
//
//        return plan.toString();
//    }
//
//    public String getPersonalizedNutritionPlan(String patientId) {
//        if (!userProfiles.containsKey(patientId)) {
//            return "I'd need to know more about you to create a personalized nutrition plan. " +
//                    "Do you have any dietary restrictions, health conditions, or specific goals?";
//        }
//
//        UserHealthProfile profile = userProfiles.get(patientId);
//        StringBuilder plan = new StringBuilder();
//
//        plan.append("Here's a personalized nutrition suggestion based on your profile:\n\n");
//
//        // Adjust based on health conditions
//        boolean hasDiabetes = false;
//        boolean hasHeartDisease = false;
//        boolean hasHighBloodPressure = false;
//
//        for (String condition : profile.healthConditions.keySet()) {
//            if (condition.contains("diabet")) {
//                hasDiabetes = true;
//            }
//            if (condition.contains("heart") || condition.contains("cardiac")) {
//                hasHeartDisease = true;
//            }
//            if (condition.contains("hypertension") || condition.contains("blood pressure")) {
//                hasHighBloodPressure = true;
//            }
//        }
//
//        if (hasDiabetes) {
//            plan.append("For managing diabetes, focus on:\n" +
//                    "- Consistent carbohydrate intake at each meal\n" +
//                    "- Including protein and healthy fats with carbs to slow digestion\n" +
//                    "- Regular meal timing to help maintain stable blood sugar\n" +
//                    "- Plenty of fiber from vegetables, whole grains, and legumes\n\n");
//        }
//
//        if (hasHeartDisease || hasHighBloodPressure) {
//            plan.append("For heart health, consider:\n" +
//                    "- Reducing sodium intake (aim for less than 2,300mg daily)\n" +
//                    "- Emphasizing potassium-rich foods like bananas, potatoes, and leafy greens\n" +
//                    "- Including omega-3 rich foods like fatty fish, walnuts, and flaxseeds\n" +
//                    "- Limiting saturated fats and focusing on heart-healthy unsaturated fats\n\n");
//        }
//
//        // General healthy eating pattern
//        plan.append("A balanced eating pattern includes:\n" +
//                "- Plenty of vegetables and fruits (aim for half your plate)\n" +
//                "- Whole grains instead of refined grains\n" +
//                "- Lean protein sources (plant or animal-based)\n" +
//                "- Healthy fats from sources like olive oil, avocados, and nuts\n" +
//                "- Adequate hydration throughout the day\n\n");
//
//        // Add personalized recommendations based on preferences
//        if (profile.preferences.containsKey("diet_type")) {
//            int preferenceValue = profile.preferences.get("diet_type");
//            if (preferenceValue == 1) { // Assuming 1 means plant-based preference
//                plan.append("For your plant-based preferences, focus on complete protein combinations like beans with rice, and consider tracking B12 intake.\n\n");
//            } else if (preferenceValue == 2) { // Assuming 2 means low-carb preference
//                plan.append("For your lower-carb approach, emphasize non-starchy vegetables, adequate protein, and healthy fats for satiety.\n\n");
//            }
//        }
//
//        plan.append("Remember that small, sustainable changes are more effective than drastic ones. What's one small improvement you could make to your eating habits this week?");
//
//        return plan.toString();
//    }
//
//    public String getPersonalizedStressManagementPlan(String patientId) {
//        if (!userProfiles.containsKey(patientId)) {
//            return "I'd need to know more about you to create a personalized stress management plan. " +
//                    "What types of stress do you typically experience, and what coping strategies have you tried?";
//        }
//
//        UserHealthProfile profile = userProfiles.get(patientId);
//        StringBuilder plan = new StringBuilder();
//
//        plan.append("Here's a personalized stress management suggestion based on your profile:\n\n");
//
//        // Check for relevant health conditions
//        boolean hasAnxiety = false;
//        boolean hasDepression = false;
//        boolean hasChronicPain = false;
//
//        for (String condition : profile.healthConditions.keySet()) {
//            if (condition.contains("anxi")) {
//                hasAnxiety = true;
//            }
//            if (condition.contains("depress")) {
//                hasDepression = true;
//            }
//            if (condition.contains("pain") || condition.contains("fibromyalgia")) {
//                hasChronicPain = true;
//            }
//        }
//
//        if (hasAnxiety) {
//            plan.append("For anxiety management:\n" +
//                    "- Grounding techniques like 5-4-3-2-1 (notice 5 things you see, 4 things you feel, etc.)\n" +
//                    "- Deep breathing exercises, especially when you notice anxiety rising\n" +
//                    "- Regular physical activity to help process stress hormones\n" +
//                    "- Potentially limiting caffeine and alcohol which can worsen anxiety\n\n");
//        }
//
//        if (hasDepression) {
//            plan.append("For mood support:\n" +
//                    "- Structure and routine can be helpful, even simple daily habits\n" +
//                    "- Movement and outdoor time, even brief, can boost mood\n" +
//                    "- Social connection, even when it feels difficult\n" +
//                    "- Breaking tasks into smaller, manageable steps\n\n");
//        }
//
//        if (hasChronicPain) {
//            plan.append("For stress related to chronic pain:\n" +
//                    "- Gentle movement within your comfort level\n" +
//                    "- Pain-focused meditation and body scan practices\n" +
//                    "- Pacing activities to prevent flare-ups\n" +
//                    "- Exploring creative outlets for expression\n\n");
//        }
//
//        // General stress management
//        plan.append("Core stress management practices:\n" +
//                "- Brief mindfulness moments throughout your day (even 1-2 minutes)\n" +
//                "- Regular physical activity that you enjoy\n" +
//                "- Adequate sleep and consistent sleep schedule\n" +
//                "- Social connection and support\n" +
//                "- Setting appropriate boundaries\n\n");
//
//        // Time-based recommendations
//        LocalTime currentTime = LocalTime.now();
//        if (currentTime.getHour() < 12) {
//            plan.append("Morning stress tip: Taking a few minutes to set intentions for the day can help you feel more centered and prepared.\n\n");
//        } else if (currentTime.getHour() < 17) {
//            plan.append("Afternoon stress tip: A brief walk or stretch break can reset your nervous system during a busy day.\n\n");
//        } else {
//            plan.append("Evening stress tip: Creating a wind-down routine signals to your body that it's time to relax and prepare for rest.\n\n");
//        }
//
//        plan.append("What's one small stress management practice you could try today?");
//
//        return plan.toString();
//    }
//
//    // Data models
////    public class UserHealthProfile {
////        public String userId;
////        public String name;
////        public int age;
////        public ConversationStyle preferredStyle = ConversationStyle.INFORMATIVE;
////        public Map<String, String> healthConditions = new HashMap<>();
////        public Set<String> medications = new HashSet<>();
////        public Map<String, Float> metrics = new HashMap<>();
////        public Map<String, Integer> preferences = new HashMap<>();
////        public List<String> recentTopics = new ArrayList<>();
////
////        public UserHealthProfile(String userId) {
////            this.userId = userId;
////        }
////
////        public void addRecentTopic(String topic) {
////            if (recentTopics.size() >= 5) {
////                recentTopics.remove(0);
////            }
////            recentTopics.add(topic);
////        }
////    }
////
////    public class HealthTopic {
////        public String overview;
////        public List<String> facts = new ArrayList<>();
////        public List<String> tips = new ArrayList<>();
////        public List<String> followUps = new ArrayList<>();
////
////        public HealthTopic(String overview) {
////            this.overview = overview;
////        }
////
////        public String getRandomFact() {
////            return facts.get(random.nextInt(facts.size()));
////        }
////
////        public String getRandomTip() {
////            return tips.get(random.nextInt(tips.size()));
////        }
////
////        public String getRandomFollowUp() {
////            return followUps.get(random.nextInt(followUps.size()));
////        }
////    }
////
////    public enum ConversationStyle {
////        INFORMATIVE,
////        MOTIVATIONAL,
////        EMPATHETIC,
////        CONCISE
////    }
////
////    public enum Sentiment {
////        POSITIVE,
////        NEUTRAL,
////        NEGATIVE,
////        ANXIOUS,
////        DISTRESSED
////    }
//}

/*
package com.healthcare.aarogyanidaan;

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
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class chatbot extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int GALLERY_REQUEST_CODE = 103;
    private static final String PATIENT_ID = "current_patient_id"; // Keep as static final

    private RecyclerView chatRecyclerView;
    private ChatBotAdapter chatAdapter;
    private EditText messageInput;
    private ImageButton camera, upload;
    private ChatDatabaseHelper dbHelper;
    private ImageButton sendButton, backbutton;
    private FirebaseFirestore db;
    private HealthAIHelper aiHelper;
    private static final String PREF_NAME = "ChatbotPrefs";
    private static final String LAST_WELCOME_DATE = "last_welcome_date";
    private SharedPreferences sharedPreferences;
    private String currentPhotoPath;
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initializeComponents();
        setupAI();
        addDateMessage();
        loadSavedMessages();
        loadPatientData();
        checkPermissions();

        sendButton.setOnClickListener(v -> sendMessage());
        camera.setOnClickListener(v -> openCamera());
        upload.setOnClickListener(v -> openGallery());
    }

    private void initializeComponents() {
        db = FirebaseFirestore.getInstance();
        dbHelper = new ChatDatabaseHelper(this);
        // Pass this as Context, not as chatbot
        aiHelper = new HealthAIHelper(getApplicationContext(), HealthAIHelper.ConversationStyle.FORMAL);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backbutton = findViewById(R.id.backbutton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        camera = findViewById(R.id.camera);
        upload = findViewById(R.id.upload);

        menuButton.setOnClickListener(v -> showPopupMenu(v));
        backbutton.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        chatAdapter = new ChatBotAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void loadPatientData() {
        db.collection("patienthealthdata")
                .document(PATIENT_ID) // Use the static PATIENT_ID
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> healthData = documentSnapshot.getData();
                        // First set patient data
                        Map<String, String> patientData = new HashMap<>();
                        patientData.put("patient_name", (String) healthData.get("name"));
                        patientData.put("patient_email", (String) healthData.get("email"));
                        // ... add other patient fields as needed
                        aiHelper.setPatientData(PATIENT_ID, patientData);

                        // Then set health data
                        aiHelper.setPatientHealthData(PATIENT_ID, healthData);

                        if (shouldShowWelcomeToday()) {
                            String welcomeMessage = generateWelcomeMessage(healthData);
                            chatAdapter.addMessage(new Message(welcomeMessage, Message.SENT_BY_BOT));
                            dbHelper.saveMessage(new Message(welcomeMessage, Message.SENT_BY_BOT), PATIENT_ID);
                            saveLastWelcomeDate();
                        }
                    }
                });
    }
    private boolean shouldShowWelcomeToday() {
        String lastDateStr = sharedPreferences.getString(LAST_WELCOME_DATE, "");

        if (lastDateStr.isEmpty()) {
            return true; // First time opening the app
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            Date lastDate = sdf.parse(lastDateStr);
            Date today = Calendar.getInstance().getTime();

            // Compare only year, month and day
            Calendar lastCal = Calendar.getInstance();
            lastCal.setTime(lastDate);
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);

            return lastCal.get(Calendar.YEAR) != todayCal.get(Calendar.YEAR) ||
                    lastCal.get(Calendar.DAY_OF_YEAR) != todayCal.get(Calendar.DAY_OF_YEAR);

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private void saveLastWelcomeDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String today = sdf.format(new Date());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LAST_WELCOME_DATE, today);
        editor.apply();
    }

    private String generateWelcomeMessage(Map<String, Object> healthData) {
        String name = (String) healthData.get("name");

        // Get current time of day
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay < 12) {
            greeting = "Good morning";
        } else if (hourOfDay < 17) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        return greeting + " " + name + "! I'm your AI health assistant. I can help you track your health, answer your questions, and provide personalized advice based on your health records. How can I assist you today?";
    }

    private void setupAI() {
        try {
            File modelFile = loadModelFile();
            tflite = new Interpreter(modelFile);
            aiHelper.setInterpreter(tflite);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private File loadModelFile() throws IOException {
        String modelPath = "health_model.tflite";
        File modelFile = new File(getCacheDir(), modelPath);

        if (!modelFile.exists()) {
            InputStream inputStream = getAssets().open(modelPath);
            FileOutputStream outputStream = new FileOutputStream(modelFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.close();
        }
        return modelFile;
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
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
        Message message = new Message(caption, Message.SENT_BY_USER, mediaPath, 1);
        chatAdapter.addMessage(message);
        dbHelper.saveMessage(message, PATIENT_ID);

        // Scroll to bottom
        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) return;

        Message userMessage = new Message(messageText, Message.SENT_BY_USER);
        chatAdapter.addMessage(userMessage);
        messageInput.setText("");

        // Process message with AI using PATIENT_ID
        processMessageWithAI(messageText);

        dbHelper.saveMessage(userMessage, PATIENT_ID);
        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void processMessageWithAI(String userMessage) {
        String response = aiHelper.processMessage(PATIENT_ID, userMessage);
        Message botMessage = new Message(response, Message.SENT_BY_BOT);
        chatAdapter.addMessage(botMessage);
        dbHelper.saveMessage(botMessage, PATIENT_ID);
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
        // Implement settings activity navigation
        // Intent settingsIntent = new Intent(this, SettingsActivity.class);
        // startActivity(settingsIntent);

        // For now, just show a toast
        Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("AarogyaAssist: \nA AI health assistant")
                .setIcon(R.drawable.chatbot)
                .setMessage("This AI assistant can help with:\n\n" +
                        "• 💡 Answering general health questions\n\n" +
                        "• 📊 Providing personalized health insights based on your records\n\n" +
                        "• 📖 Explaining medical terms\n\n" +
                        "• ⏰ Sending medication reminders\n\n" +
                        "⚠️ The assistant cannot diagnose conditions or replace professional medical advice.\n" +
                        "Always consult your healthcare provider for medical concerns. 👩‍⚕️👨‍⚕️")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showClearChatConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat History")
                .setMessage("Are you sure you want to clear all chat history? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    // Clear chat from both adapter and database
                    chatAdapter.clearMessages();
                    dbHelper.clearMessages(PATIENT_ID);

                    // Add date message back after clearing
                    addDateMessage();

                    Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addDateMessage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        String dateString = dateFormat.format(new Date());

        Message dateMessage = new Message(dateString, Message.DATE_SEPARATOR);
        chatAdapter.addMessage(dateMessage);
        dbHelper.saveMessage(dateMessage, PATIENT_ID);
    }

    private void loadSavedMessages() {
        List<Message> savedMessages = dbHelper.getMessages(PATIENT_ID);
        for (Message message : savedMessages) {
            chatAdapter.addMessage(message);
        }
        if (chatAdapter.getItemCount() > 0) {
            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
 */