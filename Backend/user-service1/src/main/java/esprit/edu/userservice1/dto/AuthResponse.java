package esprit.edu.userservice1.dto;

import esprit.edu.userservice1.entities.user;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String role;
    private String email;
    private Long   userId;
    private String step;
    private Boolean twoFaRequired;
    private Object user;

    // ── Constructeur vide ──────────────────
    public AuthResponse() {}

    // ── Constructeur utilisé dans le controller ──
    public AuthResponse(String token, user u, String role) {
        this.token  = token;
        this.role   = role;
        this.email  = u.getEmail();
        this.userId = u.getId();
        this.user   = u;
    }

    // ── Getters & Setters ──────────────────
    public String  getToken()                  { return token; }
    public void    setToken(String token)      { this.token = token; }

    public String  getRole()                   { return role; }
    public void    setRole(String role)        { this.role = role; }

    public String  getEmail()                  { return email; }
    public void    setEmail(String email)      { this.email = email; }

    public Long    getUserId()                 { return userId; }
    public void    setUserId(Long userId)      { this.userId = userId; }

    public String  getStep()                   { return step; }
    public void    setStep(String step)        { this.step = step; }

    public Boolean getTwoFaRequired()          { return twoFaRequired; }
    public void    setTwoFaRequired(Boolean b) { this.twoFaRequired = b; }

    public Object  getUser()                   { return user; }
    public void    setUser(Object user)        { this.user = user; }
}