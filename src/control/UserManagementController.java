package control;

import boundary.NotificationInterface;
import entity.AccountStatus;
import entity.DataStore;
import entity.Field;
import entity.Role;
import entity.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Control for UC-10 Manage Users and Roles: invite users, change role and field access, deactivate. */
public class UserManagementController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9()+\\-.\\s]{7,20}$");

    private final DataStore store;
    private final NotificationInterface notifier;

    public UserManagementController(DataStore store, NotificationInterface notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    /** All users with role, status and last login (UC-10 step 2). */
    public List<User> loadUsers() {
        return List.copyOf(store.users());
    }

    public List<Field> getFields() {
        return List.copyOf(store.fields());
    }

    /**
     * Invite User (UC-10 steps 3-4): validates the form, checks the email is not
     * registered yet, creates a Pending account and sends the invitation email.
     * Throws IllegalArgumentException with a message for the form on bad input.
     */
    public User inviteUser(String name, String email, String phone, Role role, List<Integer> fieldIds) {
        String n = name == null ? "" : name.trim();
        String e = email == null ? "" : email.trim();
        String p = phone == null ? "" : phone.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Enter the user's name.");
        if (e.isEmpty()) throw new IllegalArgumentException("Enter an email address.");
        if (!EMAIL.matcher(e).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        if (!p.isEmpty() && !PHONE.matcher(p).matches()) {
            throw new IllegalArgumentException("Enter a valid phone number.");
        }
        if (role == null) throw new IllegalArgumentException("Select a role.");
        if (fieldIds == null || fieldIds.isEmpty()) throw new IllegalArgumentException("Select at least one field.");
        if (checkEmailExists(e)) {
            throw new IllegalArgumentException("This email is already registered.");   // alternate flow 3a
        }

        User user = new User(store.nextUserId(), n, e, p, role, AccountStatus.PENDING,
                fieldIds, null, LocalDateTime.now());
        store.users().add(user);
        notifier.sendInvitation(user, fieldAccessLabel(user));
        return user;
    }

    public boolean checkEmailExists(String email) {
        for (User u : store.users()) {
            if (u.getEmail().equalsIgnoreCase(email.trim())) return true;
        }
        return false;
    }

    /** Edit Role (UC-10 step 5): changes role and field access. */
    public void editRole(User user, Role role, List<Integer> fieldIds) {
        if (role == null) throw new IllegalArgumentException("Select a role.");
        if (fieldIds == null || fieldIds.isEmpty()) throw new IllegalArgumentException("Select at least one field.");
        if (user.getRole() == Role.SYSTEM_ADMINISTRATOR && role != Role.SYSTEM_ADMINISTRATOR
                && isLastActiveAdmin(user)) {
            throw new IllegalArgumentException("At least one active System Administrator is required.");
        }
        user.setRole(role);
        user.setFieldAccess(fieldIds);
    }

    /** Deactivate (UC-10 alternate flow 5a): blocks login, keeps the account and its history. */
    public void deactivate(User user) {
        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new IllegalArgumentException(user.getName() + " is already inactive.");
        }
        if (user.getRole() == Role.SYSTEM_ADMINISTRATOR && isLastActiveAdmin(user)) {
            throw new IllegalArgumentException("At least one active System Administrator is required.");
        }
        user.setAccountStatus(AccountStatus.INACTIVE);
    }

    private boolean isLastActiveAdmin(User user) {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) return false;
        for (User u : store.users()) {
            if (u != user && u.getRole() == Role.SYSTEM_ADMINISTRATOR
                    && u.getAccountStatus() == AccountStatus.ACTIVE) {
                return false;
            }
        }
        return true;
    }

    /** "All fields", or the field names in field order. */
    public String fieldAccessLabel(User user) {
        List<String> names = new ArrayList<>();
        for (Field f : store.fields()) {
            if (user.getFieldAccess().contains(f.fieldId())) names.add(f.name());
        }
        if (names.isEmpty()) return "None";
        if (names.size() == store.fields().size()) return "All fields";
        return String.join(", ", names);
    }
}
