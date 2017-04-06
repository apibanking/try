import javax.xml.ws.Holder;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext; 
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.math.BigInteger;

import java.util.ArrayList;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.KeyManagementException;
import java.security.UnrecoverableKeyException;

import javax.xml.soap.SOAPException;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.soap.SOAPFault;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;

import javax.xml.datatype.XMLGregorianCalendar;

import com.quantiguous.services.FundsTransferByCustomerService2;
import com.quantiguous.services.FundsTransferByCustomerService2HttpService;

import com.quantiguous.services.CurrencyCodeType;

import org.kohsuke.args4j.*;

enum Env { UAT,PRD };

class ApiBankingFault
{
   String faultCode;
   String faultSubCode;
   String faultReason;
}

@SuppressWarnings("unchecked")
public class Try {


   @Option(name="-?", usage="display this page")
   private boolean showUsage = false;
   
   @Option(name="-env", required=true, usage="the environment connect to")
   private Env env;
   
   @Option(name="-client_id", required=true, usage="the client key from developer portal")
   private String client_id;
   
   @Option(name="-client_secret", required=true, usage="the client secret from developer portal")
   private String client_secret;
   
   @Option(name="-username", required=true, usage="the username issued to you by the bank")
   private String ldap_user;
   
   @Option(name="-password", required=true, usage="the password issued to you by the bank")
   private String ldap_password;
   
   @Option(name="-keystore", required=true, usage="the full path of the keystore (jks) that has your private key")
   private String keystore;
   
   @Option(name="-keystore_pass", required=true, usage="the password for the keystore")
   private String keystore_pass;
   
   @Option(name="-appID", required=true, usage="the appID issued to you by the bank")
   private String appID;
   
   @Option(name="-customerID", required=true, usage="the customer ID issued to you by the bank")
   private String customerID;
   
   @Option(name="-accountNo", required=true, usage="the account No issued to you by the bank")
   private String accountNo;
   
   @Option(name="-disableClientAuth", usage="to disable client auth (2-way ssl)")
   private boolean disableClientAuth = false;

   public static void main(String[] argv) throws NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, IOException, KeyStoreException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
     new Try().doMain(argv);
  }
   public void doMain(String[] argv) throws NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, IOException, KeyStoreException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
      CmdLineParser parser = new CmdLineParser(this);

      try {
          // parse the arguments.
          parser.parseArgument(argv);
          if( showUsage ) {
             parser.printUsage(System.err);
             System.err.println();
             return;
          }

      } catch( Exception e ) {
          parser.printUsage(System.err);
          System.err.println();
          return;
      }

      enableTrace();
      setClientCertificate(keystore, keystore_pass);

      Path currentRelativePath = Paths.get("");
      String wsdlFilePath = currentRelativePath.toAbsolutePath().toString() + "/fundsTransferByCustomerService2.wsdl";

      FundsTransferByCustomerService2HttpService svc =  new FundsTransferByCustomerService2HttpService(new URL("file://" + wsdlFilePath));
      FundsTransferByCustomerService2 client = svc.getFundsTransferByCustomerService2HttpPort();

      Holder<String> version                          = new Holder<String>();
      Holder<Boolean> lowBalanceAlert                 = new Holder<Boolean>();
      Holder<CurrencyCodeType> accountCurrencyCode    = new Holder<CurrencyCodeType>();
      Holder<Float>  accountBalanceAmount             = new Holder<Float>();

      version.value = "1.0";

      String baseURL = "";
      if ( env == Env.UAT && disableClientAuth ) baseURL = "https://uatsky.yesbank.in/app/uat/fundsTransferByCustomerService2";
      if ( env == Env.UAT && !disableClientAuth ) baseURL = "https://uatsky.yesbank.in:444/app/uat/ssl/fundsTransferByCustomerSevice2";
      if ( env == Env.PRD && disableClientAuth ) baseURL = "https://sky.yesbank.in/app/live/fundsTransferByCustomerService2";
      if ( env == Env.PRD && !disableClientAuth ) baseURL = "https://sky.yesbank.in:444/app/live/fundsTransferByCustomerService2";
         
      ((BindingProvider)client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseURL);

      // set the user & password
      ((BindingProvider)client).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, ldap_user);
      ((BindingProvider)client).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, ldap_password);

      // set the headers
      Map<String, List<String>> headers = new HashMap<String, List<String>>();
      headers.put("X-IBM-Client-Id", Collections.singletonList(client_id));
      headers.put("X-IBM-Client-Secret", Collections.singletonList(client_secret));
      ((BindingProvider)client).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, headers);

      // send the request
      try {
         client.getBalance(version.value, appID, customerID, accountNo, version, accountCurrencyCode, accountBalanceAmount, lowBalanceAlert);
      }
      catch(SOAPFaultException e) {
        printFault(e.getFault());
      }
      catch(Exception e) {
        e.printStackTrace(System.out); 
      };
   }

   private String parseQName(QName val) {
      if ( val != null ) {
         if ( val.getNamespaceURI() == "http://www.quantiguous.com/services" ) {
            return "ns:" + val.getLocalPart();
         }
         return val.toString();
      }
      return null;
   }

   private ApiBankingFault parseFault(SOAPFault f) {
      boolean first = false;
      ApiBankingFault apiFault = new ApiBankingFault();

      for (Iterator<QName> subCodesIterator = (Iterator<QName>)f.getFaultSubcodes(); subCodesIterator.hasNext();) {
         if (first == false) { 
            apiFault.faultCode = parseQName(subCodesIterator.next());
            first = true;
         } else {
           apiFault.faultSubCode = parseQName(subCodesIterator.next());
         }
      }
      try {
         for (Iterator<String> reasonTextsIterator = (Iterator<String>)f.getFaultReasonTexts(); reasonTextsIterator.hasNext();) {
            apiFault.faultReason = reasonTextsIterator.next();
         }
      } catch (SOAPException x) {
         x.printStackTrace(System.out); 
      }
      if ( f.hasDetail() ) {
         for (Iterator<DetailEntry> detailEntriesIterator = (Iterator<DetailEntry>)f.getDetail().getDetailEntries(); detailEntriesIterator.hasNext();) {
            System.out.println(detailEntriesIterator.next());
         }
      }
 
      return apiFault;
   }

   private void printFault(SOAPFault f) {
      ApiBankingFault apiFault = parseFault(f);

      System.out.println(apiFault.faultCode);
      System.out.println(apiFault.faultSubCode);
      System.out.println(apiFault.faultReason);
   }

   private void enableTrace() {
     System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
     System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
     System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
     System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
   }

   private void setClientCertificate(String keystore, String keystore_pass) {
     if (keystore != null) { System.setProperty("javax.net.ssl.keyStore", keystore); }
     if (keystore_pass != null) { System.setProperty("javax.net.ssl.keyStorePassword", keystore_pass); }
   }


   /* the following method doesnt work with the oracle jdk, it may work when this runs within an app-server, 
      the idea is to find out where is JAXWSProperties */
   private void setSocketFactory(BindingProvider client) 
      throws NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, IOException, KeyStoreException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
      SSLContext sc = SSLContext.getInstance("TLS");
      KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(new FileInputStream("qg-client.jks"), "apibanking".toCharArray());
      factory.init(keyStore, "apibanking".toCharArray());
      sc.init(factory.getKeyManagers(), null, null);

      // specify a SSLSocket factory that will deal wit hthe 2 way SSL
      ((BindingProvider)client).getRequestContext().put("com.sun.xml.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sc);
      ((BindingProvider)client).getRequestContext().put("com.sun.xml.internal.ws.developer.JAXWSProperties.SSL_SOCKET_FACTORY", sc);
   }
}
