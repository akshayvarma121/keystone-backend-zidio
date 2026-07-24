import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("manager: " + encoder.encode("manage123"));
        System.out.println("dispatcher: " + encoder.encode("route456"));
        System.out.println("technician: " + encoder.encode("wrench789"));
        System.out.println("customer: " + encoder.encode("tenant321"));
    }
}
