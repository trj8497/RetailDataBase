import java.sql.SQLException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputScreen extends Screen {
    private Pattern pattern;
    private AfterInputUpdater updater;
    private ScreenGenerator next;

    public InputScreen(Data data, Pattern pattern, ScreenGenerator next, AfterInputUpdater updater) {
        super(data);
        this.pattern = pattern;
        this.next = next;
        this.updater = updater;
    }

    public Matcher input() {
        Scanner stdin = new Scanner(System.in);
        boolean validInput = false;
        Matcher matcher = null;

        do {
            try {
                matcher = pattern.matcher(stdin.nextLine());
                if (!matcher.matches())
                    throw new IllegalArgumentException("Input does not match specified format. Please try again.");
                validInput = true;
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
        } while (!validInput);
        return matcher;
    }

    @Override
    public Screen next() {
        System.out.print(this);
        Screen nextScreen = null;
        do {
            Matcher matched = input();
            try {
                updater.update(matched, data);
                nextScreen = next.generate(data);
            } catch (SQLException e) {
                System.err.println(String.format("%s Please try again.", e.getMessage()));
            }
        } while (nextScreen == null);
        return nextScreen;
    }

    public interface AfterInputUpdater {
        void update(Matcher matchedInput, Data dataToUpdate) throws SQLException;
    }
}
