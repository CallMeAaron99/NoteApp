package com.collage.notes.model;

import java.io.Serializable;

public class Image implements Serializable {
    private int id;
    private int noteId;
    private String filePath;

    public Image() {
    }

    public Image(int id, int noteId, String filePath) {
        this.id = id;
        this.noteId = noteId;
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
