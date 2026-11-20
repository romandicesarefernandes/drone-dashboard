package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Entity: a dashboard user account. Deactivated users are kept (with their history), never deleted. */
public class User {
    private final int userId;
    private final String name;
    private final String email;
    private final String phone;
    private final LocalDateTime createdAt;
    private Role role;
    private AccountStatus accountStatus;
    private final List<Integer> fieldAccess = new ArrayList<>();
    private LocalDateTime lastLogin;   // null = never logged in

    public User(int userId, String name, String email, String phone, Role role, AccountStatus accountStatus,
                List<Integer> fieldAccess, LocalDateTime lastLogin, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.accountStatus = accountStatus;
        this.fieldAccess.addAll(fieldAccess);
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
    }

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public List<Integer> getFieldAccess() { return Collections.unmodifiableList(fieldAccess); }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRole(Role role) { this.role = role; }
    public void setAccountStatus(AccountStatus status) { this.accountStatus = status; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public void setFieldAccess(List<Integer> fieldIds) {
        fieldAccess.clear();
        fieldAccess.addAll(fieldIds);
    }

    @Override
    public String toString() { return name; }
}
