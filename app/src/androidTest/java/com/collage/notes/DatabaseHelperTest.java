package com.collage.notes;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.collage.notes.data.DatabaseHelper;
import com.collage.notes.model.Label;
import com.collage.notes.model.Note;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private DatabaseHelper db;

    @Before
    public void createDb() {
        db = new DatabaseHelper(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void testCreateLabel() {
        Label label = new Label();
        label.setName("赵东");
        long labelId = db.createLabel(label);
        assertTrue(labelId != -1);
    }

    @Test
    public void testGetAllLabels() {
        List<Label> labels = db.getAllLabels();
        for (Label label : labels) {
            System.out.println(label); // output in Logcat
        }
    }

    @Test
    public void testCreateNote() {

        Note note = new Note();
        note.setContent("赵东");

        Label label = new Label();
        label.setId(2);
        note.setLabels(Collections.singletonList(label));

        long noteId = db.createNote(note);
        assertTrue(noteId != -1);
    }

    @Test
    public void testSearchNotes() {
        List<Note> notes = db.getNotes("", -1);
        for (Note note : notes) {
            System.out.println(note);
        }
    }

    @Test
    public void testDeleteNote() {
        db.deleteNote(1);
    }
}
