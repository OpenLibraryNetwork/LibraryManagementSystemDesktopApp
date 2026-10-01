package net.gizmolab.library.librarymanagementsystemdesktop.dto.draft;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft.isBlank;
import static net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft.putText;

/** "Νέο πρόσωπο" dialog; payload for POST /api/persons/local. */
public class PersonDraft {

    private String firstname;
    private String lastname;
    private String qualifier;
    private String bornYear;
    private String deathYear;

    public Map<String, String> validate() {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(firstname) && isBlank(lastname)) errors.put("name", "Συμπληρώστε όνομα ή επώνυμο.");
        return errors;
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> data = new LinkedHashMap<>();
        String name = ((isBlank(firstname) ? "" : firstname.trim()) + " " + (isBlank(lastname) ? "" : lastname.trim())).trim();
        data.put("name", name);
        putText(data, "firstname", firstname);
        putText(data, "lastname", lastname);
        putText(data, "qualifier", qualifier);
        putText(data, "bornYear", bornYear);
        putText(data, "deathYear", deathYear);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", data);
        return payload;
    }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getQualifier() { return qualifier; }
    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    public String getBornYear() { return bornYear; }
    public void setBornYear(String bornYear) { this.bornYear = bornYear; }
    public String getDeathYear() { return deathYear; }
    public void setDeathYear(String deathYear) { this.deathYear = deathYear; }
}
