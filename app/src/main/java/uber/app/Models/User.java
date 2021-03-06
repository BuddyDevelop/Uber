package uber.app.Models;

public class User {
    private String name, email, surname, token, profileImageUrl, phoneNumber;
    private boolean driver;

    public User(){}

    public User( String name, String email, String surname, boolean driver ){
        this.name = name;
        this.email = email;
        this.surname = surname;
        this.driver = driver;
    }

    public User( String name, String email, String surname, boolean driver, String profileImageUrl ){
        this.name = name;
        this.email = email;
        this.surname = surname;
        this.driver = driver;
        this.profileImageUrl = profileImageUrl;
    }

    public User( String name, String email, String surname, String phoneNumber, boolean driver ){
        this.name = name;
        this.email = email;
        this.surname = surname;
        this.driver = driver;
        this.phoneNumber = phoneNumber;
    }

    public User( String name, String email, String surname, boolean driver, String profileImageUrl, String phoneNumber ){
        this.name = name;
        this.email = email;
        this.surname = surname;
        this.driver = driver;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
    }

    public User( boolean isDriver, String email, String name, String surname, String token ) {
        this.name = name;
        this.email = email;
        this.surname = surname;
        this.driver = isDriver;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname( String surname ) {
        this.surname = surname;
    }

    public String getToken() {
        return token;
    }

    public void setToken( String token ) {
        this.token = token;
    }

    public boolean isDriver() { return driver; }

    public void setDriver( boolean driver ) {
        this.driver = driver;
    }

    public String getProfileImageUrl() { return profileImageUrl; }

    public void setProfileImageUrl( String profileImageUrl ) { this.profileImageUrl = profileImageUrl; }

    public String getPhoneNumber() { return phoneNumber; }

    public void setPhoneNumber( String phoneNumber ) { this.phoneNumber = phoneNumber; }
}
