Java Bitxhub Client
=====
![build](https://github.com/meshplus/java-bitxhub-client/workflows/build/badge.svg)
[![codecov](https://codecov.io/gh/meshplus/java-bitxhub-client/branch/master/graph/badge.svg)](https://codecov.io/gh/meshplus/java-bitxhub-client)
This SDK enables Java developers to build solutions that interact with BitXHub.

## Prepare
You need to have the following dependencies installed on your machine.
- [__Java8+__](https://www.oracle.com/java/technologies/javase-downloads.html)
- [__Maven3.3+__](https://maven.apache.org/download.cgi)

## Getting started
Obtain the client SDK packages for BitXHub.
```shell script
git clone https://github.com/meshplus/java-bitxhub-client.git
```

### Installing
Use the `Maven` command to install the SDK.
```shell script
mvn clean package -DskipTests
```
Then you can get the `java-bitxhub-client-0.1.0-jar-with-dependencies.jar` in the `target` folder.

You're good to go, happy coding! Check out the examples for usage demonstrations.

### Examples

- [RPC Test](src/test/java/cn/dmlab/bitxhub/RPCTest.java): Basic example that uses SDK to query and execute transaction.
- [Block Test](src/test/java/cn/dmlab/bitxhub/BlockTest.java): Basic example that uses SDK to query blocks.
- [Contract Test](src/test/java/cn/dmlab/bitxhub/ContractTest.java): Basic example that uses SDK to deploy and invoke contract.
- [Subscribe Test](src/test/java/cn/dmlab/bitxhub/SubscribeTest.java): An example that uses SDK to subscribe the block event.
- [Sync Test](src/test/java/cn/dmlab/bitxhub/SyncTest.java): An example that uses SDK to sync the merkle wrapper.
- [Appchain Test](src/test/java/cn/dmlab/bitxhub/AppchainTest.java): Basic example that uses SDK to register and adult appchain. 

### Documentation

SDK documentation can be viewed at [JavaDoc](https://github.com/meshplus/java-bitxhub-client/wiki/Java-SDK%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3).

## Client SDK
You should start [BitXHub](https://github.com/meshplus/bitxhub) before using SDK.

### Running the test

```shell script
# In the BitXHub SDK Java directory
cd java-bitxhub-client/

# Running test
mvn test
```

### Contributing
See [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

[Apache-2.0](https://github.com/meshplus/java-bitxhub-client/blob/master/LICENSE)