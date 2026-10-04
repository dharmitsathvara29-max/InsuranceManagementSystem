import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import javax.swing.*;

public class InsurancePolicyManagementSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InsuranceGUI().setVisible(true));
    }
}

// ---------------------------------------------------------------- Customer Module
class Customer {
    private String id, name, phone, email;

    Customer(String id, String name, String phone, String email) {
        this.id = id; this.name = name; this.phone = phone; this.email = email;
    }

    String getId() { return id; }
    String getName() { return name; }
    String getPhone() { return phone; }
    String getEmail() { return email; }
    void setPhone(String phone) { this.phone = phone; }
    void setEmail(String email) { this.email = email; }

    public String toString() {
        return "Customer ID: " + id + " | Name: " + name + " | Phone: " + phone + " | Email: " + email;
    }
}

//  Policy Module
class Policy {
    static final String[] TYPES = {"Health Insurance", "Life Insurance", "Vehicle Insurance", "Travel Insurance"};

    private String id, type, status = "ACTIVE";
    private Customer customer;
    private LocalDate start, end;
    private double premium, coverage; // coverage 0 = no limit

    Policy(String id, Customer customer, String type, LocalDate start, LocalDate end, double premium) {
        this(id, customer, type, start, end, premium, 0);
    }

    Policy(String id, Customer customer, String type, LocalDate start, LocalDate end,
           double premium, double coverage) {
        this.id = id; this.customer = customer; this.type = type;
        this.start = start; this.end = end; this.premium = premium; this.coverage = coverage;
    }

    String getId() { return id; }
    Customer getCustomer() { return customer; }
    String getType() { return type; }
    LocalDate getStart() { return start; }
    LocalDate getEnd() { return end; }
    double getPremium() { return premium; }
    double getCoverage() { return coverage; }
    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }

    void renew(int years) {
        start = end.plusDays(1);
        end = start.plusYears(years);
        status = "ACTIVE";
    }

    static String rs(double d) { return "₹" + String.format("%.2f", d); }

    public String toString() {
        return "Policy ID: " + id + "\nCustomer: " + customer.getName() + "\nPolicy Type: " + type +
               "\nStart Date: " + start + "\nEnd Date: " + end + "\nPremium: " + rs(premium) +
               (coverage > 0 ? "\nCoverage: " + rs(coverage) : "") + "\nStatus: " + status;
    }
}

//  Claim Module
class Claim {
    private String id, policyId, reason, status = "PENDING";
    private double amount;
    private LocalDate date = LocalDate.now();

    Claim(String id, String policyId, double amount, String reason) {
        this.id = id; this.policyId = policyId; this.amount = amount; this.reason = reason;
    }

    String getId() { return id; }
    String getPolicyId() { return policyId; }
    double getAmount() { return amount; }
    String getReason() { return reason; }
    String getStatus() { return status; }
    LocalDate getDate() { return date; }
    void setStatus(String status) { this.status = status; }

    public String toString() {
        return "Claim ID: " + id + " | Policy ID: " + policyId + " | Amount: " + Policy.rs(amount) +
               " | Reason: " + reason + " | Status: " + status + " | Date: " + date;
    }
}

// Service Module (interfaces)
interface PolicyService {
    void createPolicy(Policy p);
    void renewPolicy(String id, int years);
    Policy searchPolicy(String id);
    void updatePolicyStatus(String id, String status);
    ArrayList<Policy> getAllPolicies();
    ArrayList<Policy> getPoliciesOfCustomer(String customerId);
    void registerClaim(Claim c);
    void updateClaimStatus(String claimId, String status);
    Claim searchClaim(String claimId);
    ArrayList<Claim> getClaims();
}

interface CustomerService {
    void addCustomer(Customer c) throws ValidationException;
    void updateCustomer(String id, String phone, String email);
    Customer findCustomer(String id);
    ArrayList<Customer> getCustomers();
}

// Validation Module
class ValidationException extends IllegalArgumentException {
    ValidationException(String message) { super(message); }
}

// Service Module (implementation)
class PolicyManager implements PolicyService, CustomerService {

    private ArrayList<Customer> customers = new ArrayList<>();
    private ArrayList<Claim> claims = new ArrayList<>();
    private HashMap<String, Policy> policies = new HashMap<>();

    // Throws ValidationException when the failing condition is true.
    private static void check(boolean fail, String msg) {
        if (fail) throw new ValidationException(msg);
    }

    private static boolean badNum(double d) { return Double.isNaN(d) || Double.isInfinite(d); }
    private static boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private static String key(String id) { return id.trim().toUpperCase(); }

    // ---- customers
    private void checkContact(String phone, String email) {
        check(phone == null || !phone.matches("\\d{10}"), "Phone must contain 10 digits.");
        check(empty(email), "Email cannot be empty.");
        check(!email.trim().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"), "Email format is invalid.");
    }

    public void addCustomer(Customer c) throws ValidationException {
        check(c == null || empty(c.getId()), "Customer ID cannot be empty.");
        check(empty(c.getName()), "Customer name cannot be empty.");
        checkContact(c.getPhone(), c.getEmail());
        check(findCustomer(c.getId()) != null, "Customer ID already exists.");
        customers.add(c);
    }

    public void updateCustomer(String id, String phone, String email) {
        Customer c = findCustomer(id);
        check(c == null, "Customer not found.");
        checkContact(phone, email);
        c.setPhone(phone.trim());
        c.setEmail(email.trim());
    }

    public Customer findCustomer(String id) {
        if (id == null) return null;
        for (Customer c : customers)
            if (c.getId().equalsIgnoreCase(id.trim())) return c;
        return null;
    }

    public ArrayList<Customer> getCustomers() { return new ArrayList<>(customers); }

    // ---- policies
    public void createPolicy(Policy p) {
        check(p == null || empty(p.getId()), "Policy ID cannot be empty.");
        check(policies.containsKey(key(p.getId())), "Policy ID already exists.");
        check(p.getCustomer() == null, "Customer is required.");
        check(!List.of(Policy.TYPES).contains(p.getType()), "Invalid policy type.");
        check(p.getStart() == null || p.getEnd() == null, "Start and end dates are required.");
        check(badNum(p.getPremium()) || p.getPremium() <= 0, "Premium must be a valid number greater than zero.");
        check(!p.getEnd().isAfter(p.getStart()), "End date must be after start date.");
        check(badNum(p.getCoverage()) || p.getCoverage() < 0, "Coverage must be a valid non-negative number.");
        policies.put(key(p.getId()), p);
    }

    public Policy searchPolicy(String id) {
        refreshExpiry();
        return empty(id) ? null : policies.get(key(id));
    }

    public ArrayList<Policy> getAllPolicies() {
        refreshExpiry();
        return new ArrayList<>(policies.values());
    }

    public ArrayList<Policy> getPoliciesOfCustomer(String customerId) {
        ArrayList<Policy> list = new ArrayList<>();
        for (Policy p : getAllPolicies())
            if (!empty(customerId) && p.getCustomer().getId().equalsIgnoreCase(customerId.trim())) list.add(p);
        return list;
    }

    public void renewPolicy(String id, int years) {
        Policy p = searchPolicy(id);
        check(p == null, "Policy not found.");
        check(years <= 0, "Renewal years must be greater than zero.");
        check(p.getStatus().equals("CANCELLED"), "Cancelled policy cannot be renewed.");
        p.renew(years);
    }

    public void updatePolicyStatus(String id, String status) {
        Policy p = searchPolicy(id);
        check(p == null, "Policy not found.");
        check(status == null, "Status cannot be empty.");
        check(!status.equals("ACTIVE") && !status.equals("EXPIRED") && !status.equals("CANCELLED"),
              "Invalid policy status.");
        check(status.equals("ACTIVE") && p.getEnd().isBefore(LocalDate.now()),
              "Policy period has ended. Renew the policy instead.");
        p.setStatus(status);
    }

    // Marks ACTIVE policies whose end date has passed as EXPIRED.
    private void refreshExpiry() {
        for (Policy p : policies.values())
            if (p.getStatus().equals("ACTIVE") && p.getEnd().isBefore(LocalDate.now())) p.setStatus("EXPIRED");
    }

    // ---- claims
    public void registerClaim(Claim c) {
        check(c == null || empty(c.getId()), "Claim ID cannot be empty.");
        check(searchClaim(c.getId()) != null, "Claim ID already exists.");
        Policy p = searchPolicy(c.getPolicyId());
        check(p == null, "Policy does not exist.");
        check(!p.getStatus().equals("ACTIVE"), "Claim requires an active policy.");
        check(badNum(c.getAmount()) || c.getAmount() <= 0, "Claim amount must be a valid number greater than zero.");
        check(empty(c.getReason()), "Claim reason cannot be empty.");
        check(c.getDate().isBefore(p.getStart()) || c.getDate().isAfter(p.getEnd()),
              "Claim date is outside the policy period.");
        check(p.getCoverage() > 0 && c.getAmount() > p.getCoverage(),
              "Claim amount exceeds policy coverage of " + Policy.rs(p.getCoverage()) + ".");
        claims.add(c);
    }

    public Claim searchClaim(String claimId) {
        if (empty(claimId)) return null;
        for (Claim c : claims)
            if (c.getId().equalsIgnoreCase(claimId.trim())) return c;
        return null;
    }

    public ArrayList<Claim> getClaims() { return new ArrayList<>(claims); }

    public void updateClaimStatus(String claimId, String status) {
        Claim c = searchClaim(claimId);
        check(c == null, "Claim not found.");
        check(status == null || (!status.equals("APPROVED") && !status.equals("REJECTED")),
              "Claim status must be APPROVED or REJECTED.");
        check(!c.getStatus().equals("PENDING"), "Claim is already " + c.getStatus() + " and cannot be changed.");
        c.setStatus(status);
    }

    // ---- summary
    private long countPolicies(String s) { return policies.values().stream().filter(p -> p.getStatus().equals(s)).count(); }
    private long countClaims(String s) { return claims.stream().filter(c -> c.getStatus().equals(s)).count(); }

    String summary() {
        refreshExpiry();
        double premium = 0, claimAmount = 0, approved = 0;
        for (Policy p : policies.values()) premium += p.getPremium();
        for (Claim c : claims) {
            claimAmount += c.getAmount();
            if (c.getStatus().equals("APPROVED")) approved += c.getAmount();
        }
        StringBuilder types = new StringBuilder();
        for (String t : Policy.TYPES)
            types.append(String.format("  %-18s: %d%n", t,
                policies.values().stream().filter(p -> p.getType().equals(t)).count()));
        return "========== INSURANCE SUMMARY ==========\n\n" +
               "Customers        : " + customers.size() + "\n" +
               "Policies         : " + policies.size() + "\n" +
               "Active Policies  : " + countPolicies("ACTIVE") + "\n" +
               "Expired Policies : " + countPolicies("EXPIRED") + "\n" +
               "Cancelled        : " + countPolicies("CANCELLED") + "\n" +
               "Total Premium    : " + Policy.rs(premium) + "\n\n" +
               "Policies by type :\n" + types + "\n" +
               "Claims           : " + claims.size() + "\n" +
               "Pending Claims   : " + countClaims("PENDING") + "\n" +
               "Approved Claims  : " + countClaims("APPROVED") + "\n" +
               "Rejected Claims  : " + countClaims("REJECTED") + "\n" +
               "Claim Amount     : " + Policy.rs(claimAmount) + "\n" +
               "Approved Amount  : " + Policy.rs(approved) + "\n" +
               "========================================";
    }
}
//GUI Module
class InsuranceGUI extends JFrame {

    private interface Task { void go() throws Exception; }

    private PolicyManager manager = new PolicyManager();
    private JTextArea output = new JTextArea();

    InsuranceGUI() {
        setTitle("Insurance Policy Management System");
        setSize(900, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        String[] names = {
            "Add Customer", "Update Customer", "Search Customer", "List Customers",
            "Create Policy", "Search Policy", "Renew Policy", "Update Policy Status",
            "Customer Policies", "List All Policies", "Register Claim", "Search Claim",
            "Update Claim Status", "List Claims", "Policy Summary", "Clear Output", "Exit"
        };
        Runnable[] actions = {
            this::addCustomer, this::updateCustomer,
            () -> run(() -> find("Customer ID:", manager::findCustomer, "CUSTOMER", "Customer not found.")),
            () -> show("ALL CUSTOMERS", manager.getCustomers()),
            this::createPolicy,
            () -> run(() -> find("Policy ID:", manager::searchPolicy, "POLICY", "Policy not found.")),
            this::renewPolicy, this::updateStatus, this::customerPolicies,
            () -> show("ALL POLICIES", manager.getAllPolicies()),
            this::registerClaim,
            () -> run(() -> find("Claim ID:", manager::searchClaim, "CLAIM", "Claim not found.")),
            this::updateClaimStatus,
            () -> show("ALL CLAIMS", manager.getClaims()),
            () -> output.setText(manager.summary()),
            () -> output.setText(""),
            this::exit
        };
        JPanel panel = new JPanel(new GridLayout(0, 4, 10, 10));
        for (int i = 0; i < names.length; i++) {
            JButton b = new JButton(names[i]);
            Runnable r = actions[i];
            b.addActionListener(e -> r.run());
            panel.add(b);
        }
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }

    // ---- actions
    private void addCustomer() {
        run(() -> {
            String[] v = ask("Customer ID:", "Customer Name:", "10-digit Phone:", "Email:");
            if (v == null) return;
            manager.addCustomer(new Customer(v[0], v[1], v[2], v[3]));
            success("Customer added.");
        });
    }

    private void updateCustomer() {
        run(() -> {
            String[] v = ask("Customer ID:", "New 10-digit Phone:", "New Email:");
            if (v == null) return;
            manager.updateCustomer(v[0], v[1], v[2]);
            success("Customer updated.");
        });
    }

    private void createPolicy() {
        run(() -> {
            String[] a = ask("Policy ID:", "Customer ID:");
            if (a == null) return;
            Customer customer = manager.findCustomer(a[1]);
            if (customer == null) { error("Customer not found."); return; }
            String type = pick("Select Policy Type:", "Policy Type", Policy.TYPES);
            if (type == null) return;
            String[] b = ask("Start Date (YYYY-MM-DD):", "End Date (YYYY-MM-DD):",
                             "Premium Amount:", "Coverage Amount (sum insured):");
            if (b == null) return;
            manager.createPolicy(new Policy(a[0], customer, type,
                parseDate(b[0]), parseDate(b[1]), parseNumber(b[2]), parseNumber(b[3])));
            success("Policy created.");
        });
    }

    private void renewPolicy() {
        run(() -> {
            String[] v = ask("Policy ID:", "Renewal Period (Years):");
            if (v == null) return;
            int years;
            try { years = Integer.parseInt(v[1]); }
            catch (NumberFormatException e) { throw new ValidationException("Invalid whole number '" + v[1] + "'."); }
            manager.renewPolicy(v[0], years);
            Policy p = manager.searchPolicy(v[0]);
            success("Policy renewed.");
            output.append("\nPolicy Renewed\nID: " + p.getId() + "\nStart: " + p.getStart() +
                          "\nEnd: " + p.getEnd() + "\n\n");
        });
    }

    private void updateStatus() {
        run(() -> {
            String id = input("Policy ID:");
            if (id == null) return;
            String status = pick("Select Status:", "Policy Status", "ACTIVE", "EXPIRED", "CANCELLED");
            if (status == null) return;
            manager.updatePolicyStatus(id, status);
            success("Status updated.");
        });
    }

    private void customerPolicies() {
        run(() -> {
            String id = input("Customer ID:");
            if (id == null) return;
            if (manager.findCustomer(id) == null) { error("Customer not found."); return; }
            show("POLICIES OF CUSTOMER " + id, manager.getPoliciesOfCustomer(id));
        });
    }

    private void registerClaim() {
        run(() -> {
            String[] v = ask("Claim ID:", "Policy ID:", "Claim Amount:", "Claim Reason:");
            if (v == null) return;
            manager.registerClaim(new Claim(v[0], v[1], parseNumber(v[2]), v[3]));
            success("Claim registered.");
        });
    }

    private void updateClaimStatus() {
        run(() -> {
            String id = input("Claim ID:");
            if (id == null) return;
            String status = pick("Select Claim Status:", "Claim Status", "APPROVED", "REJECTED");
            if (status == null) return;
            manager.updateClaimStatus(id, status);
            success("Claim status updated.");
        });
    }

    // ---- helpers

    // Runs an action and shows any exception message in an error dialog.
    private void run(Task t) {
        try { t.go(); } catch (Exception e) { error(e.getMessage()); }
    }

    // Asks for an ID, looks it up and prints the result (or an error if not found).
    private void find(String prompt, Function<String, Object> finder, String title, String notFound) {
        String id = input(prompt);
        if (id == null) return;
        Object o = finder.apply(id);
        if (o == null) error(notFound); else show(title, List.of(o));
    }

    // Prints a list of records in the output area.
    private void show(String title, List<?> items) {
        if (items.isEmpty()) { error("No records found."); return; }
        StringBuilder sb = new StringBuilder("\n========== " + title + " ==========\n");
        for (Object o : items) sb.append(o).append("\n----------------------------\n");
        output.append(sb.toString());
    }

    // Asks several questions in a row; returns null if the user cancels any of them.
    private String[] ask(String... prompts) {
        String[] v = new String[prompts.length];
        for (int i = 0; i < v.length; i++)
            if ((v[i] = input(prompts[i])) == null) return null;
        return v;
    }

    private String pick(String msg, String title, String... options) {
        return (String) JOptionPane.showInputDialog(this, msg, title,
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    }

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s.trim()); }
        catch (DateTimeParseException e) { throw new ValidationException("Invalid date '" + s + "'. Use format YYYY-MM-DD."); }
    }

    private double parseNumber(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { throw new ValidationException("Invalid number '" + s + "'."); }
    }

    private String input(String message) {
        String value = JOptionPane.showInputDialog(this, message);
        if (value == null) return null;
        value = value.trim();
        if (value.isEmpty()) { error("Input cannot be empty."); return null; }
        return value;
    }

    private void success(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Invalid input." : message,
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void exit() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?", "Exit",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            System.exit(0);
    }
}