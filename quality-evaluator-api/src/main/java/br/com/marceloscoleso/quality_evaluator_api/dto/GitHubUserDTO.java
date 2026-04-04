package br.com.marceloscoleso.quality_evaluator_api.dto;
 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
 
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubUserDTO {
 
    private Long id;
    private String login;
    private String name;
    private String email;
 
    @JsonProperty("avatar_url")
    private String avatarUrl;
 
    public Long getId()            { return id; }
    public String getLogin()       { return login; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getAvatarUrl()   { return avatarUrl; }
}