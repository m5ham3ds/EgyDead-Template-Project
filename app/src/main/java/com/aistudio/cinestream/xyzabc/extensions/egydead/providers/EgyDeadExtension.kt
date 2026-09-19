package com.aistudio.cinestream.xyzabc.extensions.egydead.providers

import com.aistudio.cinestream.xyzabc.extensions.egydead.ProviderExtension
import java.net.URLEncoder

class EgyDeadExtension : ProviderExtension {
    override val id = "egydead"
    override val name = "EgyDead"
    override val baseUrl = "https://tv10.egydead.live"
    override val isAnime = false
    override val isMovie = true
    override val isSeries = true
    override val lang = "ar"
    override val iconUrl = "https://tv10.egydead.live/wp-content/themes/egydead/images/logo.png"

    override fun getSearchUrl(titleOriginal: String, titleClean: String): String {
        val query = normalizeTitle(titleClean.ifBlank { titleOriginal })
        return "$baseUrl/?s=${URLEncoder.encode(query, "UTF-8")}" 
    }

    override fun getExtractionScript(isMovie: Boolean, episode: Int, title: String): String {
        val safeTitle = escapeForJs(normalizeTitle(title))

        return """
            (function() {
                'use strict';

                // The host application may inject the script more than once while a page loads.
                var pageKey = window.location.href;
                if (window.__egydeadInjectedPage === pageKey) return;
                window.__egydeadInjectedPage = pageKey;

                var sent = false;
                var startedAt = Date.now();
                var challengeStartedAt = 0;
                var pollTimer = null;
                var lastActionAt = 0;

                window.isMovie = $isMovie;
                window.targetEpisode = $episode;
                window.searchTitle = "$safeTitle";

                function bridge() {
                    if (typeof window.AndroidBridge !== 'undefined') return window.AndroidBridge;
                    if (typeof window.Android !== 'undefined') return window.Android;
                    return null;
                }

                function sendOnce(items) {
                    if (sent) return;
                    var api = bridge();
                    if (!api) return;
                    sent = true;
                    try {
                        if (items && items.length > 0 && typeof api.sendServersV2 === 'function') {
                            api.sendServersV2(JSON.stringify(items), window.location.href);
                        } else if (typeof api.sendFailed === 'function') {
                            api.sendFailed();
                        }
                    } catch (e) {
                        // The host bridge can disappear during navigation; never retry indefinitely.
                    }
                }

                function clean(value) {
                    return String(value || '').replace(/\\s+/g, ' ').trim();
                }

                function normalize(value) {
                    return clean(value)
                        .normalize('NFKC')
                        .replace(/[^\\p{L}\\p{N}\\s]/gu, ' ')
                        .replace(/\\s+/g, ' ')
                        .trim()
                        .toLowerCase();
                }

                function absoluteUrl(value) {
                    if (!value) return null;
                    try {
                        var url = new URL(String(value), window.location.href);
                        if (url.protocol !== 'http:' && url.protocol !== 'https:') return null;
                        return url.href;
                    } catch (e) {
                        return null;
                    }
                }

                function isCloudflarePage() {
                    var text = ((document.title || '') + ' ' + (document.body ? document.body.innerText : '')).toLowerCase();
                    var url = window.location.href.toLowerCase();
                    return !!(
                        document.querySelector('#challenge-running, #challenge-stage, .cf-challenge, .cf-turnstile, [name="cf-turnstile-response"]') ||
                        document.querySelector('iframe[src*="challenges.cloudflare.com"], iframe[title*="challenge" i]') ||
                        url.indexOf('/cdn-cgi/challenge-platform/') !== -1 ||
                        text.indexOf('just a moment') !== -1 ||
                        text.indexOf('checking your browser') !== -1 ||
                        text.indexOf('verify you are human') !== -1 ||
                        text.indexOf('performing security verification') !== -1 ||
                        text.indexOf('cloudflare') !== -1
                    );
                }

                function challengeTimedOut() {
                    if (!challengeStartedAt) challengeStartedAt = Date.now();
                    return Date.now() - challengeStartedAt > 60000;
                }

                function hasSearchPage() {
                    return !!document.querySelector('.posts-list, .pin-posts-list, a.post-item');
                }

                function hasDetailsPage() {
                    return !!document.querySelector('.single-header, .EpsList, .BtnsGroup, .watchNow');
                }

                function hasWatchPage() {
                    return !!document.querySelector('.mob-servers, .serversList, .watchAreaMaster, .donwload-servers-list');
                }

                function findBestSearchLink() {
                    var items = document.querySelectorAll('.posts-list a.postBox, .pin-posts-list a.postBox, a.post-item');
                    if (!items || items.length === 0) return null;

                    var wanted = normalize(window.searchTitle).split(/\\s+/).filter(function(part) {
                        return part.length > 1;
                    });
                    var best = null;
                    var bestScore = -1;

                    for (var i = 0; i < items.length; i++) {
                        var el = items[i];
                        var titleEl = el.querySelector('h1.BottomTitle, .post-title, h2, h3');
                        var titleText = normalize(titleEl ? (titleEl.innerText || titleEl.textContent) : el.innerText);
                        var score = 0;
                        for (var j = 0; j < wanted.length; j++) {
                            if (titleText.indexOf(wanted[j]) !== -1) score++;
                        }
                        score -= i * 0.01;
                        if (score > bestScore) {
                            bestScore = score;
                            best = el;
                        }
                    }

                    if (!best || (wanted.length > 0 && bestScore < 1)) return null;
                    return absoluteUrl(best.href || best.getAttribute('href'));
                }

                function handleSearch() {
                    var link = findBestSearchLink();
                    if (link) {
                        if (Date.now() - lastActionAt < 1000) return;
                        lastActionAt = Date.now();
                        window.location.href = link;
                        return;
                    }
                    if (Date.now() - startedAt > 20000) sendOnce([]);
                }

                function episodeNumber(text) {
                    var match = String(text || '').match(/(?:episode|ep|الحلقة|حلقة|e)?\\s*0*(\\d+)/i);
                    return match ? parseInt(match[1], 10) : null;
                }

                function handleDetails() {
                    var links = document.querySelectorAll('.EpsList li a, .episodes-list li a, a[href*="episode"]');
                    var selected = null;

                    if (!window.isMovie && window.targetEpisode > 0) {
                        for (var i = 0; i < links.length; i++) {
                            if (episodeNumber(links[i].innerText) === window.targetEpisode) {
                                selected = links[i];
                                break;
                            }
                        }
                    }
                    if (!selected && links.length > 0 && !window.isMovie) selected = links[0];

                    if (!selected || window.isMovie) {
                        selected = document.querySelector('.BtnsGroup .watchNow a, .BtnsGroup .watchNow button, .watchNow a, .watchNow button');
                    }

                    if (selected && Date.now() - lastActionAt > 1000) {
                        lastActionAt = Date.now();
                        var href = absoluteUrl(selected.href || selected.getAttribute('href'));
                        if (href) window.location.href = href;
                        else if (typeof selected.click === 'function') selected.click();
                        return;
                    }
                    if (Date.now() - startedAt > 20000) sendOnce([]);
                }

                function addServer(list, name, value) {
                    var url = absoluteUrl(value);
                    if (!url) return;
                    for (var i = 0; i < list.length; i++) {
                        if (list[i].url === url) return;
                    }
                    list.push({ name: clean(name) || 'سيرفر', url: url });
                }

                function extractServers() {
                    var result = [];
                    var items = document.querySelectorAll('.mob-servers li, .serversList li');
                    for (var i = 0; i < items.length; i++) {
                        var item = items[i];
                        var nameEl = item.querySelector('span p, span, .server-name');
                        addServer(result, nameEl ? nameEl.innerText : 'سيرفر', item.getAttribute('data-link') || item.getAttribute('data-url'));
                        var link = item.querySelector('a[href]');
                        if (link) addServer(result, nameEl ? nameEl.innerText : 'سيرفر', link.href);
                    }

                    var downloads = document.querySelectorAll('.donwload-servers-list li, .download-servers-list li');
                    for (var j = 0; j < downloads.length; j++) {
                        var dl = downloads[j];
                        var name = dl.querySelector('.ser-name');
                        var quality = dl.querySelector('.server-info em');
                        var linkEl = dl.querySelector('a.ser-link, a[href]');
                        if (linkEl) {
                            var label = name ? name.innerText : 'تحميل';
                            if (quality) label += ' (' + clean(quality.innerText) + ')';
                            addServer(result, label, linkEl.href);
                        }
                    }
                    return result;
                }

                function handleWatchPage() {
                    var result = extractServers();
                    if (result.length > 0) {
                        sendOnce(result);
                        return;
                    }
                    if (Date.now() - startedAt > 25000) sendOnce([]);
                }

                function tick() {
                    if (sent) return;

                    if (isCloudflarePage()) {
                        if (!challengeStartedAt) challengeStartedAt = Date.now();
                        if (challengeTimedOut()) sendOnce([]);
                        return;
                    }
                    challengeStartedAt = 0;

                    if (hasWatchPage()) {
                        handleWatchPage();
                    } else if (hasSearchPage()) {
                        handleSearch();
                    } else if (hasDetailsPage()) {
                        handleDetails();
                    } else if (Date.now() - startedAt > 25000) {
                        sendOnce([]);
                    }
                }

                pollTimer = setInterval(tick, 500);
                tick();
                setTimeout(function() {
                    if (pollTimer) clearInterval(pollTimer);
                    if (!sent && !isCloudflarePage()) sendOnce([]);
                }, 65000);
            })();
        """.trimIndent()
    }

    private fun normalizeTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun escapeForJs(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
    }
}
