package com.verdelioeco;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import io.github.cdimascio.dotenv.Dotenv;
import static spark.Spark.before;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;

public class ContactServer {

    public static void main(String[] args) {

        // Cargar variables de entorno (.env) o del entorno Render
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.load();
        } catch (Exception e) {
            System.out.println("⚠️ No se encontró archivo .env (usando variables de entorno del servidor)");
        }

        String emailUser = getenvOrDotenv(dotenv, "EMAIL_USER");
        String emailPass = getenvOrDotenv(dotenv, "EMAIL_PASS");
        String recipientEmail = getenvOrDotenv(dotenv, "RECIPIENT_EMAIL", emailUser);

        // ✅ Configurar puerto dinámico (Render lo asigna automáticamente)
        port(Integer.parseInt(System.getenv().getOrDefault("PORT", "4567")));
        System.out.println("Servidor corriendo en puerto " + System.getenv().getOrDefault("PORT", "4567"));

        // CORS
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
        });

        options("/*", (req, res) -> "OK");

        // Endpoint de envío
        post("/send", (req, res) -> {
            String name = req.queryParams("name");
            String email = req.queryParams("email");
            String message = req.queryParams("message");

            if (name == null || email == null || message == null) {
                res.status(400);
                return "❌ Faltan campos obligatorios.";
            }

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.zoho.eu");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUser, emailPass);
                }
            });

            try {
                Message mimeMessage = new MimeMessage(session);
                mimeMessage.setFrom(new InternetAddress(emailUser));
                mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                mimeMessage.setSubject("Nuevo mensaje de contacto de " + name);
                mimeMessage.setText("De: " + name + "\nEmail: " + email + "\n\nMensaje:\n" + message);

                Transport.send(mimeMessage);
                res.status(200);
                return "✅ Mensaje enviado correctamente.";
            } catch (MessagingException e) {
                e.printStackTrace();
                res.status(500);
                return "❌ Error al enviar el mensaje: " + e.getMessage();
            }
        });
    }

    // Método auxiliar para obtener variables
    private static String getenvOrDotenv(Dotenv dotenv, String key) {
        return getenvOrDotenv(dotenv, key, null);
    }

    private static String getenvOrDotenv(Dotenv dotenv, String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null && dotenv != null) {
            value = dotenv.get(key);
        }
        return value != null ? value : defaultValue;
    }
}
