import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class Screen {
    protected List<String> segments = new ArrayList<>();
    protected Data data;

    public Screen(Data data) {
        this.data = data;
    }

    public void add(String segment) {
        segments.add(segment);
    }

    public abstract Screen next();

    @Override
    public String toString() {
        return String.join("\n", segments);
    }

    public interface ScreenGenerator {
        Screen generate(Data data) throws SQLException;
    }
}
