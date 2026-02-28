package eu.auctionplatform.notification.infrastructure.email

import io.quarkus.mailer.Mail
import io.quarkus.mailer.Mailer
import io.quarkus.qute.Engine
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Sends transactional emails using the Quarkus Mailer (SMTP) with
 * Qute-rendered HTML templates.
 *
 * Templates are resolved from `resources/templates/{locale}/{templateName}.html`.
 * If the requested locale is not available, the sender falls back to "en".
 *
 * Thread-safe and managed as an application-scoped CDI bean.
 */
@ApplicationScoped
class EmailSender @Inject constructor(
    private val mailer: Mailer,
    private val quteEngine: Engine
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(EmailSender::class.java)
    }

    /**
     * Renders a Qute template and sends the resulting HTML as an email.
     *
     * @param to           Recipient email address.
     * @param subject      Email subject line.
     * @param templateName Template file name without extension (e.g. "overbid").
     * @param data         Key-value pairs to inject into the template.
     * @param locale       BCP-47 locale for template selection (default "en").
     * @throws RuntimeException if the template cannot be found or rendered.
     */
    fun sendEmail(
        to: String,
        subject: String,
        templateName: String,
        data: Map<String, Any>,
        locale: String = "en"
    ) {
        val htmlBody = renderTemplate(templateName, data, locale)

        val mail = Mail.withHtml(to, subject, htmlBody)

        try {
            mailer.send(mail)
            LOG.infof(
                "Email sent: to=%s, subject='%s', template=%s/%s",
                to, subject, locale, templateName
            )
        } catch (ex: Exception) {
            LOG.errorf(
                ex, "Failed to send email: to=%s, subject='%s', template=%s/%s: %s",
                to, subject, locale, templateName, ex.message
            )
            throw ex
        }
    }

    /**
     * Renders a Qute template with the given data and locale.
     *
     * Tries `{locale}/{templateName}` first, then falls back to `en/{templateName}`.
     *
     * @param templateName Template file name without extension.
     * @param data         Template data map.
     * @param locale       The preferred locale.
     * @return Rendered HTML string.
     */
    private fun renderTemplate(
        templateName: String,
        data: Map<String, Any>,
        locale: String
    ): String {
        // Try locale-specific template first
        val templatePath = "$locale/$templateName"
        var template = quteEngine.getTemplate(templatePath)

        // Fallback to English if locale-specific template is not found
        if (template == null && locale != "en") {
            LOG.debugf(
                "Template '%s' not found, falling back to 'en/%s'",
                templatePath, templateName
            )
            template = quteEngine.getTemplate("en/$templateName")
        }

        if (template == null) {
            throw IllegalArgumentException(
                "Email template '$templateName' not found for locale '$locale' or 'en'"
            )
        }

        // Inject all data entries into the template
        var instance = template.instance()
        for ((key, value) in data) {
            instance = instance.data(key, value)
        }

        return instance.render()
    }
}
