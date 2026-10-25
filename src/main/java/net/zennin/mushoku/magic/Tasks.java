package net.zennin.mushoku.magic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tareas diferidas o repetidas del servidor (tormentas, tornados, impactos con retraso). */
public final class Tasks {
    /** Se ejecuta cada tick hasta devolver false o cumplir los ticks. */
    public interface Repeating {
        boolean run(int tick);
    }

    private static final class Entry {
        final Repeating task;
        final int every;
        final int total;
        int delay;
        int elapsed = 0;

        Entry(Repeating task, int delay, int every, int total) {
            this.task = task;
            this.delay = delay;
            this.every = Math.max(1, every);
            this.total = total;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<Entry> PENDING = new ArrayList<>();

    private Tasks() {}

    /** Ejecuta una vez despues de delay ticks. */
    public static void later(int delay, Runnable r) {
        PENDING.add(new Entry(t -> {
            r.run();
            return false;
        }, delay, 1, 1));
    }

    /** Ejecuta cada 'every' ticks durante 'total' ticks (empieza despues de delay). */
    public static void repeat(int delay, int every, int total, Repeating r) {
        PENDING.add(new Entry(r, delay, every, total));
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ENTRIES.addAll(PENDING);
            PENDING.clear();
            Iterator<Entry> it = ENTRIES.iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                if (e.delay > 0) {
                    e.delay--;
                    continue;
                }
                boolean keep = true;
                if (e.elapsed % e.every == 0) {
                    try {
                        keep = e.task.run(e.elapsed);
                    } catch (Exception ex) {
                        keep = false;
                    }
                }
                e.elapsed++;
                if (!keep || e.elapsed >= e.total) it.remove();
            }
        });
    }
}
