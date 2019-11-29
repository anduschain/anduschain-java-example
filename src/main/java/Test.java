import io.anduschain.javasdk.*;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.*;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.ExecutionException;

public class Test {

    static Web3j web3j;
    static Admin adminWeb3j;

    static BigInteger sNonce;

    /****************************************************************
     * dockerNodeAddress : input your docker node address
     * dockerNodePrivateKey : input your docker node private key
     * ganache : If you use ganache, input ganache address.
     ****************************************************************/

    //When you make a transaction, use general transaction type only. (BigInteger.ZERO)
    static final BigInteger TRANSACTION_TYPE_GENERAL = BigInteger.ZERO;
    static final String WSNODE_ADDRESS = "http://localhost:8548";
    // for anduschain-docker-control (use wsnode)
    static String dockerNodeAddress = "0xd28B5411Cc96507ed17b757791f7De38eb1aA4B6";
    static String dockerNodePrivateKey = "629cf00f5435f3ddf020b56222c9fa35f52605886f01b3e994f180f1105f3133";

    // for ganache
    final String ganache = "address";

    //credentials : You can use your key-store or use it.
    final String keyStorePath = new File("wallet/UTC--2019-10-17T01-50-41.737000000Z--a2ae980a5b4e1cd07b36803e4d978dbd65da3f34.json").getAbsolutePath();
    final String passWord = "1111";
    final String myAddress = "0xa2ae980a5b4e1cd07b36803e4d978dbd65da3f34";

    public static void main(String[] args) {
        //you can connect anduschain-docker-control node running on localhost
        web3j = Web3j.build(new HttpService(WSNODE_ADDRESS));
        adminWeb3j = Admin.build(new HttpService(WSNODE_ADDRESS));
//        Web3j web3j = Web3j.build(new HttpService("HTTP://127.0.0.1:7545"));  //for ganache

        System.out.println("start Test");
        Test tt = new Test();
        try {
            tt.getClientVersion();
            tt.getBlockNumber();
//            tt.make_wallet("path/to/file");  //make wallet need to define path
            System.out.println("------------------------------------------------------");
            tt.checkBalance();
            System.out.println("------------------------------------------------------");
            tt.receiveCoin();
            System.out.println("------------------------------------------------------");
            tt.sendCoin();
            System.out.println("------------------------------------------------------");
            tt.checkBalance();
            System.out.println("------------------------------------------------------");
            tt.sendContract();
            System.out.println("------------------------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("end Test");
        }
    }

    private void getClientVersion() throws IOException {
        Web3ClientVersion clientVersion = web3j.web3ClientVersion().send();
        System.out.println("Client version: " + clientVersion.getWeb3ClientVersion());
    }

    private void getBlockNumber() throws IOException {
        EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
        System.out.println("current Block number: " + blockNumber.getBlockNumber());
    }

    private void make_wallet(String path) throws CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        String walletFileName = WalletUtils.generateFullNewWalletFile(passWord, new File(path));
        String[] fetchAddress = walletFileName.split("--");
        String getAddress = fetchAddress[fetchAddress.length - 1].split("\\.")[0];
        System.out.println("Address : " + getAddress);
    }

    private void checkBalance() throws IOException {
        System.out.println("testCheckBlanace...");
        EthGetBalance myBalance = web3j.ethGetBalance(myAddress, DefaultBlockParameter.valueOf("latest")).send();
        EthGetBalance nodeBalance = web3j.ethGetBalance(dockerNodeAddress, DefaultBlockParameter.valueOf("latest")).send();
        System.out.println("my balance : " + Convert.fromWei(myBalance.getBalance().toString(), Convert.Unit.ETHER));
        System.out.println("docker node balance : " + Convert.fromWei(nodeBalance.getBalance().toString(), Convert.Unit.ETHER));
    }

    private void receiveCoin() throws Exception {
        System.out.println("testReceiveCoin...");
        EthSendTransaction ethCall = null;

        EthGetTransactionCount ethGetTransactionCount = adminWeb3j.ethGetTransactionCount(
                dockerNodeAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        sNonce = nonce;
        System.out.println("nonce : " + sNonce);
        // create transaction with coinbase.
        PersonalUnlockAccount personalUnlockAccount = adminWeb3j.personalUnlockAccount(dockerNodeAddress,"11111").send();
        if (personalUnlockAccount.hasError()) {
            System.out.println("Can not unlock coinbase : " + personalUnlockAccount.getError().getMessage());
        } else {
            if (personalUnlockAccount.accountUnlocked()) {
                for (int i = 0; i < 1; i++) {
                    Transaction tm = Transaction.createEtherTransaction(
                            dockerNodeAddress,
                            sNonce,
                            new BigInteger("23809523805524"), // Gas price must greater than 23809523805524
                            new BigInteger("21000"),
                            myAddress,
                            Convert.toWei("5", Convert.Unit.ETHER).toBigInteger()
                    );
                    ethCall = adminWeb3j.ethSendTransaction(tm).send();
                    if (ethCall.hasError()) {
                        System.out.println(ethCall.getError().getMessage());
                    } else {
                        System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
//                    waitTransactionReceipt(ethCall.getTransactionHash());
                    }
                    sNonce = sNonce.add(BigInteger.ONE);
                    System.out.println("nonce : " + sNonce);
                }
            }
        }

        // create raw transaction
       /*
       AnduschainRawTransaction rtm = AnduschainRawTransaction.createEtherTransaction(
                TRANSACTION_TYPE_GENERAL,
                getNonce(dockerNodeAddress),
                new BigInteger("23809523805524"), // Gas price must greater than 23809523805524
                new BigInteger("21000"),
                myAddress,
                Convert.toWei("5", Convert.Unit.ETHER).toBigInteger()
        );

        //get senders address using private key
        Credentials credentials = Credentials.create(dockerNodePrivateKey);
        dockerNodeAddress = credentials.getAddress();
        ethCall = web3j.ethSendRawTransaction(getHexFromSignedMessage(rtm, credentials)).sendAsync().get();
        if (ethCall.hasError()) {
            System.out.println(ethCall.getError().getMessage());
        } else {
            System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
            waitTransactionReceipt(ethCall.getTransactionHash());
        }
        */
    }

    private void sendCoin() throws IOException, CipherException, ExecutionException, InterruptedException {
        System.out.println("testTransfer...");
        EthSendTransaction ethCall = null;

        sNonce = getNonce(myAddress);
        for ( int i = 0; i < 1; i++) {
            AnduschainRawTransaction rtm = AnduschainRawTransaction.createEtherTransaction(
                    TRANSACTION_TYPE_GENERAL,
                    sNonce,
                    new BigInteger("23809523805524"),
                    new BigInteger("21000"),
                    dockerNodeAddress,
                    Convert.toWei("1", Convert.Unit.ETHER).toBigInteger()
            );

            Credentials credentials = WalletUtils.loadCredentials(
                    passWord,
                    keyStorePath);

            ethCall = web3j.ethSendRawTransaction(getHexFromSignedMessage(rtm, credentials)).sendAsync().get();

            if (ethCall.hasError()) {
                System.out.println(ethCall.getError().getMessage());
            } else {
                System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
//            waitTransactionReceipt(ethCall.getTransactionHash());
            }
            sNonce = sNonce.add(BigInteger.ONE);
            System.out.println("nonce : " + sNonce);
        }

    }

    private void sendContract() throws Exception {
        System.out.println("testContract...");
        EthSendTransaction ethCall = null;

        Credentials credentials = WalletUtils.loadCredentials(
                passWord,
                keyStorePath);

        sNonce = getNonce(myAddress);
        for ( int i = 0; i < 1; i++) {
            AnduschainRawTransaction artm = AnduschainRawTransaction.createContractTransaction(
                    TRANSACTION_TYPE_GENERAL,
                    sNonce,
                    new BigInteger("23809523805524"),
                    new BigInteger("2100000"),
                    BigInteger.ZERO,
                    getSimpleStorageBinary()
            );

            ethCall = web3j.ethSendRawTransaction(getHexFromSignedMessage(artm, credentials)).sendAsync().get();

            if (ethCall.hasError()) {
                System.out.println(ethCall.getError().getMessage());
            } else {
                System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
//            waitTransactionReceipt(ethCall.getTransactionHash());
            }
            sNonce = sNonce.add(BigInteger.ONE);
            System.out.println("nonce : " + sNonce);
        }

    }

    private String getHexFromSignedMessage(AnduschainRawTransaction rtm, Credentials cre) {
        byte[] signedMessage = AnduschainTransactionEncoder.signMessage(rtm, cre);
        return Numeric.toHexString(signedMessage);
    }

    private String getSimpleStorageBinary() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("solidity/SimpleStorage.bin"));
        String bin = br.readLine();
        br.close();
        return bin;
    }

    private BigInteger getNonce(String address) throws ExecutionException, InterruptedException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        System.out.println("nonce : " + nonce);
        return nonce;
    }

    private void waitTransactionReceipt(String transactionHash) throws InterruptedException, IOException {
        while (true) {
            EthGetTransactionReceipt egtr = web3j.ethGetTransactionReceipt(transactionHash).send();
            if(egtr.hasError()) {
                System.out.println(egtr.getError().getMessage());
                break;
            }
            if (egtr.getResult() != null) {
                System.out.println("Transaction Receipt : " + egtr.getTransactionReceipt());
                break;
            } else {
                System.out.println("waiting for receive transaction receipt");
            }
            Thread.sleep(5000);
        }
    }
}

