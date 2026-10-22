package com.nemouw.mail;

import com.nemouw.enums.LocaleCode;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renders an email (subject, HTML and plain text) for a type and locale. Owns its own Thymeleaf
 * engine and message bundle so it is independent of any MVC view configuration.
 */
@Component
public class EmailTemplates {
    private static final String BRAND = "Nemouw";

    public record Rendered(String subject, String html, String text) {
    }

    private final SpringTemplateEngine engine;
    private final MessageSource messages;

    public EmailTemplates() {
        var bundle = new ResourceBundleMessageSource();
        bundle.setBasename("mail/properties/messages");
        bundle.setDefaultEncoding("UTF-8");
        bundle.setFallbackToSystemLocale(false);   // missing locale -> messages.properties, not the server's locale
        bundle.setAlwaysUseMessageFormat(true);    // one rule everywhere: write '' for a literal apostrophe
        this.messages = bundle;

        this.engine = new SpringTemplateEngine();
        this.engine.addTemplateResolver(resolver(TemplateMode.HTML, "*.html", 1));
        this.engine.addTemplateResolver(resolver(TemplateMode.TEXT, "*.txt", 2));
        this.engine.setTemplateEngineMessageSource(bundle);
    }

    private static ClassLoaderTemplateResolver resolver(TemplateMode mode, String pattern, int order) {
        var r = new ClassLoaderTemplateResolver();
        r.setPrefix("mail/");
        r.setTemplateMode(mode);
        r.setCharacterEncoding("UTF-8");
        r.setResolvablePatterns(Set.of(pattern));
        r.setOrder(order);
        r.setCacheable(true);
        return r;
    }

    public Rendered render(EmailType type, LocaleCode locale, Map<String, Object> vars) {
        Locale loc = Locale.forLanguageTag(locale.code());
        String subject = messages.getMessage(type.subjectKey(), null, loc);

        var ctx = new Context(loc);
        ctx.setVariables(vars);
        ctx.setVariable("subject", subject);
        ctx.setVariable("preheader", messages.getMessage(type.preheaderKey(), null, loc));
        ctx.setVariable("brand", BRAND);
        ctx.setVariable("lang", locale.code());
        ctx.setVariable("dir", locale == LocaleCode.AR ? "rtl" : "ltr");
        ctx.setVariable("year", Year.now().getValue());

        return new Rendered(subject,
                engine.process(type.htmlTemplate(), ctx),
                engine.process(type.textTemplate(), ctx));
    }
}