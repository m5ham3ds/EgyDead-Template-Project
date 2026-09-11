package com.aistudio.cinestream.xyzabc.extensions.egydead.providers

import com.example.extensions.ProviderExtension

class EgyDeadExtension : ProviderExtension {
    override val id = "egydead"
    override val name = "EgyDead"
    override val baseUrl = "https://egydead.rest"
    override val isAnime = false
    override val isMovie = true
    override val isSeries = true
    override val lang = "ar"
    override val iconUrl = "https://egydead.rest/wp-content/themes/egydead/images/logo.png"

    override fun getSearchUrl(titleOriginal: String, titleClean: String): String {
        // نستخدم titleClean الذي يأتي مدمجاً معه سنة الإصدار إن وجدت لضمان دقة البحث
        return "$baseUrl/?s=${titleClean.replace(" ", "+")}"
    }

    override fun getExtractionScript(isMovie: Boolean, episode: Int, title: String): String {
        return """
            (function() {
                window.isMovie = $isMovie;
                window.targetEpisode = $episode;
                window.searchTitle = '${title.replace("'", "\\'")}';
                
                // ========== الخطوة الأولى: معالجة صفحة البحث ==========
                function handleSearch() {
                    let interval = setInterval(function() {
                        let items = document.querySelectorAll('.posts-list a.postBox, .pin-posts-list a.postBox, a.post-item');
                        if (items.length === 0) return;
                        
                        let found = false;
                        // تقسيم العنوان المطلوب إلى كلمات منفصلة
                        let searchParts = window.searchTitle.toLowerCase().split(/\s+/).filter(p => p.length > 0);
                        
                        for (let el of items) {
                            let titleEl = el.querySelector('h1.BottomTitle') || el.querySelector('.post-title');
                            if (!titleEl) continue;
                            let titleText = titleEl.innerText.trim().toLowerCase();
                            
                            // نتحقق من أن جميع كلمات البحث موجودة في عنوان النتيجة
                            let matchesAll = true;
                            for (let part of searchParts) {
                                if (!titleText.includes(part)) {
                                    matchesAll = false;
                                    break;
                                }
                            }
                            
                            if (matchesAll) {
                                found = true;
                                clearInterval(interval);
                                el.click(); // النقر على نتيجة البحث الصحيحة
                                break;
                            }
                        }
                    }, 500);

                    // إيقاف البحث بعد 15 ثانية
                    setTimeout(function() {
                        clearInterval(interval);
                    }, 15000);
                    return true;
                }

                // ========== الخطوة الثانية: معالجة صفحة التفاصيل ==========
                function handleDetails() {
                    let episodeLinks = document.querySelectorAll('.EpsList li a');

                    if (window.isMovie || episodeLinks.length === 0) {
                        let watchBtn = document.querySelector('.BtnsGroup .watchNow button');
                        if (watchBtn) watchBtn.click();
                        return true;
                    }

                    let targetEp = window.targetEpisode;
                    if (targetEp > 0) {
                        let found = false;
                        for (let link of episodeLinks) {
                            let epText = link.innerText.trim();
                            let match = epText.match(/\d+/);
                            let epNum = match ? parseInt(match[0], 10) : null;
                            if (epNum === targetEp) {
                                found = true;
                                link.click();
                                break;
                            }
                        }
                        if (!found && episodeLinks.length > 0) episodeLinks[0].click();
                    } else {
                        if (episodeLinks.length > 0) episodeLinks[0].click();
                    }
                    return true;
                }

                // ========== الخطوة الثالثة: سحب السيرفرات ==========
                function handleWatchPage() {
                    let serverSelector = '.mob-servers ul li';
                    if (!document.querySelector(serverSelector)) {
                        serverSelector = '.serversList li';
                    }

                    let serverItems = [];
                    let interval = setInterval(function() {
                        let items = document.querySelectorAll(serverSelector);
                        
                        if (items.length > 0) {
                            clearInterval(interval);
                            
                            for (let el of items) {
                                let nameEl = el.querySelector('span p') || el.querySelector('span');
                                let name = nameEl ? nameEl.innerText.trim() : 'سيرفر';
                                let url = el.getAttribute('data-link');
                                if (url) {
                                    serverItems.push({ name: name, url: url });
                                }
                            }

                            let dlItems = document.querySelectorAll('.donwload-servers-list li');
                            for (let dl of dlItems) {
                                let nameEl = dl.querySelector('.ser-name');
                                let qualityEl = dl.querySelector('.server-info em');
                                let linkEl = dl.querySelector('a.ser-link');
                                if (linkEl && linkEl.href) {
                                    let name = nameEl ? nameEl.innerText.trim() : 'تحميل';
                                    if (qualityEl) name += ' (' + qualityEl.innerText.trim() + ')';
                                    serverItems.push({ name: name, url: linkEl.href });
                                }
                            }

                            if (typeof AndroidBridge !== 'undefined' && serverItems.length > 0) {
                                AndroidBridge.sendServersV2(JSON.stringify(serverItems), window.location.href);
                            } else if (serverItems.length === 0 && typeof AndroidBridge !== 'undefined') {
                                AndroidBridge.sendFailed();
                            }
                        }
                    }, 500);

                    setTimeout(function() {
                        clearInterval(interval);
                        if (serverItems.length === 0 && typeof AndroidBridge !== 'undefined') {
                            AndroidBridge.sendFailed();
                        }
                    }, 20000);
                    return true;
                }

                // التوجيه الرئيسي
                if (document.querySelector('.posts-list') || document.querySelector('.pin-posts-list')) {
                    handleSearch();
                    return;
                }

                if (document.querySelector('.mob-servers') || document.querySelector('.serversList') || document.querySelector('.watchAreaMaster')) {
                    handleWatchPage();
                    return;
                }

                if (document.querySelector('.single-header')) {
                    handleDetails();
                    return;
                }

                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.sendFailed();
                }

            })();
        """.trimIndent()
    }
}
