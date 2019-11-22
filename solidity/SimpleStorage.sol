// solc SimpleStorage.sol --bin --abi --optimize -o .
pragma solidity >0.4.99 <0.6.0;

contract SimpleStorage {
    uint storeData = 100;

    function set(uint x) public {
        storeData = x;
    }

    function get() public view returns (uint) {
        return storeData;
    }
}
