package com.goreecloud.browsermobile.core

import java.io.Reader
import java.net.URI
import java.util.Locale

/** Fixed, reviewed HTTPS sources. Arbitrary subscription URLs are not accepted. */
enum class FilterFormat { DOMAINS, HOSTS }

enum class FilterSource(
    val title: String,
    val summary: String,
    val url: String,
    val format: FilterFormat,
    val enabledByDefault: Boolean
) {
    OISD_SMALL(
        "OISD Small", "Primary ad-domain list · recommended",
        "https://small.oisd.nl/domainswild2", FilterFormat.DOMAINS, true
    ),
    STEVENBLACK(
        "StevenBlack Unified", "Ads, trackers, malicious hosts",
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", FilterFormat.HOSTS, true
    ),
    OISD_BIG(
        "OISD Big", "Broader blocking · replaces OISD Small",
        "https://big.oisd.nl/domainswild2", FilterFormat.DOMAINS, false
    ),
    OISD_NSFW_SMALL(
        "OISD NSFW Small", "Optional adult/shock-domain blocking",
        "https://nsfw-small.oisd.nl/domainswild2", FilterFormat.DOMAINS, false
    ),
    STEVENBLACK_GAMBLING(
        "StevenBlack Gambling", "Optional gambling-domain blocking · includes Unified",
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/gambling/hosts",
        FilterFormat.HOSTS, false
    )
}

/** A bounded parser. Unsupported cosmetic/ABP/regex syntax is ignored, not misapplied. */
object DomainFilterParser {
    const val MAX_LINES = 500_000
    private val label = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
    private val sinkIPs = setOf("0.0.0.0", "127.0.0.1", "::", "::1")
    private val splitWhitespace = Regex("\\s+")

    fun parse(reader: Reader, format: FilterFormat): Set<String> {
        val domains = HashSet<String>()
        reader.buffered().use { stream ->
            var lines = 0
            while (true) {
                val line = stream.readLine() ?: break
                if (++lines > MAX_LINES) throw IllegalArgumentException("Too many filter rules")
                val content = line.substringBefore('#').trim()
                if (content.isEmpty() || content.startsWith("!") || content.startsWith("[")) continue
                val tokens = content.split(splitWhitespace)
                if (format == FilterFormat.DOMAINS) {
                    if (tokens.size == 1) normalizedDomain(tokens[0])?.let(domains::add)
                } else if (tokens.size > 1 && tokens[0] in sinkIPs) {
                    tokens.drop(1).forEach { normalizedDomain(it)?.let(domains::add) }
                }
            }
        }
        return domains
    }

    fun normalizedDomain(raw: String): String? {
        val host = raw.lowercase(Locale.ROOT).trimEnd('.')
        if (host.length !in 4..253 || !host.contains('.') || host.contains("..")) return null
        val labels = host.split('.')
        if (labels.size < 2 || labels.any { it.length !in 1..63 || !label.matches(it) }) return null
        if (labels.last().all(Char::isDigit)) return null
        return host
    }
}

/** Match only at hostname label boundaries. */
class DomainRuleMatcher(private val blockedDomains: Set<String>) {
    val count: Int get() = blockedDomains.size

    fun matchesHost(host: String): Boolean {
        val domain = DomainFilterParser.normalizedDomain(host) ?: return false
        if (domain in blockedDomains) return true
        var dot = domain.indexOf('.')
        while (dot >= 0) {
            if (domain.substring(dot + 1) in blockedDomains) return true
            dot = domain.indexOf('.', dot + 1)
        }
        return false
    }

    fun matchesUrl(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) return false
        return uri.host?.let(::matchesHost) ?: false
    }
}
