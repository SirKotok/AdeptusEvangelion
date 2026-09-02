package eva.evangelion.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Queue {
    private List<QueuePosition> queue = new ArrayList<>();

    public List<QueuePosition> getQueue() {
        return queue;
    }

    /**
     * Returns the first QueuePosition whose ActionNumber is negative
     * (i.e., not yet processed). Returns null if none found.
     */
    public QueuePosition currentPosition() {
        for (QueuePosition pos : queue) {
            if (pos.getActionNumber() < 0) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Inserts the given position into the queue after a number of turns
     * from the current position.
     *
     * @param position the QueuePosition to insert (must not be null)
     * @param x        number of turns after the current one to insert after.
     *                 x = 0 inserts immediately after the current position,
     *                 x = 1 after the next position, etc.
     *                 If x is larger than the remaining positions, the new
     *                 position is appended at the end.
     */
    public void addPosition(QueuePosition position, int x) {
        if (position == null) return;

        // Find the index of the current position (first with negative ActionNumber)
        int currentIdx = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getActionNumber() < 0) {
                currentIdx = i;
                break;
            }
        }

        int insertIdx;
        if (currentIdx == -1) {
            // No current position – add at the end
            insertIdx = queue.size();
        } else {
            // Insert after (x + 1) positions from current index
            insertIdx = currentIdx + x + 1;
            // Clamp to valid range
            if (insertIdx > queue.size()) {
                insertIdx = queue.size();
            }
        }

        queue.add(insertIdx, position);
    }
}

