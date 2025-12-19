package com.sorro.blockdurabilitytweaks.events;

import com.sorro.blockdurabilitytweaks.BlockDurabilityTweaks;
import com.sorro.blockdurabilitytweaks.config.MainConfig;
import com.sorro.blockdurabilitytweaks.config.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EventScheduler {

    private final BlockDurabilityTweaks plugin;
    private volatile MainConfig cfg;
    private volatile ProfileManager profiles;

    private int taskId = -1;
    private final Map<String, Boolean> running = new HashMap<>();

    public EventScheduler(BlockDurabilityTweaks plugin, MainConfig cfg, ProfileManager profiles) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.profiles = profiles;
    }

    public void reload(MainConfig cfg, ProfileManager profiles) {
        this.cfg = cfg;
        this.profiles = profiles;
    }

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 20L, 20L * 60L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        running.clear();
    }

    public Set<String> scheduleNames() {
        return cfg.schedules().keySet();
    }

    public void forceRun(String name) {
        MainConfig.EventSchedule s = cfg.schedules().get(name);
        if (s == null) return;
        profiles.loadProfile(s.world(), s.profile());
        running.put(name, true);
    }

    public void forceStop(String name) {
        MainConfig.EventSchedule s = cfg.schedules().get(name);
        if (s == null) return;
        profiles.resetToVanilla(s.world());
        running.put(name, false);
    }

    private void tick() {
        if (!cfg.eventsEnabled()) return;

        ZoneId zone = cfg.eventsZone();
        ZonedDateTime now = ZonedDateTime.now(zone);

        for (var entry : cfg.schedules().entrySet()) {
            String name = entry.getKey();
            MainConfig.EventSchedule s = entry.getValue();
            boolean shouldRun = isWithin(now, s.start(), s.end(), s.durationMinutes(), zone);

            boolean currently = running.getOrDefault(name, false);
            if (shouldRun && !currently) {
                plugin.getLogger().info("Event start: " + name + " -> load profile " + s.profile() + " (world " + s.world() + ")");
                profiles.loadProfile(s.world(), s.profile());
                running.put(name, true);
            } else if (!shouldRun && currently) {
                plugin.getLogger().info("Event end: " + name + " -> reset world " + s.world() + " to vanilla");
                profiles.resetToVanilla(s.world());
                running.put(name, false);
            }
        }
    }

    private boolean isWithin(ZonedDateTime now, String start, String end, Integer durationMinutes, ZoneId zone) {
        if (start == null || start.isBlank()) return false;

        TimeWindow win = parseWindow(now.toLocalDate(), start, end, durationMinutes, zone);
        if (win == null) return false;

        return !now.isBefore(win.start) && now.isBefore(win.end);
    }

    private record TimeWindow(ZonedDateTime start, ZonedDateTime end) {}

    private TimeWindow parseWindow(LocalDate today, String startSpec, String endSpec, Integer durationMinutes, ZoneId zone) {
        ZonedDateTime start = parseSpec(today, startSpec, zone);
        if (start == null) return null;

        ZonedDateTime end;
        if (durationMinutes != null) {
            end = start.plusMinutes(durationMinutes);
        } else if (endSpec != null && !endSpec.isBlank()) {
            end = parseSpec(today, endSpec, zone);
            if (end == null) return null;
            if (!end.isAfter(start)) end = end.plusDays(7);
        } else {
            end = start.plusHours(1);
        }

        // For DAILY windows, align into current week if needed
        if (start.isAfter(ZonedDateTime.now(zone).plusDays(1))) start = start.minusDays(7);
        if (end.isBefore(start)) end = end.plusDays(7);

        // Shift window to include now if start is in past week and spec uses weekday
        ZonedDateTime now = ZonedDateTime.now(zone);
        while (end.isBefore(now.minusMinutes(1))) {
            start = start.plusDays(7);
            end = end.plusDays(7);
        }

        return new TimeWindow(start, end);
    }

    private ZonedDateTime parseSpec(LocalDate today, String spec, ZoneId zone) {
        spec = spec.trim().toUpperCase(Locale.ROOT);
        try {
            if (spec.startsWith("DAILY ")) {
                LocalTime t = LocalTime.parse(spec.substring(6).trim(), DateTimeFormatter.ofPattern("H:mm"));
                return ZonedDateTime.of(today, t, zone);
            } else {
                String[] parts = spec.split("\s+");
                if (parts.length != 2) return null;
                DayOfWeek dow = parseDow(parts[0]);
                LocalTime t = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("H:mm"));
                LocalDate date = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(dow));
                return ZonedDateTime.of(date, t, zone);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private DayOfWeek parseDow(String s) {
        return switch (s) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> DayOfWeek.MONDAY;
        };
    }
}
