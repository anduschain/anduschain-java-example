import io.anduschain.javasdk.*;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
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

    static Web3j web3;

    /****************************************************************
     * dockerNodeAddress : input your docker node address
     * dockerNodePrivateKey : input your docker node private key
     * ganache : If you use ganache, input ganache address.
     ****************************************************************/
    // for anduschain-docker-control
    String dockerNodeAddress = "node address";
    String dockerNodePrivateKey = "private key";

    // for ganache
    String ganache = "address";

    //credentials : You can use your key-store or use it.
    String keyStorePath = new File("wallet/UTC--2019-10-17T01-50-41.737000000Z--a2ae980a5b4e1cd07b36803e4d978dbd65da3f34.json").getAbsolutePath();
    String passWord = "1111";
    String myAddress = "0xa2ae980a5b4e1cd07b36803e4d978dbd65da3f34";

    public static void main(String[] args) {
        //you can connect anduschain-docker-control node running on localhost
        web3 = Web3j.build(new HttpService("HTTP://127.0.0.1:8545"));
//        Web3j web3 = Web3j.build(new HttpService("HTTP://127.0.0.1:7545"));  //for ganache

        System.out.println("start Test");
        Test tt = new Test();
        try {
            tt.getClientVersion();
            tt.getBlockNumber();
//            tt.make_wallet("path/to/file");  //make wallet need to define path
            System.out.println("------------------------------------------------------");
            tt.testCheckBalance();
            System.out.println("------------------------------------------------------");
            tt.testReceiveCoin();
            System.out.println("------------------------------------------------------");
            tt.testSendCoin();
            System.out.println("------------------------------------------------------");
            tt.testCheckBalance();
            System.out.println("------------------------------------------------------");
            tt.testContract();
            System.out.println("------------------------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("end Test");
        }
    }

    private void getClientVersion() throws IOException {
        Web3ClientVersion clientVersion = web3.web3ClientVersion().send();
        System.out.println("Client version: " + clientVersion.getWeb3ClientVersion());
    }

    private void getBlockNumber() throws IOException {
        EthBlockNumber blockNumber = web3.ethBlockNumber().send();
        System.out.println("current Block number: " + blockNumber.getBlockNumber());
    }

    private void make_wallet(String path) throws CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        String walletFileName = WalletUtils.generateFullNewWalletFile(passWord, new File(path));
        String[] fetchAddress = walletFileName.split("--");
        String getAddress = fetchAddress[fetchAddress.length - 1].split("\\.")[0];
        System.out.println("Address : " + getAddress);
    }

    private void testReceiveCoin() throws IOException, CipherException, ExecutionException, InterruptedException {
        System.out.println("testReceiveCoin...");
        EthSendTransaction ethCall = null;

        //get senders address using private key
        Credentials credentials = Credentials.create(dockerNodePrivateKey);
        dockerNodeAddress = credentials.getAddress(); // target Address means sender's address in this function.

        EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(
                dockerNodeAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        AnduschainRawTransaction rtm = AnduschainRawTransaction.createEtherTransaction(
                new BigInteger("0"),
                nonce,
                new BigInteger("23809523805524"), // Gas price must greater than 23809523805524
                new BigInteger("21000"),
                myAddress,
                Convert.toWei("5", Convert.Unit.ETHER).toBigInteger()
        );

        byte[] signedMessage = AnduschainTransactionEncoder.signMessage(rtm, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        ethCall = web3.ethSendRawTransaction(hexValue).sendAsync().get();

        if (ethCall.hasError()) {
            System.out.println(ethCall.getError().getMessage());
        } else {
            System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
        }
        waitTransactionReceipt(ethCall.getTransactionHash());
    }


    private void testSendCoin() throws IOException, CipherException, ExecutionException, InterruptedException {
        System.out.println("testTransfer...");
        EthSendTransaction ethCall = null;

        EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(
                myAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        AnduschainRawTransaction rtm = AnduschainRawTransaction.createEtherTransaction(
                new BigInteger("0"),
                nonce,
                new BigInteger("23809523805524"),
                new BigInteger("21000"),
                dockerNodeAddress,
                Convert.toWei("1", Convert.Unit.ETHER).toBigInteger()
        );

        Credentials credentials = WalletUtils.loadCredentials(
                passWord,
                keyStorePath);

        byte[] signedMessage = AnduschainTransactionEncoder.signMessage(rtm, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        ethCall = web3.ethSendRawTransaction(hexValue).sendAsync().get();

        if (ethCall.hasError()) {
            System.out.println(ethCall.getError().getMessage());
        } else {
            System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
        }
        waitTransactionReceipt(ethCall.getTransactionHash());
    }

    private void testContract() throws Exception {
        System.out.println("testContract...");
        EthSendTransaction ethCall = null;

        EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(
                myAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        Credentials credentials = WalletUtils.loadCredentials(
                passWord,
                keyStorePath);

        AnduschainRawTransaction artm = AnduschainRawTransaction.createContractTransaction(
                new BigInteger("2"),
                nonce,
                new BigInteger("23809523805524"),
                new BigInteger("2100000"),
                BigInteger.ZERO,
                getSimpleStorageBinary()
        );

        byte[] signedMessage = AnduschainTransactionEncoder.signMessage(artm, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        ethCall = web3.ethSendRawTransaction(hexValue).sendAsync().get();

        if (ethCall.hasError()) {
            System.out.println(ethCall.getError().getMessage());
        } else {
            System.out.println("Transaction Hash : " + ethCall.getTransactionHash());
        }

        waitTransactionReceipt(ethCall.getTransactionHash());
    }

    private void testCheckBalance() throws IOException {
        System.out.println("testCheckBlanace...");
        EthGetBalance myBalance = web3.ethGetBalance(myAddress, DefaultBlockParameter.valueOf("latest")).send();
        EthGetBalance targetBalance = web3.ethGetBalance(dockerNodeAddress, DefaultBlockParameter.valueOf("latest")).send();
        System.out.println("my balance : " + Convert.fromWei(myBalance.getBalance().toString(), Convert.Unit.ETHER));
        System.out.println("docker node balance : " + Convert.fromWei(targetBalance.getBalance().toString(), Convert.Unit.ETHER));
    }

    private static String getSimpleStorageBinary() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("solidity/SimpleStorage.bin"));
        String bin = br.readLine();
        br.close();
        return bin;
    }

    private static void waitTransactionReceipt(String transactionHash) throws InterruptedException, IOException {
        while (true) {
            EthGetTransactionReceipt egtr = web3.ethGetTransactionReceipt(transactionHash).send();
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

