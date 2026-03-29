package com.laetienda.model.schema;

import com.laetienda.lib.options.DbGroupPolicy;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
public class DbGroup {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull @ManyToOne
    @JoinColumn(name = "item_id")
    private DbItem item;

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
}
