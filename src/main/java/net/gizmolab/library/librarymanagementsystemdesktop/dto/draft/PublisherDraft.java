package net.gizmolab.library.librarymanagementsystemdesktop.dto.draft;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft.isBlank;
import static net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft.putText;

/** "Νέος εκδότης" dialog; payload for POST /api/publishers/local. */
public class PublisherDraft {

    private String name;
    private String qualifier;
    private String address;
    private String phone;
    private String email;
    private String website;

    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(name)) errors.put("name", "Το όνομα είναι υποχρεωτικό.");
        return errors;
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name.trim());
        putText(data, "qualifier", qualifier);
        putText(data, "address", address);
        putText(data, "phone", phone);
        putText(data, "email", email);
        putText(data, "website", website);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", data);
        return payload;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQualifier() { return qualifier; }
    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
