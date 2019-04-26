import java.sql.SQLException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class OptionScreen extends Screen {
    private List<AfterChosenUpdater> updaters = new ArrayList<>();
    private List<ScreenGenerator> options = new ArrayList<>();

    public OptionScreen(Data data) {
        super(data);
    }

    public void addOption(String segment, ScreenGenerator ifChosen, AfterChosenUpdater updater) {
        options.add(ifChosen);
        updaters.add(updater);
        segments.add(String.format("%d) %s", options.size(), segment));
    }

    public int choose() {
        Scanner stdin = new Scanner(System.in);
        boolean validOption = false;
        int chosen = 0;

        do {
            try {
                chosen = stdin.nextInt();
                if (chosen < 1 || chosen > options.size())
                    throw new IllegalArgumentException(String.format("Option must be between 1 and %d.", options.size()));
                validOption = true;
            } catch (InputMismatchException e) {
                System.err.println("Invalid option entered. Please try again.");
                stdin.nextLine();
            } catch (IllegalArgumentException e) {
                System.err.println(String.format("%s Please try again.", e.getMessage()));
            }
        } while (!validOption);
        return chosen;
    }

    @Override
    public Screen next() {
        System.out.print(this);
        Screen next = null;
        do {
            int chosen = choose();
            try {
                updaters.get(chosen - 1).update(data);
                next = options.get(chosen - 1).generate(data);
            } catch (SQLException e) {
                System.err.println(String.format("%s Please try again.", e.getMessage()));
            }
        } while (next == null);
        return next;
    }

    public interface AfterChosenUpdater {
        void update(Data dataToUpdate) throws SQLException;
    }
}
