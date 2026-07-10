package net.gizmolab.library.librarymanagementsystemdesktop.dto;

public class UserDTO {
    private Long userId;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private int activeBorrowCount;

    public UserDTO() {
    }

    public UserDTO(Long userId, String firstname, String lastname, String email, String phone) {
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.activeBorrowCount = 0;
    }

    public UserDTO(Long userId, String firstname, String lastname, String email, String phone, int activeBorrowCount) {
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.activeBorrowCount = activeBorrowCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getActiveBorrowCount() {
        return activeBorrowCount;
    }

    public void setActiveBorrowCount(int activeBorrowCount) {
        this.activeBorrowCount = activeBorrowCount;
    }

    public String getFullName() {
        return (firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "");
    }

    @Override
    public String toString() {
        return getFullName();
    }
}