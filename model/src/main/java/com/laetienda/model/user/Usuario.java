package com.laetienda.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laetienda.lib.annotation.HtmlForm;
import com.laetienda.lib.annotation.HtmlInput;
import com.laetienda.lib.interfaces.Forma;
import com.laetienda.lib.options.HtmlInputType;
//import org.springframework.ldap.odm.annotations.*;

import javax.naming.Name;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

//@Entry(
//        base = "ou=people",
//        objectClasses = {"person", "inetOrgPerson", "top"}
//)
@HtmlForm(name="usuario", url = "/user/signup.html")
final public class Usuario implements Forma {

//    @Id
    @JsonIgnore
    private Name id;

    @NotNull @Size(min=5, max=64)
    @HtmlInput(label = "Username", placeholder = "Place the username", style_size="col-md-12")
//    @Attribute(name="uid")
//    @DnAttribute(value = "uid", index=1)
    private String username;

    @NotNull @Size(max=20)
    @HtmlInput(label = "First Name", placeholder = "Insert your first name", style_size="col-md-4")
//    @Attribute(name = "cn")
    private String firstname;

    @Size(max=20)
    @HtmlInput(label = "Middle Name", placeholder = "Insert your middle name", required = false, style_size="col-md-4")
//    @Attribute(name ="givenName")
    private String middlename;

    @NotNull @Size(max=20)
    @HtmlInput(label = "Last Name", placeholder = "Please insert your last name", style_size="col-md-4")
//    @Attribute(name = "sn")
    private String lastname;

    @NotNull @Size(max=254) @Email
    @HtmlInput(label = "eMail address", placeholder = "Please insert your email address", style_size="col-md-12")
//    @Attribute(name = "mail")
    private String email;

    @Size(min=8, max=64)
    @HtmlInput(type= HtmlInputType.PASSWORD, label = "Password", placeholder = "Please insert your password", style_size="col-md-6")
//    @Attribute(name = "userPassword")
    private String password;

    @Size(min=8, max=64)
    @HtmlInput(type= HtmlInputType.PASSWORD, label = "Re-enter password", placeholder = "Please confirm your password", style_size="col-md-6")
//    @Transient
    private String password2;

    @NotNull
    private boolean emailVerified;

    @JsonIgnore
//    @Attribute(name="labeledURI")
    private String token;

//    @Transient
    private String encToken;

//    @Transient
    @JsonIgnore
    private boolean isNew = false;
    public Usuario() {

    }

    public Usuario(String username, String password){
        this.username = username;
        this.password = password;
    }

    public Usuario(String username, String firstname, String middlename, String lastname, String email, boolean emailVerified, String password, String password2) {
        this.username = username;
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
        this.email = email;
        this.emailVerified = emailVerified;
        this.password = password;
        this.password2 = password2;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return getFirstname() + " " +
                (getMiddlename() != null ? getMiddlename() + " " : "") +
                getLastname();
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    @JsonIgnore
    public Name getId() {
        return this.id;
    }

    public void setDn(Name dn) {
        this.id = dn;
    }

    @JsonIgnore
    public boolean isNew() {
        return this.isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public String getToken() {
        return token;
    }

    public void setEncToken(String encToken){
        this.encToken = encToken;
    }

    public String getEncToken(){
        return encToken;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
