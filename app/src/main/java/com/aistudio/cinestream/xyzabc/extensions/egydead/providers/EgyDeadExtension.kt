package com.aistudio.cinestream.xyzabc.extensions.egydead.providers

import com.aistudio.cinestream.xyzabc.extensions.egydead.ProviderExtension
import java.net.URLEncoder

class EgyDeadExtension : ProviderExtension {

    // 1. المعلومات الأساسية للإضافة (المعرّف، الاسم، الرابط)
    override val id: String = "egydead"
    override val name: String = "ايجي ديد"
    override val baseUrl: String = "https://tv10.egydead.live"
    
    // 2. نوع المحتوى المدعوم
    override val isAnime: Boolean = true
    override val isMovie: Boolean = true
    override val isSeries: Boolean = true
    
    // 3. لغة الموقع وأيقونة الإضافة
    override val lang: String = "ar"
    override val iconUrl: String = "https://tv10.egydead.live/wp-content/uploads/2019/01/cropped-yXYdE2f-192x192.png"

    // 4. دالة بناء رابط البحث
    override fun getSearchUrl(titleOriginal: String, titleClean: String): String {
        // نستخدم العنوان النظيف (بدون علامات) للبحث
        return "$baseUrl?s=${URLEncoder.encode(titleClean, "UTF-8")}"
    }

    // 5. دالة الجافاسكريبت التي سيتم حقنها في المتصفح الخفي للتعامل مع الموقع
    override fun getExtractionScript(isMovie: Boolean, episode: Int, title: String): String {
        return """
            (function() {
                // ========== تعريف المتغيرات المستقبلة من التطبيق الأساسي ==========
                window.targetTitle = "$title";
                window.targetEpisode = $episode;
                window.isMovie = $isMovie;

                // ========== دالة مساعدة لاستخراج بارامتر من الرابط (إن لزم الأمر) ==========
                function getQueryParam(param) {
                    let urlParams = new URLSearchParams(window.location.search);
                    return urlParams.get(param);
                }

                // ========== الخطوة الأولى: معالجة صفحة البحث ==========
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
                            // مقارنة النص المكتوب في الموقع مع العنوان المطلوب
                            if (titleText.toLowerCase() === searchTitle.toLowerCase()) {
                                found = true;
                                clearInterval(interval);
                                el.click(); // النقر على نتيجة البحث الصحيحة
                                break;
                            }
                        }
                    }, 500);

                    // إيقاف البحث بعد 15 ثانية لتجنب استهلاك الموارد إذا لم يجد شيئاً
                    setTimeout(function() {
                        clearInterval(interval);
                    }, 15000);

                    return true;
                }

                // ========== الخطوة الثانية: معالجة صفحة التفاصيل (اختيار الحلقة أو زر المشاهدة) ==========
                function handleDetails() {
                    let episodeLinks = document.querySelectorAll('.EpsList li a');

                    // إذا كان فيلماً أو لا توجد قائمة حلقات، نضغط زر "المشاهدة" المباشر
                    if (window.isMovie || episodeLinks.length === 0) {
                        let watchBtn = document.querySelector('.BtnsGroup .watchNow button');
                        if (watchBtn) {
                            watchBtn.click();
                        }
                        return true;
                    }

                    // إذا كان مسلسلاً، نبحث عن الحلقة المطلوبة
                    let targetEp = window.targetEpisode;
                    if (targetEp > 0) {
                        let found = false;
                        for (let link of episodeLinks) {
                            let epText = link.innerText.trim();
                            let match = epText.match(/\d+/);
                            let epNum = match ? parseInt(match[0], 10) : null;
                            if (epNum === targetEp) {
                                found = true;
                                link.click(); // النقر على رابط الحلقة
                                break;
                            }
                        }
                        // إذا لم نجد الحلقة المطلوبة بالرقم، نضغط على أول حلقة احتياطياً
                        if (!found && episodeLinks.length > 0) {
                            episodeLinks[0].click();
                        }
                    } else {
                        // إذا لم يحدد التطبيق رقم حلقة معينة، نفتح الحلقة الأولى
                        if (episodeLinks.length > 0) {
                            episodeLinks[0].click();
                        }
                    }
                    return true;
                }

                // ========== الخطوة الثالثة: معالجة صفحة المشاهدة وسحب السيرفرات ==========
                function handleWatchPage() {
                    // تحديد المكان الذي تتواجد فيه سيرفرات المشاهدة
                    let serverSelector = '.mob-servers ul li';
                    if (!document.querySelector(serverSelector)) {
                        serverSelector = '.serversList li';
                    }

                    let serverItems = [];
                    let interval = setInterval(function() {
                        let items = document.querySelectorAll(serverSelector);
                        if (items.length > 0) {
                            clearInterval(interval);
                            
                            // 1. استخراج سيرفرات المشاهدة
                            for (let el of items) {
                                let nameEl = el.querySelector('span p') || el.querySelector('span');
                                let name = nameEl ? nameEl.innerText.trim() : 'سيرفر';
                                let url = el.getAttribute('data-link');
                                if (url) {
                                    serverItems.push({ name: name, url: url });
                                }
                            }

                            // 2. استخراج سيرفرات التحميل (كميزة إضافية)
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

                            // 3. إرسال جميع السيرفرات (المشاهدة والتحميل) إلى التطبيق الأساسي
                            if (typeof AndroidBridge !== 'undefined' && serverItems.length > 0) {
                                AndroidBridge.sendServersV2(JSON.stringify(serverItems), window.location.href);
                            } else if (serverItems.length === 0 && typeof AndroidBridge !== 'undefined') {
                                AndroidBridge.sendFailed();
                            }
                        }
                    }, 500);

                    // إذا لم تظهر السيرفرات بعد 20 ثانية، نبلغ التطبيق بالفشل
                    setTimeout(function() {
                        clearInterval(interval);
                        if (serverItems.length === 0 && typeof AndroidBridge !== 'undefined') {
                            AndroidBridge.sendFailed();
                        }
                    }, 20000);

                    return true;
                }

                // =========================================================
                // ========== التوجيه الرئيسي: أين نحن الآن؟ ===============
                // =========================================================
                
                // 1. هل نحن في صفحة نتائج البحث؟
                if (document.querySelector('.posts-list') || document.querySelector('.pin-posts-list')) {
                    handleSearch();
                    return;
                }

                // 2. هل نحن في صفحة المشاهدة (تحتوي على قائمة سيرفرات)؟
                // (مهم جداً: يجب فحص صفحة المشاهدة قبل صفحة التفاصيل في إيجي ديد لتجنب التداخل)
                if (document.querySelector('.mob-servers') || document.querySelector('.serversList') || document.querySelector('.watchAreaMaster')) {
                    handleWatchPage();
                    return;
                }

                // 3. هل نحن في صفحة التفاصيل العادية (ولا توجد سيرفرات جاهزة)؟
                if (document.querySelector('.single-header')) {
                    handleDetails();
                    return;
                }

                // 4. إذا لم تتناسب الصفحة مع أي شرط، نبلغ التطبيق الأساسي بالفشل
                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.sendFailed();
                }
            })();
        """.trimIndent()
    }
}
