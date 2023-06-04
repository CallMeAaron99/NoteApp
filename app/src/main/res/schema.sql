CREATE DATABASE note_db;

USE note_db;

CREATE TABLE note (
    id INT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE label (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE note_label (
    note_id INT,
    label_id INT,
    FOREIGN KEY (note_id) REFERENCES note (id),
    FOREIGN KEY (label_id) REFERENCES label (id)
);

CREATE TABLE image (
    id INT PRIMARY KEY AUTO_INCREMENT,
    note_id INT,
    file_path TEXT NOT NULL,
    FOREIGN KEY (note_id) REFERENCES note (id)
);