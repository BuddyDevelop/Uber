package uber.app.Models;

public class User {
    private String name, email, surname, token;
    private boolean driver;

    public User(){}

    public User( String name, String email, String surname, boolean driver ){
        this.name = name;
        this.email = email;
        this.surname = surname;
        this.driver = driver;
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
}
