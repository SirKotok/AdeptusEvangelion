package eva.evangelion.state;

import eva.evangelion.state.actions.Action;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    private InitialState initialState;
    private List<Action> ActionList = new ArrayList<>();
    public enum GAME_MODE {
        CLASSIC,
        CUSTOM,
        FFA,
        TEAM
    }
    private GAME_MODE GameMode;

    public void setGameMode(GAME_MODE gameMode) {
        GameMode = gameMode;
    }

    public GAME_MODE getGameMode() {
        return GameMode;
    }

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



    // field
    private String sessionId = UUID.randomUUID().toString();   // only set by the constructor, so old files load as null
    public String getSessionId() { return sessionId; }
    public void setSessionId(String id) { this.sessionId = id; }
    public void newSession() { this.sessionId = UUID.randomUUID().toString(); }

    /** Deep copy through serialization. */
    public GameState deepCopy() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            oos.flush();
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                return (GameState) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Could not copy GameState", e);
        }
    }

    /**
     * Returns a NEW state that keeps actions up to and including actionNumber (by list order)
     * and drops everything after. If a number is duplicated in an old file, the last one is used.
     * The original is untouched. The result has a new sessionId.
     */
    public GameState revertedTo(int actionNumber) {
        GameState copy = deepCopy();
        int keep = -1;
        for (int i = 0; i < copy.ActionList.size(); i++) {
            if (copy.ActionList.get(i).getActionNumber() == actionNumber) keep = i + 1;
        }
        if (keep < 0) throw new IllegalArgumentException("No action with number " + actionNumber);
        copy.ActionList = new ArrayList<>(copy.ActionList.subList(0, keep));
        copy.newSession();
        return copy;
    }

}