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

                // The host can inject the provider script more than once while a page loads.
                var currentPage = window.location.href;
                if (window.__egydeadPage === currentPage && window.__egydeadRunning) return;
                window.__egydeadPage = currentPage;
                window.__egydeadRunning = true;

                var startedAt = Date.now();
                var challengeStartedAt = 0;
                var lastStatus = '';
                var lastNavigationAt = 0;
                var sent = false;
                var timer = null;

                window.isMovie = $isMovie;
                window.targetEpisode = $episode;
                window.searchTitle = "$safeTitle";

                function getBridge() {
                    if (typeof window.AndroidBridge !== 'undefined') return window.AndroidBridge;
                    if (typeof window.Android !== 'undefined') return window.Android;
                    return null;
                }

                function sendStatus(status) {
                    if (lastStatus === status) return;
                    lastStatus = status;
                    var api = getBridge();
                    if (api && typeof api.sendBypassStatus === 'function') {
                        try { api.sendBypassStatus(status); } catch (e) {}
                    }
                }

                function sendOnce(items) {
                    if (sent) return;
                    var api = getBridge();
                    if (!api) return;
                    sent = true;
                    window.__egydeadRunning = false;
                    try {
                        if (items && items.length > 0 && typeof api.sendServersV2 === 'function') {
                            api.sendServersV2(JSON.stringify(items), window.location.href);
                        } else if (typeof api.sendFailed === 'function') {
                            api.sendFailed();
                        }
                    } catch (e) {
                        // Navigation can destroy the bridge. Do not send a second result.
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

                function isChallengePage() {
                    var titleText = clean(document.title).toLowerCase();
                    var bodyText = clean(document.body ? document.body.innerText : '').toLowerCase();
                    var hasChallengeElement = !!document.querySelector(
                        '#challenge-running, #challenge-stage, #challenge-form, .cf-turnstile, .cf-turnstile-wrapper, ' +
                        'iframe[src*="challenges.cloudflare.com"], iframe[title*="challenge" i]'
                    );
                    var hasChallengeTitle = titleText === 'just a moment...' ||
                        titleText === 'just a moment' ||
                        titleText.indexOf('attention required') !== -1;
                    var hasChallengeText = bodyText.indexOf('checking your browser') !== -1 ||
                        bodyText.indexOf('verify you are human') !== -1 ||
                        bodyText.indexOf('performing security verification') !== -1 ||
                        bodyText.indexOf('يتم التحقق') !== -1 ||
                        bodyText.indexOf('تحقق من أنك لست روبوت') !== -1;
                    return hasChallengeElement || hasChallengeTitle || hasChallengeText;
                }

                function challengeExpired() {
                    if (!challengeStartedAt) challengeStartedAt = Date.now();
                    return Date.now() - challengeStartedAt >= 90000;
                }

                function searchPageReady() {
                    return !!document.querySelector('.posts-list, .pin-posts-list, a.post-item');
                }

                function detailsPageReady() {
                    return !!document.querySelector('.single-header, .EpsList, .BtnsGroup, .watchNow');
                }

                function watchPageReady() {
                    return !!document.querySelector('.mob-servers, .serversList, .watchAreaMaster, .donwload-servers-list, .download-servers-list');
                }

                function findSearchResult() {
                    var items = document.querySelectorAll('.posts-list a.postBox, .pin-posts-list a.postBox, a.post-item');
                    if (!items.length) return null;

                    var wanted = normalize(window.searchTitle).split(/\\s+/).filter(function(part) {
                        return part.length > 1;
                    });
                    var best = null;
                    var bestScore = -1;

                    for (var i = 0; i < items.length; i++) {
                        var item = items[i];
                        var titleEl = item.querySelector('h1.BottomTitle, .post-title, h2, h3');
                        var text = normalize(titleEl ? (titleEl.innerText || titleEl.textContent) : item.innerText);
                        var score = 0;
                        for (var j = 0; j < wanted.length; j++) {
                            if (text.indexOf(wanted[j]) !== -1) score++;
                        }
                        score -= i * 0.01;
                        if (score > bestScore) {
                            bestScore = score;
                            best = item;
                        }
                    }

                    if (!best || (wanted.length > 0 && bestScore < 1)) return null;
                    return absoluteUrl(best.href || best.getAttribute('href'));
                }

                function navigateOnce(url) {
                    if (!url || Date.now() - lastNavigationAt < 1200) return;
                    lastNavigationAt = Date.now();
                    window.location.href = url;
                }

                function handleSearch() {
                    var result = findSearchResult();
                    if (result) {
                        navigateOnce(result);
                    } else if (Date.now() - startedAt >= 30000) {
                        sendOnce([]);
                    }
                }

                function parseEpisode(text) {
                    var match = String(text || '').match(/(?:episode|ep|الحلقة|حلقة|e)?\\s*0*(\\d+)/i);
                    return match ? parseInt(match[1], 10) : null;
                }

                function handleDetails() {
                    var links = document.querySelectorAll('.EpsList li a, .episodes-list li a, a[href*="episode"]');
                    var selected = null;

                    if (!window.isMovie && window.targetEpisode > 0) {
                        for (var i = 0; i < links.length; i++) {
                            if (parseEpisode(links[i].innerText) === window.targetEpisode) {
                                selected = links[i];
                                break;
                            }
                        }
                    }

                    if (!selected && !window.isMovie && links.length > 0) selected = links[0];
                    if (!selected || window.isMovie) {
                        selected = document.querySelector(
                            '.BtnsGroup .watchNow a, .BtnsGroup .watchNow button, .watchNow a, .watchNow button'
                        );
                    }

                    if (selected) {
                        var href = absoluteUrl(selected.href || selected.getAttribute('href'));
                        if (href) navigateOnce(href);
                        else if (typeof selected.click === 'function' && Date.now() - lastNavigationAt >= 1200) {
                            lastNavigationAt = Date.now();
                            selected.click();
                        }
                    } else if (Date.now() - startedAt >= 30000) {
                        sendOnce([]);
                    }
                }

                function addServer(list, name, value) {
                    var url = absoluteUrl(value);
                    if (!url) return;
                    for (var i = 0; i < list.length; i++) {
                        if (list[i].link === url) return;
                    }
                    list.push({ name: clean(name) || 'سيرفر', link: url });
                }

                function extractServers() {
                    var result = [];
                    var items = document.querySelectorAll('.mob-servers li, .serversList li');

                    for (var i = 0; i < items.length; i++) {
                        var item = items[i];
                        var nameEl = item.querySelector('span p, span, .server-name');
                        var name = nameEl ? nameEl.innerText : item.innerText;
                        addServer(result, name, item.getAttribute('data-link'));
                        addServer(result, name, item.getAttribute('data-src'));
                        addServer(result, name, item.getAttribute('data-server'));
                        var link = item.querySelector('a[href]');
                        if (link) addServer(result, name, link.href);
                    }

                    var downloads = document.querySelectorAll('.donwload-servers-list li, .download-servers-list li');
                    for (var j = 0; j < downloads.length; j++) {
                        var download = downloads[j];
                        var nameNode = download.querySelector('.ser-name');
                        var qualityNode = download.querySelector('.server-info em');
                        var linkNode = download.querySelector('a.ser-link, a[href]');
                        if (linkNode) {
                            var label = nameNode ? nameNode.innerText : 'تحميل';
                            if (qualityNode) label += ' (' + clean(qualityNode.innerText) + ')';
                            addServer(result, label, linkNode.href);
                        }
                    }

                    return result;
                }

                function handleWatchPage() {
                    var servers = extractServers();
                    if (servers.length > 0) {
                        sendOnce(servers);
                    } else if (Date.now() - startedAt >= 35000) {
                        sendOnce([]);
                    }
                }

                function tick() {
                    if (sent) return;

                    if (isChallengePage()) {
                        if (!challengeStartedAt) challengeStartedAt = Date.now();
                        sendStatus('CLOUDFLARE');
                        if (challengeExpired()) sendOnce([]);
                        return;
                    }

                    if (challengeStartedAt) {
                        challengeStartedAt = 0;
                        sendStatus('NORMAL');
                    } else if (lastStatus !== 'NORMAL') {
                        sendStatus('NORMAL');
                    }

                    if (watchPageReady()) {
                        handleWatchPage();
                    } else if (searchPageReady()) {
                        handleSearch();
                    } else if (detailsPageReady()) {
                        handleDetails();
                    } else if (Date.now() - startedAt >= 35000) {
                        sendOnce([]);
                    }
                }

                timer = setInterval(tick, 500);
                tick();
                setTimeout(function() {
                    if (timer) clearInterval(timer);
                    if (!sent && !isChallengePage()) sendOnce([]);
                }, 95000);
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
