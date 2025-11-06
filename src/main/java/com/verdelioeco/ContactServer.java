package com.verdelioeco;

import static spark.Spark.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import io.github.cdimascio.dotenv.Dotenv;

public class ContactServer {

    public static void main(String[] args) {

        // Cargar variables de entorno (.env)
        Dotenv dotenv = Dotenv.load();
        String emailUser = dotenv.get("EMAIL_USER");
        String emailPass = dotenv.get("EMAIL_PASS");

        // ✅ Configurar el puerto ANTES de definir las rutas
        port(4567);
        System.out.println("Servidor corriendo en http://localhost:4567");

        // Permitir CORS
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
        });

        options("/*", (req, res) -> "OK");

        // Ruta principal para enviar el correo
        post("/send", (req, res) -> {
            String name = req.queryParams("name");
            String email = req.queryParams("email");
            String message = req.queryParams("message");

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
                mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailUser));
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
}
