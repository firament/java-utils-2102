/**
 * Simple application to demonstrate email send capability on the server
 *      Using external configuration file to extend test options.
 *
 * Configuration file Should be encoded in US_ASCII format,
 *      and be present in Current Working Directory.
 *
 * * Uses gson library to read JSON formatted configuration data.
 * * Uses javax.mail library to send emails.
 *
 * --------------------------------------------------------------------------- *
 ** Dependencies: groupId:artifactId:version
 * com.sun.mail:javax.mail:1.6.2
 *   https://mvnrepository.com/artifact/com.google.code.gson/gson
 * com.google.code.gson:gson:2.8.6
 *   https://mvnrepository.com/artifact/com.sun.mail/javax.mail
 *
 * --------------------------------------------------------------------------- *
 ** Testing Notes:
 *
 * # Begin:
 * cd working-dir # (folder should contain this file and all dependency jars)
 *
 * # Compile:
 * javac -cp gson-2.8.6.jar:javax.mail-1.6.2.jar:activation-1.1.jar jmt2.java
 *
 * # Run test:
 * java -cp gson-2.8.6.jar:javax.mail-1.6.2.jar:activation-1.1.jar jmt2.java {Option}
 *
 * --------------------------------------------------------------------------- *
 ** Online compiler for JDK 1.8.0-66
 * https://www.jdoodle.com/online-java-compiler/
 *
 * --------------------------------------------------------------------------- *
 *
 */

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author sak
 */
public class jmt2
  {

    /**
     * Entry point for standalone tests
     *
     * @param args Takes the configuration set tag value as the first argument.
     *             Other arguments are ignores.
     */
    public static void main(String[] args)
      {
        if (args.length == 0) {
            throw new IllegalArgumentException("Required option flag not provided. Aborting.");
        }
        jmt2 javamailtester = new jmt2();
        javamailtester.doMailTest(args[0]);
      }

    public void jmt2()
      {
      }

    /**
     * Run the mail test using the given configuration set. Orchestrates the
     * steps to complete the test.
     *
     * @param vtag The parameter set to use.
     */
    public void doMailTest(String vtag)
      {
        System.out.println("BEGIN - doMailTest()");

        String CurrTS = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z").format(new Date());
        String lsConfigFilePath = jmt2.class.getSimpleName() + "-config.json";
        HashMap<String, emailEnvelope> loConfig = new HashMap<String, emailEnvelope>();
        emailEnvelope loCurrConfig = null;

        System.out.println("Option Tag  => " + vtag);
        System.out.println("TimeStamp   => " + CurrTS);
        System.out.println("Working Dir => " + Paths.get("").toAbsolutePath().toString());
        System.out.println("Config File => " + lsConfigFilePath);

        // Get config data
        loConfig = getConfigData(lsConfigFilePath);
        if (loConfig == null) {
            System.out.println("Unusable configuration. Aborting now.");
            return;
        }

        // Check option exists
        if (!loConfig.containsKey(vtag)) {
            System.out.println("Requested configuration key not found.");
            System.out.println("Keys are case sensitive, check and try agaain with proper key.");
            System.out.println("Aborting NOW.");
            return;
        }
        loCurrConfig = loConfig.get(vtag);

        // Send email
        try {
            sendMail(CurrTS, loCurrConfig);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("Done Mail test, with Timestamp " + CurrTS);
        System.out.println("FINIS - doMailTest()");
      }

    /**
     * Read and De-Serialize configuration sets from the Json formatted
     * configuration file.
     * <p>
     * File encoding should be US_ASCII
     *
     * @param ConfigFilePath Name of the configuration file The file is expected
     *                       to be present in the current working dir.
     * @return Configuration Map de-serialized from the given configuration
     *         file.
     */
    public HashMap<String, emailEnvelope> getConfigData(String ConfigFilePath)
      {
        System.out.println("BEGIN - getConfigData()");

        // Get data from file
        String lsData = null;
        try {
            // Path loFile = Path.of(ConfigFilePath); // Java 11 and later, fails on 1.8
            Path loFile = Paths.get(ConfigFilePath);  // Java 1.8 usage
            System.out.println("Reading from File: " + loFile.toAbsolutePath().toString());
            byte[] labData = Files.readAllBytes(loFile);
            System.out.println("OK. Read from file.");
            lsData = new String(labData, StandardCharsets.US_ASCII);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        if (lsData == null || lsData.trim().length() == 0) {
            System.out.println("Enpty configuration file. Cannot be used");
            throw new UnsupportedOperationException("Enpty configuration file. Cannot be used");
        }

        // Deserialize to map
        Gson gson = new Gson();
        Type loCfgType = new TypeToken<HashMap<String, emailEnvelope>>()
          {
          }.getType();
        HashMap<String, emailEnvelope> loDat = null;
        try {
            loDat = gson.fromJson(lsData, loCfgType);
            System.out.println("OK. Deserialize object");
            System.out.println("Deserialized keys : " + loDat.keySet().toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("FINIS - getConfigData()");
        return loDat;

      }

    /**
     * Prepare and send the mail using the given configuration set.
     *
     * @param CurrTS     Current timestamp, to distinguish between several test
     *                   messages sent in quick succession.
     * @param MailConfig Configuration set to be used.
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    public void sendMail(String CurrTS, emailEnvelope MailConfig) throws UnsupportedEncodingException, MessagingException
      {
        System.out.println("BEGIN - sendMail()");

        //<editor-fold defaultstate="collapsed" desc="Initialize">
        // Mail Content
        String Subj = MessageFormat.format("Mail from app server, java, v1.0, {0}", CurrTS);
        String BodyHTML = MessageFormat.format("<h1>Success!!</h1><hr /><p>Mail generated at: {0}</p><p>This mail was generated by the test program written in Java to demonstrate email sending capability from the server.</p><hr />", CurrTS);
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Prepare">
        // Mail Envelope
        Session loSession = Session.getDefaultInstance(MailConfig.SmtpProps, new Authenticator()
          {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
              {
                return new PasswordAuthentication(MailConfig.User, MailConfig.Pwd);
              }
          });
        MimeMessage loMesg = new MimeMessage(loSession);

        loMesg.setSubject(Subj);
        loMesg.setContent(BodyHTML, "text/html;charset=UTF-8");
        loMesg.setFrom(new InternetAddress(MailConfig.User, MailConfig.Name));
        loMesg.setRecipients(
          Message.RecipientType.TO,
          MailConfig.ToList.toArray(new InternetAddress[MailConfig.ToList.size()])
        );

        loMesg.saveChanges();
        System.out.println("OK. Mail prepeartion.");
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Send">
        try (
           com.sun.mail.smtp.SMTPTransport SmtpSender = (com.sun.mail.smtp.SMTPTransport) loSession.getTransport()) {

            // Connect
            System.out.println("Attempting SMTP connection now...");
            SmtpSender.connect();
            System.out.print(SmtpSender.getLastServerResponse());
            if (SmtpSender.isConnected()) {
                System.out.println("OK. SMTP connection.");
            } else {
                System.out.println("FAIL. Connection attempt failed, error thrown.");
                return;
            }
            System.out.println("LocalHost = " + SmtpSender.getLocalHost());

            // Send
            System.out.println("Attempting send mail now...");
            SmtpSender.sendMessage(loMesg, loMesg.getAllRecipients());
            System.out.print(SmtpSender.getLastServerResponse());
            if (SmtpSender.getLastReturnCode() == 250) {
                if (MailConfig.User.endsWith("@ethereal.email")) {
                    System.out.println("");
                    System.out.println(
                      "Link to view test email:  https://ethereal.email/message/"
                      + SmtpSender.getLastServerResponse().substring(31).replace(']', '/')
                    );
                }
                System.out.println("OK. Mail send.");
            } else {
                System.out.println("Unexpected response recieved. Analyze...");
            }

            // Windup
            System.out.println("Closing SMTP connection now...");
            SmtpSender.close();
            System.out.print(SmtpSender.getLastServerResponse());
        }

        System.out.println("FINIS - sendMail()");
      }

    // *************************************************************************
    //<editor-fold defaultstate="collapsed" desc="Data Structures">
    /**
     * Container for configuration data.
     */
    private class emailEnvelope
      {

        public String Tag = "_";
        public String Note = "-na-";
        public String Name = "_User_Name";
        public String User = "user_ID";
        public String Pwd = "User_Password_Plaintext";
        public Properties SmtpProps = new Properties();
        public Set<InternetAddress> ToList = new HashSet<InternetAddress>();

        public emailEnvelope()
          {
            System.out.println("Creating emailEnvelope");
          }

        /* Convenience Methods, to maintain reference intgerity */
        public void AddProp(String key, String val)
          {
            SmtpProps.put(key, val);
          }

        public void AddRecepients(InternetAddress[] tolist)
          {
            ToList.addAll(Arrays.asList(tolist));
          }

        public void AddRecepients(InternetAddress recep)
          {
            ToList.add(recep);
          }
      }
    //</editor-fold>
  }
