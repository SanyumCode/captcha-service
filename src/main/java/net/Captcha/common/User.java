package net.Captcha.common;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;

@Entity("Users")
@Indexes(
    @Index(fields = @Field("name"), options = @IndexOptions(unique = true))
)
public class User {
    @Property("name")
    private String userName;
    @Id
    private int id;
    @Property("apiK")
    private String apiKey;

    public User() {

    }
    public User(String name, String api_key) {
        this.userName = name;
        this.apiKey = api_key;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }



}
