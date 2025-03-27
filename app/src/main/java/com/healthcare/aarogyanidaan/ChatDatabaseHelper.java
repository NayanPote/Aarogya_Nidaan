package com.healthcare.aarogyanidaan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_MESSAGES = "messages";

    // Column names
    private static final String KEY_ID = "id";
    private static final String KEY_SENDER = "sender";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_TIME = "time";
    private static final String KEY_MEDIA_PATH = "media_path";
    private static final String KEY_TYPE = "type";

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_SENDER + " TEXT,"
                + KEY_MESSAGE + " TEXT,"
                + KEY_TIME + " TEXT,"
                + KEY_MEDIA_PATH + " TEXT,"
                + KEY_TYPE + " INTEGER DEFAULT 0)";

        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    // Add a regular message
    public void addMessage(chatbot.ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_SENDER, message.getSender());
            values.put(KEY_MESSAGE, message.getMessage());
            values.put(KEY_TIME, message.getTime());
            values.put(KEY_MEDIA_PATH, message.getMediaPath());
            values.put(KEY_TYPE, message.sentBy);

            db.insert(TABLE_MESSAGES, null, values);
        } catch (Exception e) {
            Log.e("ChatDB", "Error adding message", e);
        } finally {
            db.close();
        }
    }

    // Add a date separator
    public void addDateSeparator(chatbot.ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_MESSAGES,
                    new String[]{KEY_ID},
                    KEY_MESSAGE + " = ? AND " + KEY_TYPE + " = ?",
                    new String[]{message.getMessage(), String.valueOf(chatbot.ChatMessage.DATE_SEPARATOR)},
                    null, null, null);

            boolean exists = (cursor != null && cursor.getCount() > 0);

            if (!exists) {
                ContentValues values = new ContentValues();
                values.put(KEY_SENDER, "");
                values.put(KEY_MESSAGE, message.getMessage());
                values.put(KEY_TIME, "");
                values.put(KEY_MEDIA_PATH, (String) null);
                values.put(KEY_TYPE, chatbot.ChatMessage.DATE_SEPARATOR);

                db.insert(TABLE_MESSAGES, null, values);
            }
        } catch (Exception e) {
            Log.e("ChatDB", "Error adding date separator", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Get all messages
    public List<chatbot.ChatMessage> getAllMessages() {
        List<chatbot.ChatMessage> messageList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + KEY_ID + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    String sender = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SENDER));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MESSAGE));
                    String time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIME));
                    String mediaPath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEDIA_PATH));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_TYPE));

                    chatbot.ChatMessage chatMessage;

                    if (type == chatbot.ChatMessage.DATE_SEPARATOR) {
                        chatMessage = new chatbot.ChatMessage(message);
                    } else if (mediaPath != null) {
                        chatMessage = new chatbot.ChatMessage(sender, message, time, mediaPath);
                    } else {
                        chatMessage = new chatbot.ChatMessage(sender, message, time);
                    }

                    chatMessage.sentBy = type;
                    messageList.add(chatMessage);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("ChatDB", "Error loading messages", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return messageList;
    }

    // Check if today's date separator already exists
    public boolean todayDateSeparatorExists(String todayDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_MESSAGES +
                            " WHERE " + KEY_MESSAGE + " = ? AND " + KEY_TYPE + " = ?",
                    new String[]{todayDate, String.valueOf(chatbot.ChatMessage.DATE_SEPARATOR)});

            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return exists;
    }

    public void clearAllMessages() {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.delete(TABLE_MESSAGES, null, null);
        } catch (Exception e) {
            e.printStackTrace(); //  Log the exception for debugging
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }
}
