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
        // نمرر المعاملات إلى الجافا سكريبت كمتغيرات عامة
        return """
            (function() {
                // ========== تعريف المتغيرات المستقبلة من Kotlin ==========
                window.targetTitle = "$title";
                window.targetEpisode = $episode;
                window.isMovie = $isMovie;

                // ========== دالة مساعدة لاستخراج بارامتر من الرابط ==========
                function getQueryParam(param) {
                    let urlParams = new URLSearchParams(window.location.search);
                    return urlParams.get(param);
                }

                // ========== الخطوة 2: معالجة البحث (باستخدام targetTitle) ==========
                function handleSearch() {
                    // لا نعتمد على بارامتر s، بل نستخدم العنوان الذي مررناه مباشرة
                    let searchTitle = window.targetTitle;
                    if (!searchTitle) return false;

                    let interval = setInterval(function() {
                        let items = document.querySelectorAll('.movieItem a');
                        let found = false;
                        for (let el of items) {
                            let titleEl = el.querySelector('h1.BottomTitle');
                            if (!titleEl) continue;
                            let titleText = titleEl.innerText.trim();
                            // مقارنة غير حساسة لحالة الأحرف مع العنوان المستلم
                            if (titleText.toLowerCase() === searchTitle.toLowerCase()) {
                                found = true;
                                clearInterval(interval);
                                el.click();
                                break;
                            }
                        }
                    }, 500);

                    setTimeout(function() {
                        clearInterval(interval);
                    }, 15000);

                    return true;
                }

                // ========== الخطوة 3: معالجة صفحة التفاصيل ==========
                function handleDetails() {
                    if (!document.querySelector('.single-header')) return false;

                    let episodeLinks = document.querySelectorAll('.EpsList li a');

                    // إذا كان فيلماً أو لا توجد قائمة حلقات، نضغط زر المشاهدة مباشرة
                    if (window.isMovie || episodeLinks.length === 0) {
                        let watchBtn = document.querySelector('.BtnsGroup .watchNow button');
                        if (watchBtn) {
                            watchBtn.click();
                        }
                        return true;
                    }

                    // إذا كان مسلسلاً ولدينا رقم حلقة، نبحث عنها
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
                        // إذا لم نجد الحلقة، نضغط على أول حلقة كحل احتياطي
                        if (!found && episodeLinks.length > 0) {
                            episodeLinks[0].click();
                        }
                    } else {
                        // إذا لم يحدد رقم حلقة، نضغط أول حلقة
                        if (episodeLinks.length > 0) {
                            episodeLinks[0].click();
                        }
                    }
                    return true;
                }

                // ========== الخطوة 4: معالجة صفحة المشاهدة واستخلاص السيرفرات ==========
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

                // ========== تنفيذ المنطق حسب نوع الصفحة ==========
                // 1. هل هي صفحة بحث؟
                if (document.querySelector('.posts-list') || document.querySelector('.pin-posts-list')) {
                    handleSearch();
                    return;
                }

                // 2. هل هي صفحة تفاصيل؟
                if (document.querySelector('.single-header')) {
                    handleDetails();
                    return;
                }

                // 3. هل هي صفحة مشاهدة؟
                if (document.querySelector('.mob-servers') || document.querySelector('.serversList') || document.querySelector('.watchAreaMaster')) {
                    handleWatchPage();
                    return;
                }

                // إذا لم تتناسب مع أي نوع
                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.sendFailed();
                }
            })();
        """.trimIndent()
    }
}