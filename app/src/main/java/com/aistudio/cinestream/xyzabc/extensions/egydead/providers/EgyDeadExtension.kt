package com.aistudio.cinestream.xyzabc.extensions.egydead.providers

import com.aistudio.cinestream.xyzabc.extensions.egydead.ProviderExtension
import java.net.URLEncoder

class EgyDeadExtension : ProviderExtension {

    override val id: String = "egydead"
    override val name: String = "ايجي ديد"
    override val baseUrl: String = "https://tv10.egydead.live"
    override val isAnime: Boolean = true
    override val isMovie: Boolean = true
    override val isSeries: Boolean = true
    override val lang: String = "ar"
    override val iconUrl: String = "https://tv10.egydead.live/wp-content/uploads/2019/01/cropped-yXYdE2f-192x192.png"

    override fun getSearchUrl(titleOriginal: String, titleClean: String): String {
        // نستخدم العنوان النظيف (بدون علامات) للبحث
        return "$baseUrl?s=${URLEncoder.encode(titleClean, "UTF-8")}"
    }

    override fun getExtractionScript(isMovie: Boolean, episode: Int, title: String): String {
        return """
            (function() {
                window.targetTitle = "$title";
                window.targetEpisode = $episode;
                window.isMovie = $isMovie;

                function handleSearch() {
                    let searchTitle = window.targetTitle;
                    if (!searchTitle) return false;

                    let interval = setInterval(function() {
                        let items = document.querySelectorAll('.movieItem a');
                        let found = false;
                        for (let el of items) {
                            let titleEl = el.querySelector('h1.BottomTitle');
                            if (!titleEl) continue;
                            let titleText = titleEl.innerText.trim();
                            
                            // تم إصلاح التطابق ليكون بحث جزئي (includes) بدلاً من تطابق تام
                            if (titleText.toLowerCase().includes(searchTitle.toLowerCase())) {
                                found = true;
                                clearInterval(interval);
                                el.click();
                                break;
                            }
                        }
                    }, 500);

                    setTimeout(function() { clearInterval(interval); }, 15000);
                    return true;
                }

                function handleDetails() {
                    if (!document.querySelector('.single-header')) return false;

                    let episodeLinks = document.querySelectorAll('.EpsList li a');

                    if (window.isMovie || episodeLinks.length === 0) {
                        let watchBtn = document.querySelector('.BtnsGroup .watchNow button');
                        if (watchBtn) {
                            watchBtn.click();
                        }
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
                        if (!found && episodeLinks.length > 0) {
                            episodeLinks[0].click();
                        }
                    } else {
                        if (episodeLinks.length > 0) {
                            episodeLinks[0].click();
                        }
                    }
                    return true;
                }

                function handleWatchPage() {
                    let serverItems = [];
                    
                    let interval = setInterval(function() {
                        let items = document.querySelectorAll('.mob-servers ul li, .serversList li');
                        let downloadItems = document.querySelectorAll('.donwload-servers-list li');
                        
                        if (items.length > 0 || downloadItems.length > 0) {
                            clearInterval(interval);
                            
                            // 1. سحب سيرفرات المشاهدة
                            for (let el of items) {
                                let nameEl = el.querySelector('span p') || el.querySelector('span');
                                let name = nameEl ? nameEl.innerText.trim() : 'سيرفر مشاهدة';
                                let url = el.getAttribute('data-link');
                                if (url && url.includes('http')) {
                                    serverItems.push({ name: name, link: url }); // إصلاح: استخدام link بدلاً من url
                                }
                            }
                            
                            // 2. سحب سيرفرات التحميل وإضافتها للقائمة
                            for(let el of downloadItems) {
                                let nameEl = el.querySelector('.ser-name');
                                let name = nameEl ? nameEl.innerText.trim() : 'سيرفر تحميل';
                                let qEl = el.querySelector('.server-info em');
                                if(qEl) name += ' (' + qEl.innerText.trim() + ')';
                                
                                let linkEl = el.querySelector('a.ser-link');
                                if(linkEl && linkEl.href) {
                                    serverItems.push({ name: name, link: linkEl.href }); // إصلاح: استخدام link بدلاً من url
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

                if (document.querySelector('.posts-list') || document.querySelector('.pin-posts-list')) {
                    handleSearch(); return;
                }
                if (document.querySelector('.single-header')) {
                    handleDetails(); return;
                }
                if (document.querySelector('.mob-servers') || document.querySelector('.serversList') || document.querySelector('.watchAreaMaster')) {
                    handleWatchPage(); return;
                }
                
                if (typeof AndroidBridge !== 'undefined') { AndroidBridge.sendFailed(); }
            })();
        """.trimIndent()
    }
}
