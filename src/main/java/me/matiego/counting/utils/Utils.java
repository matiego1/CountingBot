package me.matiego.counting.utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import me.matiego.counting.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.minidns.DnsClient;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.record.Record;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Utils {

    public static final int SECOND = 1000;

    /**
     * Returns a {@code OkHttpClient} for the {@code JDABuilder}. <br>
     * Based on the <a href="https://github.com/DiscordSRV/DiscordSRV/blob/master/src/main/java/github/scarsz/discordsrv/DiscordSRV.java">DiscordSRV plugin</a>
     * @return the {@code OkHttpClient}
     */
    public static @NotNull OkHttpClient getHttpClient(){
        Dns dns = Dns.SYSTEM;
        try {
            List<InetAddress> fallbackDnsServers = new CopyOnWriteArrayList<>(Arrays.asList(
                    // CloudFlare resolvers
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    // Google resolvers
                    InetAddress.getByName("8.8.8.8"),
                    InetAddress.getByName("8.8.4.4")
            ));
            dns = new Dns() {
                // maybe drop minidns in favor of something else
                // https://github.com/dnsjava/dnsjava/blob/master/src/main/java/org/xbill/DNS/SimpleResolver.java
                // https://satreth.blogspot.com/2015/01/java-dns-query.html

                private final DnsClient client = new DnsClient();
                private int failedRequests = 0;
                @NotNull
                @Override
                public List<InetAddress> lookup(@NotNull String host) throws UnknownHostException {
                    int max = 5;
                    if (failedRequests < max) {
                        try {
                            List<InetAddress> result = Dns.SYSTEM.lookup(host);
                            failedRequests = 0; // reset on successful lookup
                            return result;
                        } catch (Exception e) {
                            failedRequests++;
                            Logs.error("System DNS FAILED to resolve hostname " + host + ", " + (failedRequests >= max ? "using fallback DNS for this request" : "switching to fallback DNS servers") + "!");
                        }
                    }
                    return lookupPublic(host);
                }
                private @NotNull List<InetAddress> lookupPublic(String host) throws UnknownHostException {
                    for (InetAddress dnsServer : fallbackDnsServers) {
                        try {
                            DnsMessage query = client.query(host, Record.TYPE.A, Record.CLASS.IN, dnsServer).response;
                            if (query.responseCode != DnsMessage.RESPONSE_CODE.NO_ERROR) {
                                Logs.error("DNS server " + dnsServer.getHostAddress() + " failed our DNS query for " + host + ": " + query.responseCode.name());
                            }

                            List<InetAddress> resolved = query.answerSection.stream()
                                    .map(record -> record.payloadData.toString())
                                    .map(s -> {
                                        try {
                                            return InetAddress.getByName(s);
                                        } catch (UnknownHostException e) {
                                            // impossible
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.toList());
                            if (!resolved.isEmpty()) {
                                return resolved;
                            } else {
                                Logs.error("DNS server " + dnsServer.getHostAddress() + " failed to resolve " + host + ": no results");
                            }
                        } catch (Exception e) {
                            Logs.error("DNS server " + dnsServer.getHostAddress() + " failed to resolve " + host, e);
                        }

                        // this dns server gave us an error so we move this dns server to the end of the
                        // list, effectively making it the last resort for future requests
                        fallbackDnsServers.remove(dnsServer);
                        fallbackDnsServers.add(dnsServer);
                    }

                    // this sleep is here to prevent OkHTTP from repeatedly trying to query DNS servers with no
                    // delay of it's own when internet connectivity is lost. that's extremely bad because it'll be
                    // spitting errors into the console and consuming 100% cpu
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}

                    UnknownHostException exception = new UnknownHostException("All DNS resolvers failed to resolve hostname " + host + ". Not good.");
                    exception.setStackTrace(new StackTraceElement[]{exception.getStackTrace()[0]});
                    throw exception;
                }
            };
        } catch (Exception e) {
            Logs.error("An error was encountered!", e);
        }
        return IOUtil.newHttpClientBuilder()
                .dns(dns)
                // more lenient timeouts (normally 10 seconds for these 3)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .hostnameVerifier(OkHostnameVerifier.INSTANCE)
                .build();
    }

    /**
     * Returns a set of {@code GatewayIntent}s for the {@code JDABuilder}
     * @return the set of {@code GatewayIntent}s
     */
    public static @NotNull ImmutableSet<GatewayIntent> getIntents() {
        return Sets.immutableEnumSet(EnumSet.of(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_WEBHOOKS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES
        ));
    }

    /**
     * Returns a list of disabled {@code CacheFlag}s for the {@code JDABuilder}
     * @return the list of {@code CacheFlag}s
     */
    public static @NotNull List<CacheFlag> getDisabledCacheFlag() {
        return Arrays.asList(
                CacheFlag.ACTIVITY,
                CacheFlag.VOICE_STATE,
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ONLINE_STATUS
        );
    }

    public static @NotNull List<Permission> getRequiredPermissions() {
        return Arrays.asList(
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_WEBHOOKS,
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MANAGE_CHANNEL,
                Permission.MESSAGE_HISTORY
        );
    }

    /**
     * Runs the given task async.
     * @param task the task
     */
    public static void async(@NotNull Runnable task) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), task);
        } catch (IllegalPluginAccessException ignored) {}
    }

    /**
     * Sends the webhook with the given url.
     * @param url the webhook's url
     * @param iconUrl the webhook's icon or {@code null} if you want to use default
     * @param name the webhook's name or {@code null} if you want to use default
     * @param message the webhook's message content
     * @return {@code} true if the webhook was sent successfully otherwise {@code false}
     */
    public static boolean sendWebhook(@NotNull String url, @Nullable String iconUrl, @Nullable String name, @NotNull String message) {
        try (WebhookClient client = WebhookClient.withUrl(url)) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            if (name != null) builder.setUsername(name);
            if (iconUrl != null) builder.setAvatarUrl(iconUrl);
            builder.setContent(checkLength(message, Message.MAX_CONTENT_LENGTH));
            client.send(builder.build());
            return true;
        } catch (Exception e) {
            Logs.error("An error occurred while sending the webhook", e);
        }
        return false;
    }

    /**
     * Checks the length of the given string. If it is longer than {@code maxLength}, it will change its three last characters to "...".
     * <p>
     *     Examples: <br>
     *     1234567 [maxLength = 5] -> 12... <br>
     *     1234567 [maxLength = 100] -> 1234567 <br>
     *     123 [maxLength = 3] -> ... <br>
     *     (blank string) -> "..." <br>
     * </p>
     * @param string the string to check
     * @param maxLength the max length of string
     * @return the checked string, not longer than {@code maxLength} characters.
     */
    public static @NotNull String checkLength(@NotNull String string, @Range(from = 3, to = Integer.MAX_VALUE) int maxLength) {
        if (string.isBlank()) return "...";
        return string.length() > maxLength - 3 ? string.substring(0, maxLength - 3) + "..." : string;
    }

    /**
     * Sends a private message to the user.
     * @param user the user
     * @param message the message
     */
    public static void sendPrivateMessage(@NotNull User user, @NotNull String message) {
        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(checkLength(message, Message.MAX_CONTENT_LENGTH)).queue());
    }

    /**
     * Returns an url of the member's effective avatar. If the member is {@code null} this will return an url of the user's effective avatar.
     * @param user the user
     * @param member the member
     * @return the url of the avatar
     */
    public static @NotNull String getAvatar(@NotNull User user, @Nullable Member member) {
        return (member != null) ? member.getEffectiveAvatarUrl() : user.getEffectiveAvatarUrl();
    }

    /**
     * Returns a name of the member. If the member is {@code null} this will return a name of the user.
     * @param user the user
     * @param member the member
     * @return the name
     */
    public static @NotNull String getName(@NotNull User user, @Nullable Member member) {
        return (member != null) ? member.getEffectiveName() : user.getName();
    }

    /**
     * Returns a map of all {@code DiscordLocale}s associated with the given string.
     * @param string the string
     * @return the map of {@code DiscordLocale} and string
     */
    public static @NotNull Map<DiscordLocale, String> getAllLocalizations(@NotNull String string) {
        Map<DiscordLocale, String> result = new HashMap<>();
        for (DiscordLocale value : DiscordLocale.values()) {
            if (value != DiscordLocale.UNKNOWN) result.put(value, string);
        }
        return result;
    }
}
