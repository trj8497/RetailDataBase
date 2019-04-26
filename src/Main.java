import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Access to the database, same object passed from screen to screen
        try (Data data = new Data("./data/retail", "hxm3443", "")) {
            Screen.ScreenGenerator start = Screens.home;
            Screen current = start.generate(data);
            while (true) {
                current = current.next();
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
}
