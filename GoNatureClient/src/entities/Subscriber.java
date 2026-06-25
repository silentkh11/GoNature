package entities;

import java.io.Serializable;

/**
 * Represents a registered GoNature subscriber or certified tour guide.
 * Subscribers receive booking discounts; guides additionally lead group tours
 * and benefit from special group pricing (guide enters free).
 */
public class Subscriber implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Visitor Table Fields
    private String visitorId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    
    // Subscriber Table Fields
    private int subscriberId;   // auto-increment member number (e.g. 1001)
    private int familySize;
    private String creditCard;
    private boolean isGuide;

    public Subscriber(String visitorId, String firstName, String lastName, String email, String phone, int familySize, String creditCard, boolean isGuide) {
        this.visitorId = visitorId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.familySize = familySize;
        this.creditCard = creditCard;
        this.isGuide = isGuide;
    }

    // --- Getters ---
    public String getVisitorId() { return visitorId; }
    public int    getSubscriberId() { return subscriberId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public int getFamilySize() { return familySize; }
    public String getCreditCard() { return creditCard; }
    public boolean isGuide() { return isGuide; }

    /** Injected by the server after DB fetch to carry the auto-increment member number. */
    public void setSubscriberId(int id) { this.subscriberId = id; }
}