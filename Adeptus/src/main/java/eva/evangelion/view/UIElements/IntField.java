package eva.evangelion.view.UIElements;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Text field that only accepts whole numbers (negative allowed). */
public class IntField extends TextField {

    public IntField(int initial) {
        super(Integer.toString(initial));
        setPrefColumnCount(5);
        setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().matches("-?\\d{0,6}") ? change : null));
        focusedProperty().addListener((obs, was, now) -> {
            if (!now && (getText().isEmpty() || getText().equals("-"))) setText("0");
        });
    }

    public int getInt() {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setInt(int value) {
        setText(Integer.toString(value));
    }
}