package com.collage.notes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.collage.notes.data.DatabaseHelper;
import com.collage.notes.model.Label;
import com.collage.notes.model.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private NoteAdapter noteAdapter;
    private LabelAdapter labelAdapter;
    private int labelId = -1;
    private List<Label> allLabels = new ArrayList<>();
    private Label focusedLabel = null;
    private EditText focusedEditLabelText = null;
    private DatabaseHelper databaseHelper;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    public EditText searchBar;
    public Button addButton;
    public Button editLabelButton;

    private void init() {
        databaseHelper = new DatabaseHelper(this);

        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        searchBar = findViewById(R.id.search_bar);
        addButton = findViewById(R.id.add_button);
        editLabelButton = findViewById(R.id.edit_label_button);

        setData();
        setListener();
    }

    private void setData() {
        noteAdapter = new NoteAdapter(databaseHelper.getNotes("", labelId));
        RecyclerView noteView = findViewById(R.id.note_view);
        noteView.setLayoutManager(new LinearLayoutManager(this));
        noteView.setAdapter(noteAdapter);

        updateAllLabels();
        labelAdapter = new LabelAdapter(allLabels);
        RecyclerView labelView = findViewById(R.id.label_view);
        labelView.setLayoutManager(new LinearLayoutManager(this));
        labelView.setAdapter(labelAdapter);

        setTitle(R.string.all_labels);
    }

    private void updateAllLabels(){
        allLabels.clear();
        // add "all label" as first label
        allLabels.add(new Label(-1, getString(R.string.all_labels)));
        allLabels.addAll(databaseHelper.getAllLabels());
    }

    private void setListener() {
        noteAdapter.setOnItemClickListener(note -> {
            // edit note
            Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
            intent.putExtra("note", note);
            startActivity(intent);
        });

        labelAdapter.setOnItemClickListener(label -> {
            // label filter
            labelId = label.getId();
            noteAdapter.updateNotes(databaseHelper.getNotes(searchBar.getText().toString(), labelId));
            // set action bar title to selected label name
            setTitle(label.getName());
            drawerLayout.close();
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                startActivity(intent);
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private int delay = getResources().getInteger(R.integer.search_delay); // milliseconds

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                // delay trigger
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                // fix "Only the original thread that created a view hierarchy can touch its views" error
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        noteAdapter.updateNotes(databaseHelper.getNotes(searchBar.getText().toString(), labelId));
                                    }
                                });
                            }
                        },
                        delay
                );
            }
        });

        editLabelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inflate custom dialog layout
                View editLabelDialog = getLayoutInflater().inflate(R.layout.edit_label_dialog, null);
                LinearLayout editLabelContainer = editLabelDialog.findViewById(R.id.edit_label_container);
                // add "new label" as first label
                createNewLabel(editLabelContainer);
                // add labels to list
                for (Label label : allLabels) {
                    // filter out "all label"
                    if (label.getId() == -1)
                        continue;
                    createEditLabel(editLabelContainer, label);
                }

                // create dialog
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.edit_label_dialog_title)
                        .setView(editLabelDialog)
                        .setPositiveButton(R.string.ok, null)
                        .create();

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // update or create label when dialog is closed
                        if(focusedLabel != null){
                            String trimmedEditLabelText = focusedEditLabelText.getText().toString().trim();
                            if(!Objects.equals(focusedLabel.getName(), trimmedEditLabelText)) {
                                if (focusedLabel.getId() == 0) {
                                    focusedLabel.setName(trimmedEditLabelText);
                                    int newLabelId = (int) databaseHelper.createLabel(focusedLabel);
                                    if (newLabelId != -1) {
                                        // create label success
                                        createEditLabel(editLabelContainer, databaseHelper.getLabelById(newLabelId));
                                        // refresh UI
                                        focusedEditLabelText.setText("");
                                    }
                                } else {
                                    focusedLabel.setName(trimmedEditLabelText);
                                    if (databaseHelper.updateLabel(focusedLabel) != 0)
                                        // update label success
                                        focusedEditLabelText.setText(trimmedEditLabelText);
                                }
                            }
                        }
                        // refresh UI
                        updateAllLabels();
                        labelAdapter.notifyDataSetChanged();
                        noteAdapter.updateNotes(databaseHelper.getNotes(searchBar.getText().toString(), labelId));
                    }
                });

                // show dialog
                dialog.show();
            }
        });
    }

    private void createEditLabel(LinearLayout editLabelContainer, Label label){
        View editLabelItem = getLayoutInflater().inflate(R.layout.edit_label_item, null);
        EditText editLabelText = editLabelItem.findViewById(R.id.edit_label_text);
        ImageButton deleteLabelButton = editLabelItem.findViewById(R.id.delete_label_button);

        // set label data
        editLabelText.setText(label.getName());
        editLabelText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String trimmedEditLabelText = editLabelText.getText().toString().trim();
                if (hasFocus) {
                    // "edit label text" gained focus
                    focusedLabel = label;
                    focusedEditLabelText = editLabelText;
                }
                if (!hasFocus && !trimmedEditLabelText.equals("")) {
                    // "edit label text" lost focus
                    label.setName(trimmedEditLabelText);
                    if (databaseHelper.updateLabel(label) != 0)
                        // update label success
                        editLabelText.setText(trimmedEditLabelText);
                }
            }
        });

        deleteLabelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // delete label dialog
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.delete_title)
                        .setMessage(R.string.delete_hint)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // delete label in db
                                databaseHelper.deleteLabel(label.getId());
                                // remove label
                                editLabelContainer.removeView(editLabelItem);
                                allLabels.remove(label);
                                focusedLabel = null;
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        // add label to list
        editLabelContainer.addView(editLabelItem);
    }

    private void createNewLabel(LinearLayout editLabelContainer) {
        View editLabelItem = getLayoutInflater().inflate(R.layout.edit_label_item, null);
        EditText editLabelText = editLabelItem.findViewById(R.id.edit_label_text);
        ImageButton deleteLabelButton = editLabelItem.findViewById(R.id.delete_label_button);
        // remove "new label" delete button
        deleteLabelButton.setVisibility(View.INVISIBLE);

        // set label data
        editLabelText.setHint(R.string.new_label);
        editLabelText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Label newLabel = new Label(0, "");
                if (hasFocus) {
                    // "edit label text" gained focus
                    focusedLabel = newLabel;
                    focusedEditLabelText = editLabelText;
                }
                String trimmedEditLabelText = editLabelText.getText().toString().trim();
                if (!hasFocus && !trimmedEditLabelText.equals("")) {
                    // "edit label text" lost focus
                    newLabel.setName(trimmedEditLabelText);
                    int newLabelId = (int) databaseHelper.createLabel(newLabel);
                    if (newLabelId != -1) {
                        // create label success
                        createEditLabel(editLabelContainer, databaseHelper.getLabelById(newLabelId));
                        // refresh UI
                        editLabelText.setText("");
                    }
                }
            }
        });
        // add new label to list at first place
        editLabelContainer.addView(editLabelItem, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        // update notes after edit note activity
        noteAdapter.updateNotes(databaseHelper.getNotes(searchBar.getText().toString(), labelId));
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}