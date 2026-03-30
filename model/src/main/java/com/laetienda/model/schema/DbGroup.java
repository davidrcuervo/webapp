package com.laetienda.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.laetienda.lib.options.DbGroupPolicy;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.checkerframework.common.aliasing.qual.Unique;

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
    @NotNull
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "schema_item_groups",
            joinColumns = @JoinColumn(name="group_id"),
            inverseJoinColumns = @JoinColumn(name="item_id")
    )
    private Set<DbItem> items;

    @NotNull
    private String owner;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DbGroupPolicy policy;

    @ElementCollection
    @CollectionTable(name="ITEM_GROUP_MEMBER", joinColumns = @JoinColumn(name = "ITEM_GROUP_ID"))
    private Set<String> members;

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

    public Set<DbItem> getItems() {
        if(items == null){
            items = new HashSet<>();
        }
        return items;
    }

    public void addItem(DbItem item) {
        if(items == null){
            items = new HashSet<>();
        }

        items.add(item);
    }

    public void removeItem(DbItem item) {
        if(items != null){
            items.remove(item);
        }
    }
}
