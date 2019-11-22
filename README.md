# anduschain-java-example
anduschain-java-exmaple

## package
git clone https://github.com/anduschain/anduschain-java-example.git
    
    import io.anduschain.javasdk.AnduschainDefaultGasProvider;
    import io.anduschain.javasdk.AnduschainRawTransaction;
    import io.anduschain.javasdk.AnduschainRawTransactionManager;
    import io.anduschain.javasdk.AnduschainTransactionEncoder;
    

## 1. connection 
    web3 = Web3j.build(new HttpService("HTTP://127.0.0.1:8545"));        //for localhost
    Web3j web3 = Web3j.build(new HttpService("HTTP://127.0.0.1:7545"));  //for ganache

## 2. create wallet : make_wallet()
    make_wallet() { 
      String walletFileName = WalletUtils.generateFullNewWalletFile(passWord, new File("path/to/file"));
      ...
    }

## 3. transfer (create transaction) : testTransfer() -> testCheckBalance()

### 1) get nonce
    EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(...).sendAsync().get();
    BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        
### 2) create transaction
    AnduschainRawTransaction rtm = AnduschainRawTransaction.createEtherTransaction(....)
### 3) load credentials
    Credentials credentials = WalletUtils.loadCredentials(...)
### 4) sign and send
    byte[] signedMessage = AnduschainTransactionEncoder.signMessage(rtm, credentials);
    String hexValue = Numeric.toHexString(signedMessage);
    ethCall = web3.ethSendRawTransaction(hexValue).sendAsync().get();
        
## 4. contract (create transaction) : testContract()
### 1) contract code 
you need to pre-compiled contract code.

### 2) load credential
    Credentials credentials = WalletUtils.loadCredentials(...)

### 3) deploy contract
    //SimpleStorage is a pre-compiled solidity contract code.
    SimpleStorage ss = SimpleStorage.deploy(...)
    
    or createContractTransaction(...)
    AnduschainRawTransaction artm = AnduschainRawTransaction.createContractTransaction(
        new BigInteger("2"),
        nonce,
        new BigInteger("23809523805524"),
        new BigInteger("2100000"),
        BigInteger.ZERO,
        getSimpleStorageBinary() // read file (solidity/compiledFile.bin)
    );
    

### 4) Get the transaction recipt
    ss.getTransactionReceipt()
    or see waitTransactionReceipt(String transactionHash)
