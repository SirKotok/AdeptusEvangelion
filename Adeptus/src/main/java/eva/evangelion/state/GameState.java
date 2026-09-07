package eva.evangelion.state;

import eva.evangelion.state.actions.Action;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    private InitialState initialState;
    private List<Action> ActionList = new ArrayList<>();

    public GameState() {
    }

    public void addAction(Action action) {
        ActionList.add(action);
    }

    public int getLastActionNumber() {
        if (ActionList.isEmpty()) {
            return -1;
        }
        return ActionList.get(ActionList.size() - 1).getActionNumber();
    }



    /**
     * Returns the total number of actions stored.
     */
    public int getActionCount() {
        return ActionList.size();
    }

    /**
     * Returns an unmodifiable view of the action list.
     */
    public List<Action> getActions() {
        return new ArrayList<>(ActionList);
    }
    public Action getActionfromNumber(int number) {
        List<Action> list = getActions();
        for (Action act : list) {
            if (act.getActionNumber() == number) return act;
        }
        return null;
    }
    /**
     * Saves this GameState to the given file using Java serialization.
     *
     * @param path the file path to save to
     * @throws IOException if an I/O error occurs
     */
    public void saveToFile(Path path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))) {
            oos.writeObject(this);
        }
    }

    /**
     * Loads a GameState from the given file using Java serialization.
     *
     * @param path the file path to load from
     * @return the loaded GameState, or null if the file does not exist or is invalid
     * @throws IOException if an I/O error occurs during reading
     * @throws ClassNotFoundException if the class definition is not found
     */
    public static GameState loadFromFile(Path path) throws IOException, ClassNotFoundException {
        if (!Files.exists(path)) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            return (GameState) ois.readObject();
        }
    }

    public InitialState getInitialState() {
        return initialState;
    }

    public void setInitialState(InitialState initialState) {
        this.initialState = initialState;
    }
}