const fs = require('fs');

const languages = [
    "English (US)", "English (UK)", "简体中文", "繁體中文", "Español", "Deutsch", "Français", "Italiano", "日本語", 
    "Afrikaans", "Shqip (Albanian)", "አማርኛ (Amharic)", "العربية (Arabic)", "Հայերեն (Armenian)", "Azərbaycan dili (Azerbaijani)", 
    "Euskara (Basque)", "Беларуская (Belarusian)", "বাংলা (Bengali)", "Bosanski (Bosnian)", "Български (Bulgarian)", 
    "Català (Catalan)", "Cebuano", "Chichewa", "Corsu (Corsican)", "Hrvatski (Croatian)", "Čeština (Czech)", "Dansk (Danish)", 
    "Nederlands (Dutch)", "Esperanto", "Eesti (Estonian)", "Filipino", "Suomi (Finnish)", "Frysk (Frisian)", "Galego (Galician)", 
    "ქართული (Georgian)", "Ελληνικά (Greek)", "ગુજરાતી (Gujarati)", "Kreyòl ayisyen (Haitian Creole)", "Hausa", "ʻŌlelo Hawaiʻi (Hawaiian)", 
    "עברית (Hebrew)", "हिन्दी (Hindi)", "Hmong", "Magyar (Hungarian)", "Íslenska (Icelandic)", "Igbo", "Bahasa Indonesia (Indonesian)", 
    "Gaeilge (Irish)", "Basa Jawa (Javanese)", "ಕನ್ನಡ (Kannada)", "Қазақ тілі (Kazakh)", "ខ្មែរ (Khmer)", "Kinyarwanda", 
    "한국어 (Korean)", "Kurdî (Kurdish)", "Кыргызча (Kyrgyz)", "ລາວ (Lao)", "Latina (Latin)", "Latviešu (Latvian)", 
    "Lietuvių (Lithuanian)", "Lëtzebuergesch (Luxembourgish)", "Македонски (Macedonian)", "Malagasy", "Bahasa Melayu (Malay)", 
    "മലയാളം (Malayalam)", "Malti (Maltese)", "Māori", "मराठी (Marathi)", "Монгол (Mongolian)", "ဗမာစာ (Burmese)", "नेपाली (Nepali)", 
    "Norsk (Norwegian)", "ଓଡ଼ିଆ (Odia)", "پښتو (Pashto)", "فارسی (Persian)", "Polski (Polish)", "Português (Portuguese)", 
    "ਪੰਜਾਬੀ (Punjabi)", "Română (Romanian)", "Русский (Russian)", "Gagana fa'a Sāmoa (Samoan)", "Gàidhlig (Scots Gaelic)", 
    "Српски (Serbian)", "Sesotho", "Shona", "سنڌي (Sindhi)", "සිංහල (Sinhala)", "Slovenčina (Slovak)", "Slovenščina (Slovenian)", 
    "Soomaali (Somali)", "Basa Sunda (Sundanese)", "Kiswahili (Swahili)", "Svenska (Swedish)", "Тоҷикӣ (Tajik)", "தமிழ் (Tamil)", 
    "Татар (Tatar)", "తెలుగు (Telugu)", "ไทย (Thai)", "Türkçe (Turkish)", "Türkmen (Turkmen)", "Українська (Ukrainian)", 
    "اردو (Urdu)", "ئۇيغۇرچە (Uyghur)", "O'zbek (Uzbek)", "Tiếng Việt (Vietnamese)", "Cymraeg (Welsh)", "isiXhosa (Xhosa)", 
    "ייִדיש (Yiddish)", "Yorùbá", "isiZulu (Zulu)"
];

const nameToCodeMap = {
    "English (US)": "en", "English (UK)": "en", "简体中文": "zh-CN", "繁體中文": "zh-TW", "Español": "es", "Deutsch": "de", "Français": "fr", "Italiano": "it", "日本語": "ja",
    "Afrikaans": "af", "Shqip (Albanian)": "sq", "አማርኛ (Amharic)": "am", "العربية (Arabic)": "ar", "Հայերեն (Armenian)": "hy", "Azərbaycan dili (Azerbaijani)": "az",
    "Euskara (Basque)": "eu", "Беларуская (Belarusian)": "be", "বাংলা (Bengali)": "bn", "Bosanski (Bosnian)": "bs", "Български (Bulgarian)": "bg",
    "Català (Catalan)": "ca", "Cebuano": "ceb", "Chichewa": "ny", "Corsu (Corsican)": "co", "Hrvatski (Croatian)": "hr", "Čeština (Czech)": "cs", "Dansk (Danish)": "da",
    "Nederlands (Dutch)": "nl", "Esperanto": "eo", "Eesti (Estonian)": "et", "Filipino": "tl", "Suomi (Finnish)": "fi", "Frysk (Frisian)": "fy", "Galego (Galician)": "gl",
    "ქართული (Georgian)": "ka", "Ελληνικά (Greek)": "el", "ગુજરાતી (Gujarati)": "gu", "Kreyòl ayisyen (Haitian Creole)": "ht", "Hausa": "ha", "ʻŌlelo Hawaiʻi (Hawaiian)": "haw",
    "עברית (Hebrew)": "iw", "हिन्दी (Hindi)": "hi", "Hmong": "hmn", "Magyar (Hungarian)": "hu", "Íslenska (Icelandic)": "is", "Igbo": "ig", "Bahasa Indonesia (Indonesian)": "id",
    "Gaeilge (Irish)": "ga", "Basa Jawa (Javanese)": "jw", "ಕನ್ನಡ (Kannada)": "kn", "Қазақ тілі (Kazakh)": "kk", "ខ្មែរ (Khmer)": "km", "Kinyarwanda": "rw",
    "한국어 (Korean)": "ko", "Kurdî (Kurdish)": "ku", "Кыргызча (Kyrgyz)": "ky", "ລາວ (Lao)": "lo", "Latina (Latin)": "la", "Latviešu (Latvian)": "lv",
    "Lietuvių (Lithuanian)": "lt", "Lëtzebuergesch (Luxembourgish)": "lb", "Македонски (Macedonian)": "mk", "Malagasy": "mg", "Bahasa Melayu (Malay)": "ms",
    "മലയാളം (Malayalam)": "ml", "Malti (Maltese)": "mt", "Māori": "mi", "मराठी (Marathi)": "mr", "Монгол (Mongolian)": "mn", "ဗမာစာ (Burmese)": "my", "नेपाली (Nepali)": "ne",
    "Norsk (Norwegian)": "no", "ଓଡ଼ିଆ (Odia)": "or", "پښتو (Pashto)": "ps", "فارسی (Persian)": "fa", "Polski (Polish)": "pl", "Português (Portuguese)": "pt",
    "ਪੰਜਾਬੀ (Punjabi)": "pa", "Română (Romanian)": "ro", "Русский (Russian)": "ru", "Gagana fa'a Sāmoa (Samoan)": "sm", "Gàidhlig (Scots Gaelic)": "gd",
    "Српски (Serbian)": "sr", "Sesotho": "st", "Shona": "sn", "سنڌي (Sindhi)": "sd", "සිංහල (Sinhala)": "si", "Slovenčina (Slovak)": "sk", "Slovenščina (Slovenian)": "sl",
    "Soomaali (Somali)": "so", "Basa Sunda (Sundanese)": "su", "Kiswahili (Swahili)": "sw", "Svenska (Swedish)": "sv", "Тоҷикӣ (Tajik)": "tg", "தமிழ் (Tamil)": "ta",
    "Татар (Tatar)": "tt", "తెలుగు (Telugu)": "te", "ไทย (Thai)": "th", "Türkçe (Turkish)": "tr", "Türkmen (Turkmen)": "tk", "Українська (Ukrainian)": "uk",
    "اردو (Urdu)": "ur", "ئۇيغۇرچە (Uyghur)": "ug", "O'zbek (Uzbek)": "uz", "Tiếng Việt (Vietnamese)": "vi", "Cymraeg (Welsh)": "cy", "isiXhosa (Xhosa)": "xh",
    "ייִדיש (Yiddish)": "yi", "Yorùbá": "yo", "isiZulu (Zulu)": "zu"
};

let output = `    val LANG_CODES = mapOf(\n`;
languages.forEach((lang, i) => {
    output += `        "${lang}" to "${nameToCodeMap[lang] || 'en'}"${i === languages.length - 1 ? '' : ','}\n`;
});
output += `    )\n`;

fs.writeFileSync('lang_map.txt', output);
