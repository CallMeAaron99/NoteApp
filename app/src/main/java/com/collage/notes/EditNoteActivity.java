package com.collage.notes;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.collage.notes.data.DatabaseHelper;
import com.collage.notes.model.Label;
import com.collage.notes.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class EditNoteActivity extends AppCompatActivity {

    private TextView labelDropdown;
    private DatabaseHelper databaseHelper;
    private Note note = null;
    private List<Label> allLabels = new ArrayList<>();
    private Set<Label> selectedLabel = new HashSet<>();
    private boolean[] checkedLabels = null;
    private CharSequence[] labelNames;
    private boolean changeFlag = false;
    public EditText editContent;
    public ImageButton deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_note_layout);

        databaseHelper = new DatabaseHelper(this);
        allLabels = databaseHelper.getAllLabels();

        // get all the label name
        labelNames = allLabels.stream().map(Label::getName).toArray(CharSequence[]::new);

        editContent = findViewById(R.id.edit_content);
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
        });
        labelDropdown = findViewById(R.id.label_dropdown);

        if (getIntent().getExtras() != null) {
            // editing note
            // get note from main activity
            note = (Note) getIntent().getSerializableExtra("note");
            editContent.setText(note.getContent());

            // show delete button
            setDeleteButton();
            TextView dateText = findViewById(R.id.date_text);

            // format and show created at date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String dateStr = dateFormat.format(note.getCreatedAt());
            dateText.setText(dateStr);

            // show selected label
            selectedLabel.addAll(note.getLabels());
            checkedLabels = new boolean[allLabels.size()];
            for (int i = 0; i < allLabels.size(); i++) {
                if (note.getLabels().contains(allLabels.get(i)))
                    checkedLabels[i] = true;
            }
        } else {
            // creating note
            note = new Note();
        }

        setLabelDropdown();
    }

    @Override
    protected void onPause() {
        if (note == null) {
            // note deleted
            super.onPause();
            return;
        }

        if (note.getId() == 0) {
            changeFlag = !editContent.getText().toString().equals("");

            if (changeFlag) {
                // create note
                note.setContent(editContent.getText().toString());
                databaseHelper.createNote(note);
            }
        } else {
            if (!Objects.equals(note.getContent(), editContent.getText().toString()))
                // content changed
                changeFlag = true;

            if (changeFlag) {
                // update note
                note.setContent(editContent.getText().toString());
                databaseHelper.updateNote(note);
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }

    private void setLabelDropdown() {
        labelDropdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // "drop down label" dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(EditNoteActivity.this);
                builder.setTitle(R.string.label_dropdown_hint);
                builder.setCancelable(false);

                builder.setMultiChoiceItems(labelNames, checkedLabels, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        // using hashset instead of arraylist to improve time complexity to O(1)
                        if (b)
                            selectedLabel.add(allLabels.get(i));
                        else
                            selectedLabel.remove(allLabels.get(i));
                    }
                });

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        note.setLabels(new ArrayList<>(selectedLabel));
                        changeFlag = true;
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.setNeutralButton(R.string.clear_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // remove all selection
                        selectedLabel.clear();
                        note.getLabels().clear();
                        checkedLabels = null;
                        changeFlag = true;
                    }
                });
                // show dialog
                builder.show();
            }
        });
    }

    private void setDeleteButton() {
        deleteButton = findViewById(R.id.delete_button);
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // "delete note" dialog
                new AlertDialog.Builder(EditNoteActivity.this)
                        .setTitle(R.string.delete_title)
                        .setMessage(R.string.delete_hint)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // delete note
                                databaseHelper.deleteNote(note.getId());
                                note = null;
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }
}