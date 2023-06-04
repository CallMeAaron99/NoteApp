package com.collage.notes.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.collage.notes.model.Image;
import com.collage.notes.model.Label;
import com.collage.notes.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "note_db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE note (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE label (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE note_label (note_id INTEGER, label_id INTEGER, FOREIGN KEY (note_id) REFERENCES note (id), FOREIGN KEY (label_id) REFERENCES label (id))");
        db.execSQL("CREATE TABLE image (id INTEGER PRIMARY KEY AUTOINCREMENT, note_id INTEGER, file_path TEXT NOT NULL, FOREIGN KEY (note_id) REFERENCES note (id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // handle database upgrade here
    }

    public long createNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("content", note.getContent());
        values.put("created_at", System.currentTimeMillis());

        long id = db.insert("note", null, values);

        // insert labels
        for (Label label : note.getLabels()) {
            createNoteLabel(id, label.getId());
        }

        // insert images
        /* TODO
        for (Image image : note.getImages()) {
            createNoteImage(id, image);
        }*/

        return id;
    }

    private void createNoteLabel(long noteId, int labelId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("note_id", noteId);
        values.put("label_id", labelId);

        db.insert("note_label", null, values);
    }

    private void createNoteImage(long noteId, Image image) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("note_id", noteId);
        values.put("file_path", image.getFilePath());

        db.insert("image", null, values);
    }

    private boolean labelExist(Label label){
        SQLiteDatabase db = this.getReadableDatabase();

        // check if label with same name already exists
        String selectQuery = "SELECT * FROM label WHERE name = ? AND id != ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{label.getName(), String.valueOf(label.getId())});
        boolean isExist = cursor.moveToFirst();
        cursor.close();
        return isExist;
    }

    public long createLabel(Label label) {

        if(labelExist(label))
            return -1;

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", label.getName());

        return db.insert("label", null, values);
    }

    @SuppressLint("Range")
    public Note getNoteById(int noteId) {
        Note note = null;

        String selectQuery = "SELECT * FROM note WHERE id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(noteId)});

        if (cursor.moveToFirst()) {
            note = new Note();
            note.setId(cursor.getInt(cursor.getColumnIndex("id")));
            note.setContent(cursor.getString(cursor.getColumnIndex("content")));
            note.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex("created_at"))));
        }

        cursor.close();

        if (note != null) {
            // populate labels and images
            note.setLabels(getNoteLabels(note.getId()));
            // TODO note.setImages(getNoteImages(note.getId()));
        }

        return note;
    }

    @SuppressLint("Range")
    public List<Note> getNotes(String contentQuery, int labelId) {
        List<Note> notes = new ArrayList<>();

        String selectQuery = "SELECT DISTINCT note.id, note.content, note.created_at FROM note " +
                "LEFT JOIN note_label ON note.id = note_label.note_id " +
                "LEFT JOIN label ON note_label.label_id = label.id WHERE 1 = 1";

        List<String> args = new ArrayList<>();

        if (contentQuery != null && !contentQuery.isEmpty()) {
            selectQuery += " AND note.content LIKE ?";
            args.add("%" + contentQuery + "%");
        }

        if (labelId != -1) {
            selectQuery += " AND label.id = ?";
            args.add(String.valueOf(labelId));
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, args.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndex("id")));
                note.setContent(cursor.getString(cursor.getColumnIndex("content")));
                note.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndex("created_at"))));

                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();

        // populate labels and images
        for (Note note : notes) {
            note.setLabels(getNoteLabels(note.getId()));
            // TODO note.setImages(getNoteImages(note.getId()));
        }

        return notes;
    }

    @SuppressLint("Range")
    private List<Label> getNoteLabels(int noteId) {
        List<Label> labels = new ArrayList<>();

        String selectQuery = "SELECT label.id, label.name FROM label " +
                "INNER JOIN note_label ON label.id = note_label.label_id " +
                "WHERE note_label.note_id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(noteId)});

        if (cursor.moveToFirst()) {
            do {
                Label label = new Label();
                label.setId(cursor.getInt(cursor.getColumnIndex("id")));
                label.setName(cursor.getString(cursor.getColumnIndex("name")));

                labels.add(label);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return labels;
    }

    @SuppressLint("Range")
    private List<Image> getNoteImages(int noteId) {
        List<Image> images = new ArrayList<>();

        String selectQuery = "SELECT * FROM image WHERE note_id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(noteId)});

        if (cursor.moveToFirst()) {
            do {
                Image image = new Image();
                image.setId(cursor.getInt(cursor.getColumnIndex("id")));
                image.setNoteId(cursor.getInt(cursor.getColumnIndex("note_id")));
                image.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));

                images.add(image);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return images;
    }

    @SuppressLint("Range")
    public Label getLabelById(int labelId){
        Label label = null;

        String selectQuery = "SELECT * FROM label WHERE id = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(labelId)});
        if(cursor.moveToFirst()){
            label = new Label();
            label.setId(cursor.getInt(cursor.getColumnIndex("id")));
            label.setName(cursor.getString(cursor.getColumnIndex("name")));
        }

        cursor.close();

        return label;
    }

    @SuppressLint("Range")
    public List<Label> getAllLabels() {
        List<Label> labels = new ArrayList<>();

        String selectQuery = "SELECT * FROM label";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Label label = new Label();
                label.setId(cursor.getInt(cursor.getColumnIndex("id")));
                label.setName(cursor.getString(cursor.getColumnIndex("name")));

                labels.add(label);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return labels;
    }

    public int updateLabel(Label label) {
        if(labelExist(label))
            return 0;

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", label.getName());

        return db.update("label", values, "id = ?", new String[]{String.valueOf(label.getId())});
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("content", note.getContent());
        values.put("created_at", System.currentTimeMillis());

        // update note
        int count = db.update("note", values, "id = ?", new String[]{String.valueOf(note.getId())});

        // delete all labels for the note
        deleteNoteLabels(note.getId());

        // insert labels
        for (Label label : note.getLabels()) {
            createNoteLabel(note.getId(), label.getId());
        }

        // delete all images for the note
        // TODO deleteNoteImages(note.getId());

        // insert images
        /* TODO
            for (Image image : note.getImages()) {
            createNoteImage(note.getId(), image);
        }*/

        return count;
    }

    private void deleteNoteLabels(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("note_label", "note_id = ?", new String[]{String.valueOf(noteId)});
    }

    private void deleteNoteImages(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("image", "note_id = ?", new String[]{String.valueOf(noteId)});
    }

    private void deleteLabelNotes(int labelId){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("note_label", "label_id = ?", new String[]{String.valueOf(labelId)});
    }

    public void deleteNote(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // delete note
        db.delete("note", "id = ?", new String[]{String.valueOf(noteId)});

        // delete all labels for the note
        deleteNoteLabels(noteId);

        // delete all images for the note
        // TODO deleteNoteImages(noteId);
    }

    public void deleteLabel(int labelId){
        SQLiteDatabase db = this.getWritableDatabase();

        // delete label
        db.delete("label", "id = ?", new String[]{String.valueOf(labelId)});

        // delete all notes for the label
        deleteLabelNotes(labelId);
    }
}