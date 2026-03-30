package com.laetienda.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners({AuditingEntityListener.class})
@Table(name="ITEM")
public abstract class DbItem {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Column(name="owner", nullable = false)
    private String owner;

    @ElementCollection
    @CollectionTable(name="ITEM_EDITOR", joinColumns = @JoinColumn(name = "ITEM_ID"))
    @Column(name = "editor")
    private List<String> editors;

    @ElementCollection
    @CollectionTable(name = "ITEM_READER", joinColumns = @JoinColumn(name = "ITEM_ID"))
    @Column(name = "reader")
    private List<String> readers;

    @ManyToMany(mappedBy = "items")
    private Set<DbGroup> readersGroups;

    @ManyToMany(mappedBy = "items")
    private Set<DbGroup> editorGroups;

    @CreatedDate
    @Column(name = "created", insertable = true, updatable = false)
    private LocalDateTime created;

    @LastModifiedDate
    @Column(name = "modified", insertable = false, updatable = true)
    private LocalDateTime modified;

    public DbItem(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getOwner() {
        return owner;
    }

    public void setOwner(@NotNull String owner) {
        this.owner = owner;
    }

    public List<String> getEditors() {
        return editors;
    }

    public List<String> getReaders() {
        return readers;
    }

    public void setEditors(List<String> editors) {
        this.editors = editors;
    }

    public void setReaders(List<String> readers) {
        this.readers = readers;
    }

    public DbItem addReader(String reader){
        if(readers == null){
            readers = new ArrayList<String>();
        }

        if(!readers.contains(reader)){
            readers.add(reader);
        }

        return this;
    }

    public DbItem addEditor(String editor){
        if(editors == null){
            editors = new ArrayList<String>();
        }

        if(!editors.contains(editor)){
            editors.add(editor);
        }

        return this;
    }

    public DbItem removeReader(String username){
        if(readers != null && readers.contains(username)){
            readers.remove(username);
        }
        return this;
    }

    public DbItem removeEditor(String username){
        if(editors != null && editors.contains(username)){
            editors.remove(username);
        }
        return this;
    }

    public Set<DbGroup> getReadersGroups() {
        return readersGroups;
    }

    public void addReaderGroup(DbGroup group) {
        if(readersGroups == null){
            readersGroups = new HashSet<DbGroup>();
        };

        group.addItem(this);
        readersGroups.add(group);
    }

    public void removeReaderGroup(DbGroup group) {
        if(readersGroups != null){
            readersGroups.remove(group);
        }
    }

    public Set<DbGroup> getEditorGroups() {
        return editorGroups;
    }

    public void addEditorGroup(DbGroup group) {
        if(editorGroups == null){
            editorGroups = new HashSet<>();
        }

        group.addItem(this);
        editorGroups.add(group);
    }

    public void removeEditorGroup(DbGroup group) {
        if(editorGroups != null){
            editorGroups.remove(group);
        }
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getModified() {
        return modified;
    }
}
