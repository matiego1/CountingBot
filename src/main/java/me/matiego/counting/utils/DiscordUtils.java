package me.matiego.counting.utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.ISlowmodeChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.minidns.DnsClient;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.record.Record;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiscordUtils {
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
                            DnsMessage query = client.query(host, org.minidns.record.Record.TYPE.A, Record.CLASS.IN, dnsServer).response;
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
                    // delay of its own when internet connectivity is lost. that's extremely bad because it'll be
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

    public static @NotNull ImmutableSet<GatewayIntent> getIntents() {
        return Sets.immutableEnumSet(EnumSet.of(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_WEBHOOKS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES
        ));
    }

    public static @NotNull List<CacheFlag> getDisabledCacheFlag() {
        return Arrays.asList(
                CacheFlag.ACTIVITY,
                CacheFlag.VOICE_STATE,
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.ONLINE_STATUS,
                CacheFlag.SCHEDULED_EVENTS
        );
    }

    public static @NotNull List<Permission> getRequiredPermissions() {
        return Arrays.asList(
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_WEBHOOKS,
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MANAGE_CHANNEL,
                Permission.MESSAGE_HISTORY,
                Permission.MANAGE_ROLES
        );
    }

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

    public static boolean sendWebhookToThread(long threadId, @NotNull String url, @Nullable String iconUrl, @Nullable String name, @NotNull String message) {
        try (WebhookClient client = WebhookClient.withUrl(url)) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            if (name != null) builder.setUsername(name);
            if (iconUrl != null) builder.setAvatarUrl(iconUrl);
            builder.setContent(checkLength(message, Message.MAX_CONTENT_LENGTH));
            client.onThread(threadId).send(builder.build());
            return true;
        } catch (Exception e) {
            Logs.error("An error occurred while sending the webhook", e);
        }
        return false;
    }

    private static final HashMap<Long, Long> privateMessages = Utils.createLimitedSizeMap(1000);
    public static void sendPrivateMessage(@NotNull User user, @NotNull String message) {
        user.openPrivateChannel().queue(
                privateChannel -> privateChannel.sendMessage(checkLength(message, Message.MAX_CONTENT_LENGTH)).queue(
                        success -> {},
                        failure -> {
                            if (failure instanceof ErrorResponseException e && e.getErrorCode() == 50007) {
                                long now = Utils.now();
                                if (now - privateMessages.getOrDefault(user.getIdLong(), 0L) >= 15 * 60 * 1000L) {
                                    Logs.warning("User `" + DiscordUtils.getAsTag(user) + "` doesn't allow private messages.");
                                    privateMessages.put(user.getIdLong(), now);
                                }
                            } else {
                                Logs.error("An error occurred while sending a private message.", failure);
                            }
                        })
        );
    }

    public static @NotNull String checkLength(@NotNull String string, @Range(from = 3, to = Integer.MAX_VALUE) int maxLength) {
        string = string.stripTrailing();
        if (string.isBlank()) return "...";
        if (string.length() <= maxLength) return string;
        return string.substring(0, maxLength - 3) + "...";
    }

    public static @NotNull String getAvatar(@NotNull User user, @Nullable Member member) {
        return (member != null) ? member.getEffectiveAvatarUrl() : user.getEffectiveAvatarUrl();
    }

    public static @NotNull String getName(@NotNull User user, @Nullable Member member) {
        return (member != null) ? member.getEffectiveName() : user.getName();
    }

    public static @NotNull String getMemberAsTag(@NotNull User user, @Nullable Member member) {
        String tag = user.getDiscriminator();
        return getName(user, member) + (tag.equals("0000") ? "" : "#" + tag);
    }

    public static boolean hasRequiredPermissions(@NotNull MessageChannelUnion union) {
        if (!union.getType().isGuild()) return union.canTalk();
        GuildChannel chn = union.asGuildMessageChannel();
        return chn.getGuild().getSelfMember().hasPermission(chn, getRequiredPermissions());
    }

    public static @NotNull String getAsTag(@NotNull User user) {
        return user.getDiscriminator().equals("0000") ? user.getName() : user.getAsTag();
    }

    public static boolean checkAdminKey(@Nullable String string, @NotNull User user) {
        if (string == null) return false;
        if (string.equals(Main.getInstance().getConfig().getString("admin-key"))) {
            Logs.info(getAsTag(user) + " successfully used admin key.");
            return true;
        }
        return false;
    }

    public static boolean isSupportedChannel(@NotNull MessageChannel channel) {
        if (channel.getType() == ChannelType.TEXT) return true;
        if (channel.getType() == ChannelType.GUILD_PUBLIC_THREAD && channel instanceof ThreadChannel chn) {
            return chn.getParentChannel().getType() == ChannelType.FORUM;
        }
        return false;
    }

    public static @Nullable GuildMessageChannel getSupportedChannelById(@NotNull JDA jda, long id) {
        GuildMessageChannel chn = jda.getChannelById(GuildMessageChannel.class, id);
        if (chn == null) return null;
        if (isSupportedChannel(chn)) return chn;
        return null;
    }

    public static @NotNull CompletableFuture<Webhook> getOrCreateWebhook(IWebhookContainer channel) {
        return channel.retrieveWebhooks().submit()
                .thenCompose(webhooks -> {
                    if (webhooks.isEmpty()) {
                        return channel.createWebhook("Counting Bot").submit();
                    }
                    return CompletableFuture.completedFuture(webhooks.getFirst());
                });
    }

    public static void setSlowmode(@NotNull Main plugin, @NotNull GuildMessageChannel chn) {
        if (chn instanceof ISlowmodeChannel slowmodeChannel) {
            slowmodeChannel.getManager().setSlowmode(Math.max(0, Math.min(ISlowmodeChannel.MAX_SLOWMODE, plugin.getConfig().getInt("slowmode")))).queue();
        }
    }

    public static @NotNull EmbedBuilder getOpenChannelEmbed(@NotNull ChannelData.Type type, @NotNull User user, @Nullable Member member) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("**Kanał do liczenia został otwarty!**");
        eb.setDescription("""
                **Dołącz się do wspólnego liczenia!**
                
                Typ kanału: `%s`
                Opis: `%s`
                (Szczegółowe opisy kanałów znajdziesz [tutaj](<https://github.com/matiego1/CountingBot/blob/master/README.md>))"""
                .formatted(type, type.getDescription())
        );
        eb.setColor(Utils.GREEN);
        eb.setTimestamp(Instant.now());
        eb.setFooter(DiscordUtils.getMemberAsTag(user, member), DiscordUtils.getAvatar(user, member));
        return eb;
    }
}
