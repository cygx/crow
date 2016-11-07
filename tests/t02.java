import de.cygx.crow.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.*;
import javax.net.ssl.*;

public class t02 {
    static final String[] CIPHERS = {
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
    };

    static final X509Certificate[] EMPTY_CERT_LIST = new X509Certificate[0];

    static final String[] PROTOCOLS = { "TLSv1.2" };

    static final PublicKey PUBLIC_CLIENT_KEY_EC;
    static final PrivateKey PRIVATE_CLIENT_KEY_EC;

    static final PublicKey PUBLIC_SERVER_KEY_EC;
    static final PrivateKey PRIVATE_SERVER_KEY_EC;

    static {
        KeyPairGenerator pg;
        try { pg = KeyPairGenerator.getInstance("EC"); }
        catch(NoSuchAlgorithmException e) { throw new RuntimeException(e); }
        pg.initialize(256);

        KeyPair cp = pg.genKeyPair();
        KeyPair sp = pg.genKeyPair();

        PUBLIC_CLIENT_KEY_EC = cp.getPublic();
        PRIVATE_CLIENT_KEY_EC = cp.getPrivate();

        PUBLIC_SERVER_KEY_EC = sp.getPublic();
        PRIVATE_SERVER_KEY_EC = sp.getPrivate();
    }

    static final PublicKey PUBLIC_CLIENT_KEY_RSA;
    static final PrivateKey PRIVATE_CLIENT_KEY_RSA;

    static final PublicKey PUBLIC_SERVER_KEY_RSA;
    static final PrivateKey PRIVATE_SERVER_KEY_RSA;

    static {
        KeyPairGenerator pg;
        try { pg = KeyPairGenerator.getInstance("RSA"); }
        catch(NoSuchAlgorithmException e) { throw new RuntimeException(e); }
        pg.initialize(1024);

        KeyPair cp = pg.genKeyPair();
        KeyPair sp = pg.genKeyPair();

        PUBLIC_CLIENT_KEY_RSA = cp.getPublic();
        PRIVATE_CLIENT_KEY_RSA = cp.getPrivate();

        PUBLIC_SERVER_KEY_RSA = sp.getPublic();
        PRIVATE_SERVER_KEY_RSA = sp.getPrivate();
    }

    static final String[] SERVER_ALIASES_EC = { "ec-server-alias" };
    static final String[] SERVER_ALIASES_RSA = { "rsa-server-alias" };
    static final String[] CLIENT_ALIASES_EC = { "ec-client-alias" };
    static final String[] CLIENT_ALIASES_RSA = { "rsa-client-alias" };

    static final X509KeyManager[] KEY_MANAGERS = { new X509KeyManager() {
        public String chooseClientAlias(String[] keyType, Principal[] issuers,
                Socket socket) {
            for(String kt : keyType) switch(kt) {
                case "EC_EC": return CLIENT_ALIASES_EC[0];
                case "RSA": return CLIENT_ALIASES_RSA[0];
            }
            return null;
        }

        public String chooseServerAlias(String keyType, Principal[] issuers,
                Socket socket) {
            switch(keyType) {
                case "EC_EC": return SERVER_ALIASES_EC[0];
                case "RSA": return SERVER_ALIASES_RSA[0];
                default: return null;
            }
        }

        public X509Certificate[] getCertificateChain(String alias) {
            return EMPTY_CERT_LIST;
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {
            switch(keyType) {
                case "EC_EC": return CLIENT_ALIASES_EC;
                case "RSA": return CLIENT_ALIASES_RSA;
                default: return null;
            }
        }

        public PrivateKey getPrivateKey(String alias) {
            switch(alias) {
                case "ec-client-alias": return PRIVATE_CLIENT_KEY_EC;
                case "rsa-client-alias": return PRIVATE_CLIENT_KEY_RSA;
                case "ec-server-alias": return PRIVATE_SERVER_KEY_EC;
                case "rsa-server-alias": return PRIVATE_SERVER_KEY_RSA;
                default: return null;
            }
        }

        public String[] getServerAliases(String keyType, Principal[] issuers) {
            switch(keyType) {
                case "EC_EC": return SERVER_ALIASES_EC;
                case "RSA": return SERVER_ALIASES_RSA;
                default: return null;
            }
        }
    } };

    static final X509TrustManager[] TRUST_MANAGER = { new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain,
                String authType) {}

        public void checkServerTrusted(X509Certificate[] chain,
                String authType) {}

        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_CERT_LIST;
        }
    } };

    public static void main(String[] args) throws Exception {
        //System.setProperty("javax.net.debug", "ssl");
        /*{
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, PASSWORD.toCharArray());

            FileOutputStream os = new FileOutputStream(KEYSTORE);
            store.store(os, PASSWORD.toCharArray());
            os.close();

            System.setProperty("javax.net.ssl.keyStore", KEYSTORE);
            System.setProperty("javax.net.ssl.keyStorePassword", PASSWORD);
        }*/

        SSLContext context;
        SocketAddress address;

        {
            context = SSLContext.getInstance("TLSv1.2");
            context.init(KEY_MANAGERS, TRUST_MANAGER, null);
        }

        {
            ServerSocketFactory factory = context.getServerSocketFactory();
            SSLServerSocket server =
                (SSLServerSocket)factory.createServerSocket();
            server.setEnabledCipherSuites(CIPHERS);
            server.setEnabledProtocols(PROTOCOLS);
            server.bind(new InetSocketAddress(
                InetAddress.getLoopbackAddress(), 0));
            address = server.getLocalSocketAddress();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Socket socket = server.accept();
                        System.out.println(new BufferedReader(
                            new InputStreamReader(
                                socket.getInputStream())).readLine());
                        socket.close();
                    }
                    catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        {
            SocketFactory factory = context.getSocketFactory();
            SSLSocket socket = (SSLSocket)factory.createSocket();
            socket.setEnabledCipherSuites(CIPHERS);
            socket.setEnabledProtocols(PROTOCOLS);
            socket.connect(address);
            socket.getOutputStream().write("hello world\n".getBytes());
            socket.close();
        }
    }
}
