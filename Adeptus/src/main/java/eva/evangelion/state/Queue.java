package eva.evangelion.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Queue {
    private List<QueuePosition> queue = new ArrayList<>();

    public List<QueuePosition> getQueue() {
        return queue;
    }

    public int getNUMPOSof(QueuePosition position) {
        return getQueue().indexOf(position);
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

    public QueuePosition lastPosition() {
        for (QueuePosition pos : queue) {
            if (queue.indexOf(pos) == 0) return null;
            if (pos.getActionNumber() < 0) {
                return queue.get(queue.indexOf(pos)-1);
            }
        }
        return null;
    }

    /**
     * Inserts the given position into the queue after a number of turns
     * from the current position.
     *
     * @param position the QueuePosition to insert (must not be null)
     * @param x        number of turns after the current one to insert after -1.
     *                 x = 0 inserts at current location, the current position becomes next.
     *                 x = 1 inserts immediately after the current position,
     *                 x = 2 after the next position, etc.
     *                 If x is larger than the remaining positions, the new
     *                 position is appended at the end.
     */
    public void addPosition(QueuePosition position, int x) {

        if (position == null) return;
        int currentIdx = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getActionNumber() < 0) {
                currentIdx = i;
                break;
            }
        }
        int insertIdx;
        if (currentIdx == -1) {
            insertIdx = queue.size();
        } else {
            insertIdx = currentIdx + x;
            if (insertIdx > queue.size()) {
                insertIdx = queue.size();
            }
            if (insertIdx < 0) {        // <-- Add this missing check
                insertIdx = 0;
            }
        }
        queue.add(insertIdx, position);
        System.out.println("Added new pos, its now at "+getNUMPOSof(position)+" x was = "+x);
    }

}

