package org.nia.niamod.features;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.gui.screen.IgnoreManagerScreen;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.SchedulerManager;
import org.nia.niamod.models.events.ChatMessageReceivedEvent;
import org.nia.niamod.models.ignore.IgnoreAction;
import org.nia.niamod.models.ignore.IgnorePlayerEntry;
import org.nia.niamod.models.ignore.IgnorePlayerMode;
import org.nia.niamod.models.ignore.QueuedIgnoreCommand;
import org.nia.niamod.util.WynncraftAPI;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class IgnoreFeature extends Feature {
    private static final int IGNORE_QUEUE_INTERVAL_TICKS = 10;
    private static final String IGNORE_PREFIX = "(\uDAFF\uDFFC\uE008\uDAFF\uDFFF\uE002\uDAFF\uDFFE|\uDAFF\uDFFC\uE001\uDB00\uDC06)";
    private static final String IGNORE_PLAYER = "([A-Za-z0-9]{3,16})";
    private static final Pattern IGNORED_PATTERN =
            Pattern.compile(IGNORE_PREFIX + " " + IGNORE_PLAYER + " has been added to your ignore list!");
    private static final Pattern ALREADY_IGNORED_PATTERN =
            Pattern.compile(IGNORE_PREFIX + " " + IGNORE_PLAYER + " is already ignored!");
    private static final Pattern UNIGNORED_PATTERN =
            Pattern.compile(IGNORE_PREFIX + " " + IGNORE_PLAYER + " has been removed from your ignore list!");

    private final Set<String> ignoredPlayers = new HashSet<>();
    private final Set<String> chatDetectedPlayers = new HashSet<>();
    private final Deque<QueuedIgnoreCommand> queuedCommands = new ArrayDeque<>();
    @Getter
    private IgnoreAction nextFavouriteAction = IgnoreAction.IGNORE;
    @Getter
    private int revision;
    @Getter
    private List<String> players;

    private static Optional<String> playerName(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find() || matcher.groupCount() < 2) {
            return Optional.empty();
        }

        String playerName = matcher.group(2);
        return playerName == null || playerName.isBlank() ? Optional.empty() : Optional.of(playerName);
    }

    @Override
    public void init() {
        KeybindManager.registerKeybinding(
                "Open Ignore Manager",
                GLFW.GLFW_KEY_BACKSLASH,
                safeRunnable("open_manager", this::openScreen));
        KeybindManager.registerKeybinding(
                "Mass Ignore",
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                safeRunnable("toggle_favourite_ignores", this::toggleFavourites));
        NiaEventBus.subscribe(this);
        players = List.of();
        loadPersistedIgnoredPlayers();
        loadGuildPlayersAsync();
        scheduleQueuedCommands();
    }

    private void loadGuildPlayersAsync() {
        WynncraftAPI.guildResponseAsync(NyahConfig.getData().getGuildName())
                .whenComplete(
                        (response, throwable) -> {
                            if (throwable != null) {
                                NiamodClient.LOGGER.warn("Failed to load guild players", throwable);
                                return;
                            }
                            List<String> loadedPlayers = response == null ? List.of() : response.allUsernames();
                            Minecraft.getInstance().execute(() -> {
                                players = loadedPlayers;
                                markChanged();
                            });
                        });
    }

    public List<IgnorePlayerEntry> getVisiblePlayers(String searchQuery) {
        String query = normalise(searchQuery);
        return listedPlayers().stream()
                .filter(entry -> query.isEmpty() || normalise(entry.playerName()).contains(query))
                .sorted(
                        Comparator.comparingInt(this::rowRank)
                                .thenComparing(entry -> normalise(entry.playerName())))
                .toList();
    }

    public IgnorePlayerMode modeFor(String playerName) {
        if (isFavourite(playerName)) {
            return IgnorePlayerMode.FAVOURITE;
        }
        if (isAvoided(playerName)) {
            return IgnorePlayerMode.AVOID;
        }
        return IgnorePlayerMode.NONE;
    }

    public boolean isFavourite(String playerName) {
        return containsName(NyahConfig.getData().getFavouritePlayers(), playerName);
    }

    public boolean isAvoided(String playerName) {
        return containsName(NyahConfig.getData().getAvoidedPlayers(), playerName);
    }

    public boolean canChangeMode(String playerName) {
        return !isChatDetected(playerName);
    }

    public void setMode(String playerName, IgnorePlayerMode mode) {
        if (!canChangeMode(playerName)) {
            return;
        }
        removeName(NyahConfig.getData().getFavouritePlayers(), playerName);
        removeName(NyahConfig.getData().getAvoidedPlayers(), playerName);
        if (mode == IgnorePlayerMode.FAVOURITE) {
            addName(NyahConfig.getData().getFavouritePlayers(), playerName);
        } else if (mode == IgnorePlayerMode.AVOID) {
            addName(NyahConfig.getData().getAvoidedPlayers(), playerName);
        }
        NyahConfig.save();
        markChanged();
    }

    public void setFavourite(String playerName) {
        setMode(playerName, IgnorePlayerMode.FAVOURITE);
    }

    public void setAvoided(String playerName) {
        setMode(playerName, IgnorePlayerMode.AVOID);
    }

    public void clearMode(String playerName) {
        setMode(playerName, IgnorePlayerMode.NONE);
    }

    public void addChatDetectedPlayer(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            String trimmed = playerName.trim();
            boolean detectedChanged = addChatDetected(trimmed);
            boolean ignoredChanged = setIgnored(trimmed, true);
            if (detectedChanged || ignoredChanged) {
                persistIgnoredPlayers();
                markChanged();
            }
        }
    }

    public void clearChatDetectedPlayers() {
        if (!chatDetectedPlayers.isEmpty()) {
            chatDetectedPlayers.clear();
            markChanged();
        }
    }

    public boolean isIgnored(String playerName) {
        return ignoredPlayers.contains(normalise(playerName));
    }

    public boolean hasIgnoredPlayers() {
        return !ignoredPlayers.isEmpty();
    }

    public void markIgnored(String playerName) {
        setIgnoredState(playerName, true);
    }

    public void markUnignored(String playerName) {
        setIgnoredState(playerName, false);
    }

    public void setIgnoredState(String playerName, boolean ignored) {
        boolean ignoredChanged = setIgnored(playerName, ignored);
        boolean detectedChanged = !ignored && removeChatDetected(playerName);
        if (ignoredChanged || detectedChanged) {
            persistIgnoredPlayers();
            markChanged();
        }
    }

    public void toggleIgnored(String playerName) {
        String normalised = normalise(playerName);
        boolean ignored = ignoredPlayers.contains(normalised);
        queueAction(playerName, ignored ? IgnoreAction.UNIGNORE : IgnoreAction.IGNORE);
    }

    public void toggleFavourites() {
        IgnoreAction action = nextFavouriteAction;
        runFavouriteAction(action);
        setNextFavouriteAction(action.opposite());
    }

    public void setNextFavouriteAction(IgnoreAction action) {
        if (action != null && nextFavouriteAction != action) {
            nextFavouriteAction = action;
            markChanged();
        }
    }

    public void runFavouriteAction(IgnoreAction action) {
        queue(cleanNames(NyahConfig.getData().getFavouritePlayers()), action);
    }

    public void unignoreEveryone() {
        queue(ignoredNames(), IgnoreAction.UNIGNORE);
    }

    @Subscribe
    public void onChatMessage(ChatMessageReceivedEvent event) {
        if (event.message() != null) {
            addPlayerFromChatMessage(event.message().getString());
        }
    }

    public void addPlayerFromChatMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        playerName(message, IGNORED_PATTERN)
                .or(() -> playerName(message, ALREADY_IGNORED_PATTERN))
                .ifPresent(this::markIgnoredFromChat);
        playerName(message, UNIGNORED_PATTERN).ifPresent(this::markUnignored);
    }

    private void markIgnoredFromChat(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        String trimmed = playerName.trim();
        boolean ignoredChanged = setIgnored(trimmed, true);
        boolean detectedChanged = false;
        if (!isKnownPlayer(trimmed)) {
            detectedChanged = addChatDetected(trimmed);
        }
        if (ignoredChanged || detectedChanged) {
            persistIgnoredPlayers();
            markChanged();
        }
    }

    private List<IgnorePlayerEntry> listedPlayers() {
        Map<String, IgnorePlayerEntry> entries = new LinkedHashMap<>();
        addKnownPlayers(entries, getPlayers());
        addKnownPlayers(entries, NyahConfig.getData().getFavouritePlayers());
        addKnownPlayers(entries, NyahConfig.getData().getAvoidedPlayers());
        addPersistedIgnoredPlayers(entries);
        addChatPlayers(entries, chatDetectedPlayers);
        return new ArrayList<>(entries.values());
    }

    private void addKnownPlayers(Map<String, IgnorePlayerEntry> entries, List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            entries.putIfAbsent(normalise(trimmed), player(trimmed, canChangeMode(trimmed)));
        }
    }

    private void addChatPlayers(Map<String, IgnorePlayerEntry> entries, Set<String> names) {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            entries.put(normalise(trimmed), player(trimmed, false));
        }
    }

    private void addPersistedIgnoredPlayers(Map<String, IgnorePlayerEntry> entries) {
        for (String name : NyahConfig.getData().getIgnoredPlayers()) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            String normalised = normalise(trimmed);
            if (ignoredPlayers.contains(normalised)) {
                entries.putIfAbsent(normalised, player(trimmed, false));
            }
        }
    }

    private IgnorePlayerEntry player(String playerName, boolean modeEditable) {
        return new IgnorePlayerEntry(
                playerName, modeFor(playerName), isIgnored(playerName), modeEditable);
    }

    private boolean addChatDetected(String playerName) {
        String trimmed = playerName.trim();
        boolean removed =
                chatDetectedPlayers.removeIf(name -> normalise(name).equals(normalise(trimmed)));
        boolean added = chatDetectedPlayers.add(trimmed);
        return removed || added;
    }

    private boolean removeChatDetected(String playerName) {
        String normalised = normalise(playerName);
        return chatDetectedPlayers.removeIf(name -> normalise(name).equals(normalised));
    }

    private int rowRank(IgnorePlayerEntry entry) {
        if (entry.mode() == IgnorePlayerMode.FAVOURITE) {
            return 0;
        }
        if (entry.ignored() && entry.modeEditable()) {
            return 1;
        }
        if (!entry.modeEditable()) {
            return 2;
        }
        return entry.mode().getSortOrder();
    }

    private boolean isChatDetected(String playerName) {
        String normalised = normalise(playerName);
        return chatDetectedPlayers.stream().anyMatch(name -> normalise(name).equals(normalised));
    }

    private boolean isKnownPlayer(String playerName) {
        return containsName(getPlayers(), playerName)
                || containsName(NyahConfig.getData().getFavouritePlayers(), playerName)
                || containsName(NyahConfig.getData().getAvoidedPlayers(), playerName)
                || containsName(NyahConfig.getData().getIgnoredPlayers(), playerName)
                || isChatDetected(playerName);
    }

    private boolean setIgnored(String playerName, boolean ignored) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }
        String normalised = normalise(playerName);
        if (ignored) {
            return ignoredPlayers.add(normalised);
        }
        return ignoredPlayers.remove(normalised);
    }

    private void scheduleQueuedCommands() {
        SchedulerManager.scheduleRepeating(
                safeRunnable("ignore_queue", this::runNextQueuedCommand),
                IGNORE_QUEUE_INTERVAL_TICKS,
                IGNORE_QUEUE_INTERVAL_TICKS,
                () -> false);
    }

    private void queueAction(String playerName, IgnoreAction action) {
        if (action == null || playerName == null || playerName.isBlank()) {
            return;
        }
        String trimmed = playerName.trim();
        queuedCommands.addLast(new QueuedIgnoreCommand(trimmed, action));
        boolean ignoredChanged = setIgnored(trimmed, action.isIgnoredState());
        boolean detectedChanged = !action.isIgnoredState() && removeChatDetected(trimmed);
        if (ignoredChanged || detectedChanged) {
            persistIgnoredPlayers();
            markChanged();
        }
    }

    private void runNextQueuedCommand() {
        if (isDisabled() || queuedCommands.isEmpty()) {
            return;
        }
        QueuedIgnoreCommand command = queuedCommands.removeFirst();
        sendIgnoreCommand(command.playerName(), command.action());
    }

    private void markChanged() {
        revision++;
    }

    private void loadPersistedIgnoredPlayers() {
        List<String> current = NyahConfig.getData().getIgnoredPlayers();
        List<String> saved = sortedNames(cleanNames(NyahConfig.getData().getIgnoredPlayers()));
        ignoredPlayers.clear();
        for (String playerName : saved) {
            ignoredPlayers.add(normalise(playerName));
        }
        if (!current.equals(saved)) {
            replaceSavedIgnoredPlayers(saved);
            NyahConfig.save();
        }
        if (!saved.isEmpty()) {
            markChanged();
        }
    }

    private void persistIgnoredPlayers() {
        List<String> saved = sortedNames(ignoredNames());
        if (!NyahConfig.getData().getIgnoredPlayers().equals(saved)) {
            replaceSavedIgnoredPlayers(saved);
            NyahConfig.save();
        }
    }

    private void replaceSavedIgnoredPlayers(List<String> playerNames) {
        List<String> saved = NyahConfig.getData().getIgnoredPlayers();
        saved.clear();
        saved.addAll(playerNames);
    }

    private void addName(List<String> names, String playerName) {
        if (names != null && !containsName(names, playerName)) {
            names.add(playerName);
        }
    }

    private void removeName(List<String> names, String playerName) {
        if (names != null) {
            names.removeIf(name -> normalise(name).equals(normalise(playerName)));
        }
    }

    private List<String> cleanNames(List<String> names) {
        Map<String, String> uniqueNames = new LinkedHashMap<>();
        if (names != null) {
            for (String name : names) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                String trimmed = name.trim();
                uniqueNames.putIfAbsent(normalise(trimmed), trimmed);
            }
        }
        return new ArrayList<>(uniqueNames.values());
    }

    private List<String> sortedNames(List<String> names) {
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    private List<String> ignoredNames() {
        Map<String, String> names = new LinkedHashMap<>();
        listedPlayers().stream()
                .filter(IgnorePlayerEntry::ignored)
                .forEach(entry -> names.putIfAbsent(normalise(entry.playerName()), entry.playerName()));
        ignoredPlayers.forEach(name -> names.putIfAbsent(name, name));
        return new ArrayList<>(names.values());
    }

    private boolean containsName(List<String> names, String playerName) {
        if (names == null) {
            return false;
        }
        String normalised = normalise(playerName);
        return names.stream().anyMatch(name -> normalise(name).equals(normalised));
    }

    public void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new IgnoreManagerScreen(minecraft.screen, this));
    }

    private void queue(List<String> playerNames, IgnoreAction action) {
        if (action == null) {
            return;
        }
        for (String playerName : playerNames) {
            queueAction(playerName, action);
        }
    }

    private void sendIgnoreCommand(String playerName, IgnoreAction action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }
        minecraft.getConnection().sendCommand("ignore " + action.getCommandAction() + " " + playerName);
    }

    private String normalise(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }
}
