package top.alwaysready.trivials.module.noteblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.scheduler.BukkitTask;
import top.alwaysready.trivials.Trivials;

public class PlayingNotes {

    private double time = 0;
    private NoteData data;
    private String sound;
    private Location location;
    private NoteNode currentNode;

    private boolean running = false;
    private Object taskHandle;
    private long lastTickTime;

    public PlayingNotes setData(NoteData data) {
        this.data = data;
        return this;
    }

    public PlayingNotes setLocation(Location location) {
        this.location = location.clone();
        return this;
    }

    public PlayingNotes setSound(String sound) {
        this.sound = sound;
        return this;
    }

    public NoteData getData() {
        return data;
    }

    public double getTime() {
        return time;
    }

    public Location getLocation() {
        return location;
    }

    public NoteNode getCurrentNode() {
        return currentNode;
    }

    public String getSound() {
        return sound;
    }

    public void start() {
        if (running) return;
        if (getSound() == null || getLocation() == null || getData() == null) {
            Trivials.getInstance().getLogger().warning("PlayingNotes: missing parameters");
            return;
        }

        currentNode = getData().getFirst();
        if (currentNode == null) {
            Trivials.getInstance().getLogger().warning("PlayingNotes: no note data");
            return;
        }

        running = true;
        time = 0;
        lastTickTime = System.currentTimeMillis();

        playNode(currentNode, true);

        long period = 1L;
        if (Trivials.isFolia()) {
            taskHandle = Bukkit.getRegionScheduler().runAtFixedRate(
                    Trivials.getInstance(),
                    location,
                    scheduledTask -> tick(),
                    0L,
                    period
            );
        } else {
            taskHandle = Bukkit.getScheduler().runTaskTimer(
                    Trivials.getInstance(),
                    this::tick,
                    0L,
                    period
            );
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        cancelTask();
        taskHandle = null;
        currentNode = null;
    }

    private void tick() {
        if (!running || currentNode == null) {
            stop();
            return;
        }

        long now = System.currentTimeMillis();
        double elapsed = now - lastTickTime;
        lastTickTime = now;

        time += elapsed;

        while (currentNode != null && currentNode.getEnd() - time <= 25) {
            playNode(currentNode, false);
            currentNode = currentNode.getNext();
        }

        if (currentNode == null) {
            stop();
        }
    }

    private void playNode(NoteNode node, boolean sendTitle) {
        if (node == null) return;
        if (node.getPitch() < 0) return;

        location.getWorld().playSound(
                location,
                sound,
                SoundCategory.BLOCKS,
                3.0f,
                node.getPitch()
        );

        if (sendTitle) {
            String title = data.getTitle();
            if (title != null && !title.isEmpty()) {
                location.getWorld().getPlayers().stream()
                        .filter(player -> player.getLocation().distanceSquared(location) < 2304)
                        .forEach(player -> player.sendTitle(title, null, 10, 20, 10));
            }
        }
    }

    private void cancelTask() {
        if (taskHandle == null) return;
        if (Trivials.isFolia()) {
            if (taskHandle instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) taskHandle).cancel();
            }
        } else {
            if (taskHandle instanceof BukkitTask) {
                ((BukkitTask) taskHandle).cancel();
            }
        }
    }

    public boolean isStopped() {
        return !running || currentNode == null;
    }
}