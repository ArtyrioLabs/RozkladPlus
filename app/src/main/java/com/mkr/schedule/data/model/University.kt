package com.mkr.schedule.data.model

data class University(
    val id: String,
    val name: String,
    val shortName: String,
    val baseUrl: String,
    val portalUrl: String? = null,
    val needAuth: Boolean = false,
    val structureCode: Int = 0,
    val currentError: String? = null
) {
    companion object {
        val MKR_UNIVERSITIES = listOf(
            // Default: SUTE / DTEU (Kyiv)
            University(
                id = "50",
                name = "Державний торговельно-економічний університет",
                shortName = "ДТЕУ",
                baseUrl = "https://mia.mobil.knute.edu.ua",
                portalUrl = "https://mia1.knute.edu.ua/"
            ),
            University("13", "Национальный фармацевтический университет (г. Харьков)", "НФаУ", "http://smsapi.nuph.edu.ua", "https://portal.nuph.edu.ua", false, 0, "Проблемы с электроэнергией, то есть то нет.\nЕсли нет подключения, напишите на почту support@mkr.org.ua и укажите ВУЗ. Возможно в университете остановлена служба на сервере."),
            University("17", "Національний університет цивільного захисту України (м. Харків)", "НУЦЗУ", "http://mrozklad.nuczu.edu.ua", "http://rozklad.nuczu.edu.ua", true, 0, "Университет отключен от приложения в связи с переходом на другую программу"),
            University("14", "Сумський нацiональний аграрний унiверситет", "СНАУ", "https://web.portal.snau.edu.ua", "http://portal.snau.edu.ua"),
            University("65", "Київський національний університет технологій та дизайну", "КНУТД", "https://osvita-api.knutd.edu.ua", "https://osvita.knutd.edu.ua"),
            University("35", "Уманський національний університет", "УНУС", "https://mkr-mobile.udau.edu.ua", "http://mkr.udau.edu.ua", true),
            University("22", "Національна академія внутрішніх справ (м.Київ)", "НАВС", "https://asuapi.naiau.kiev.ua", "http://asu.naiau.kiev.ua", true),
            University("61", "Харківський торговельно-економічний професійний коледж Державного торговельно-економічного університету", "ХТЕК ДТЕУ", "https://api.htek.org.ua", "https://htek.com.ua"),
            University("15", "Государственнй университет телекомуникаций", "ДУТ", "http://api.e-rozklad.dut.edu.ua", "http://e-rozklad.dut.edu.ua/"),
            University("66", "Білоцерківський національний аграрний університет", "БНАУ", "https://asu.btsau.edu.ua/api", "https://asu.btsau.edu.ua"),
            University("12", "Українська інженерно-педагогічна академія (м. Харків)", "УІПА", "http://uo-api.uipa.edu.ua", "http://uo.uipa.edu.ua"),
            University("38", "Полтавський юридичний інститут Національного юридичного університету імені Ярослава Мудрого", "ПЮІ НЮУ", "http://api.nlu.edu.ua", "https://acs.nlu.edu.ua", true, 2),
            University("44", "Киевский национальный университет строительства и архитектуры", "КНУСА", "http://mob.mkr.knuba.edu.ua", "http://mkr.knuba.edu.ua"),
            University("1", "Харьковский национальный университет городского хозяйства имени А.М. Бекетова", "ХНУГХ", "https://api.kname.edu.ua", "http://erp.kname.edu.ua"),
            University("19", "Государственный налоговый университет (г.Ирпень, Киевская обл.)", "ДПУ", "https://api.asu.dpu.edu.ua", "http://asu.nusta.com.ua"),
            University("43", "Національна академія Державної прикордонної служби України імені Б. Хмельницького", "НАДПСУ", "https://mkr.nadpsu.edu.ua", "http://asu.nadpsu.edu.ua:8009", true),
            University("60", "Донецкий государственный университет внутренних дел", "ДДУВС", "https://mkr-api.dnuvs.in.ua/"),
            University("69", "Центральноукраїнський державний університет імені Володимира Винниченка", "ЦДУ", "http://portal.cusu.edu.ua/api", "http://portal.cusu.edu.ua"),
            University("20", "Одеський національний економічний університет", "ОНЕУ", "http://qqqqqqasu.oneu.edu.ua:1080", "Https://asu.oneu.edu.ua", false, 0, "Университет отключен от приложения"),
            University("63", "Дніпровський державний університет внутрішніх справ", "ДДУВС Дніпро", "https://api.dduvs.edu.ua1", "http://mkr-portal.dduvs.in.ua", false, 0, "Приложение для ДДУВС отключено и работать не будет."),
            University("16", "Національний юридичний університет імені Ярослава Мудрого (м. Харкiв)", "НЮУ", "https://api.nlu.edu.ua", "https://acs.nlu.edu.ua", true),
            University("26", "Одеський національний морський університет", "ОНМУ", "https://api-mkp.onmu.org.ua", "https://mkp.onmu.org.ua"),
            University("18", "Вінницький національний аграрний університет", "ВНАУ", "http://api.vsau.org", "http://81.30.162.30"),
            University("45", "Коледж національного фармацевтичного університету (м. Харків)", "Коледж НФаУ", "http://smsapi.nuph.edu.ua", "https://portal.nuph.edu.ua", false, 1),
            University("51", "Львівський державний університет внутрішніх справ МВС України", "ЛДУВС", "https://dekanat.lvduvs.edu.ua/api", "https://dekanat.lvduvs.edu.ua"),
            University("32", "Університет ім. Альфреда Нобеля", "УА Нобеля", "https://studmob.duan.edu.ua", "https://stud.duan.edu.ua"),
            University("42", "Харківський національний автомобільно-дорожній університет", "ХНАДУ", "https://mvuz.khadi.kharkov.ua", "http://vuz.khadi.kharkov.ua"),
            University("24", "Тернопільський національний медичний університет ім. І. Я. Горбачевського", "ТНМУ", "https://asumob.tdmu.edu.ua", "http://asu.tdmu.edu.ua", true),
            University("55", "Запорізький національний університет", "ЗНУ", "https://api.znu.edu.ua/", "https://mkr.znu.edu.ua"),
            University("25", "Національний медичний університет імені О.О.Богомольця (м. Київ)", "НМУ", "http://api.nmuofficial.com1", "http://asu.nmuofficial.com", false, 0, "Приложение для вашего университета было отключено по решению руководства университета."),
            University("30", "Харківський національний медичний університет", "ХНМУ", "http://xnmu.mkr.org.ua:8081", "http://31.128.79.157:8080", true),
            University("29", "Одеський державний університет внутрішніх справ", "ОДУВС", "http://asu.oduvs.edu.ua:8085", "http://asu.oduvs.edu.ua", true),
            University("47", "Криворізький національний університет", "КНУ Кривий Ріг", "http://apiasu.knu.edu.ua", "http://asu.knu.edu.ua"),
            University("31", "Харківський національний університет внутрішніх справ", "ХНУВС", "https://api-rozklad.univd.edu.ua", "http://rozklad.univd.edu.ua"),
            University("57", "Центральноукраїнський національний технічний університет (м.Кропивницький)", "ЦНТУ", "https://rest.kntu.kr.ua", currentError = "Приложение для данного вуза отключено."),
            University("48", "Кременчуцький льотний Коледж ХНУВС", "КЛК ХНУВС", "https://mkpapi.klkmvs.org/", "https://mkp.klkmvs.org"),
            University("54", "Харківська державна академія дизайну та мистецтв", "ХДАДМ", "http://api.ksada.org:8090", "http://portal.ksada.org:8090", false, 0, "Сервер полностью отключен, когда будет восстановлен не известно."),
            University("67", "Національний університет «Запорізька політехніка»", "Запорізька політехніка", "https://api.zp.edu.ua", "https://portal.zp.edu.ua"),
            University("68", "Національна академія Служби Безпеки України", "НАСБУ", "https://api.asu.nasbu.edu.ua", "https://www.nasbu.edu.ua", true),
            University("74", "Фаховий коледж \"Альма-матер (ЦДУ ім. В. Винниченка)", "Коледж Альма-матер", "http://portal.cusu.edu.ua/api", "http://portal.cusu.edu.ua", false, 1)
        )
    }
    
    // Returns normalized Retrofit base URL guaranteed to end with a trailing slash
    fun getRetrofitBaseUrl(): String {
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }
    
    // Returns alternate URL permutations for fallback connectivity checks
    fun getAlternativeUrls(): List<String> {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val urls = mutableListOf<String>()
        
        // Original base URL
        urls.add(base)
        
        // With /api
        urls.add(base + "api/")
        
        // With /mkr
        urls.add(base + "mkr/")
        
        // With /mkr/api
        urls.add(base + "mkr/api/")
        
        // HTTPS and HTTP variants
        if (base.startsWith("http://")) {
            urls.add(base.replace("http://", "https://"))
            urls.add(base.replace("http://", "https://") + "api/")
        }
        
        return urls.distinct()
    }
}

