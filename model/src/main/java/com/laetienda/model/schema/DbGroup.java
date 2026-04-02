package com.laetienda.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laetienda.lib.options.DbGroupPolicy;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
public class DbGroup {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Size(min=1, max = 32)
    @Column(unique = true, nullable = false)
    private String name;

    @JsonIgnore
    @ManyToMany(
            mappedBy = "readerGroups"
    )
    private Set<DbItem> readerItems;

    @JsonIgnore
    @ManyToMany(
            mappedBy = "editorGroups"
    )
    private Set<DbItem> editorItems;

    @NotNull
    private String owner;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DbGroupPolicy policy;

    @ElementCollection
    @CollectionTable(name="ITEM_GROUP_MEMBER", joinColumns = @JoinColumn(name = "ITEM_GROUP_ID"))
    private Set<String> members;

    public DbGroup() {

    }

    public DbGroup(String name){
        this.setName(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public DbGroupPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(DbGroupPolicy policy) {
        this.policy = policy;
    }

    public Set<String> getMembers() {
        if(members == null) {
            members = new HashSet<>();
        }
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public void addMember(String member){
        if(members == null){
            members = new HashSet<String>();
        }

        members.add(member);
    }

    public void removeMember(String member){
        if(members != null){
            members.remove(member);
        }
    }

    public Set<DbItem> getReaderItems() {
        return readerItems;
    }

    public void addReaderItem(DbItem item) {
        if(readerItems == null){
            readerItems = new HashSet<>();
        }

        readerItems.add(item);
    }

    public Set<DbItem> getEditorItems() {
        return editorItems;
    }

    public void addEditorItem(DbItem item) {
        if(editorItems == null){
            editorItems = new HashSet<>();
        }

        editorItems.add(item);
    }
}
