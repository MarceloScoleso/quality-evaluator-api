package br.com.marceloscoleso.quality_evaluator_api.dto;
 
import com.fasterxml.jackson.annotation.JsonProperty;
 
// ── Resposta do GitHub ao trocar code por token ────────────────────────────
public class GitHubTokenResponseDTO {
 
    @JsonProperty("access_token")
    private String accessToken;
 
    @JsonProperty("scope")
    private String scope;
 
    @JsonProperty("token_type")
    private String tokenType;
 
    @JsonProperty("error")
    private String error;
 
    @JsonProperty("error_description")
    private String errorDescription;
 
    public String getAccessToken()        { return accessToken; }
    public String getScope()              { return scope; }
    public String getTokenType()          { return tokenType; }
    public String getError()              { return error; }
    public String getErrorDescription()   { return errorDescription; }
    public boolean hasError()             { return error != null && !error.isBlank(); }
}