package org.sovereigntv.stvinsurance.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.managers.ConfigManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private final Main plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageUtils(Main plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(Player player, String key) {
        String message = plugin.getConfigManager().getMessage(key);
        String prefix = plugin.getConfigManager().getPrefix();
        player.sendMessage(colorize(prefix + message));
    }

    public void sendMessage(Player player, String key, String... replacements) {
        String message = plugin.getConfigManager().getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        String prefix = plugin.getConfigManager().getPrefix();
        player.sendMessage(colorize(prefix + message));
    }

    public void sendRawMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public void playSound(Player player, String soundKey) {
        if (!plugin.getConfigManager().isSoundsEnabled()) return;

        ConfigManager.SoundData soundData = plugin.getConfigManager().getSound(soundKey);
        if (soundData != null) {
            player.playSound(player.getLocation(), soundData.getSound(), soundData.getVolume(), soundData.getPitch());
        }
    }

    public static String colorize(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(colorize(message));
    }

    public static String centerMessage(String message) {
        if (message == null || message.isEmpty()) return "";

        final int CENTER_PX = 154;
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : colorize(message).toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo info = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? info.getBoldLength() : info.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();

        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }

        return sb.toString() + message;
    }

    private enum DefaultFontInfo {
        A('A', 5), a('a', 5), B('B', 5), b('b', 5), C('C', 5), c('c', 5),
        D('D', 5), d('d', 5), E('E', 5), e('e', 5), F('F', 5), f('f', 4),
        G('G', 5), g('g', 5), H('H', 5), h('h', 5), I('I', 3), i('i', 1),
        J('J', 5), j('j', 5), K('K', 5), k('k', 4), L('L', 5), l('l', 1),
        M('M', 5), m('m', 5), N('N', 5), n('n', 5), O('O', 5), o('o', 5),
        P('P', 5), p('p', 5), Q('Q', 5), q('q', 5), R('R', 5), r('r', 5),
        S('S', 5), s('s', 5), T('T', 5), t('t', 4), U('U', 5), u('u', 5),
        V('V', 5), v('v', 5), W('W', 5), w('w', 5), X('X', 5), x('x', 5),
        Y('Y', 5), y('y', 5), Z('Z', 5), z('z', 5),
        NUM_1('1', 5), NUM_2('2', 5), NUM_3('3', 5), NUM_4('4', 5), NUM_5('5', 5),
        NUM_6('6', 5), NUM_7('7', 5), NUM_8('8', 5), NUM_9('9', 5), NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1), AT_SYMBOL('@', 6), NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5), PERCENT('%', 5), UP_ARROW('^', 5), AMPERSAND('&', 5),
        ASTERISK('*', 5), LEFT_PARENTHESIS('(', 4), RIGHT_PARENTHESIS(')', 4),
        MINUS('-', 5), UNDERSCORE('_', 5), PLUS_SIGN('+', 5), EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4), RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3), RIGHT_BRACKET(']', 3),
        COLON(':', 1), SEMI_COLON(';', 1), DOUBLE_QUOTE('"', 3), SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4), RIGHT_ARROW('>', 4), QUESTION_MARK('?', 5),
        SLASH('/', 5), BACK_SLASH('\\', 5), LINE('|', 1), TILDE('~', 5),
        TICK('`', 2), PERIOD('.', 1), COMMA(',', 1), SPACE(' ', 3), DEFAULT('a', 4);

        private final char character;
        private final int length;

        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        public char getCharacter() { return character; }
        public int getLength() { return length; }
        public int getBoldLength() { return this == SPACE ? length : length + 1; }

        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo info : values()) {
                if (info.getCharacter() == c) return info;
            }
            return DEFAULT;
        }
    }
}
