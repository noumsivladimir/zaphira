import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Scanner;

public class BCryptHashGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Entrez le mot de passe ou PIN à hasher : ");
        String password = scanner.nextLine();

        // Crée l'encodeur BCrypt
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Génère le hash
        String hashedPassword = encoder.encode(password);

        System.out.println("Hash BCrypt généré : " + hashedPassword);
    }
}
