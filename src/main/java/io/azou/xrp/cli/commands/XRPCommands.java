package io.azou.xrp.cli.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedInteger;
import java.math.BigDecimal;
import okhttp3.HttpUrl;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.crypto.KeyMetadata;
import org.xrpl.xrpl4j.crypto.PrivateKey;
import org.xrpl.xrpl4j.crypto.signing.SignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.SingleKeySignatureService;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;
import org.xrpl.xrpl4j.wallet.SeedWalletGenerationResult;
import org.xrpl.xrpl4j.wallet.Wallet;

@ShellComponent
public class XRPCommands implements Commands {

  protected static final XrplClient client =
      new XrplClient(HttpUrl.parse("https://s.altnet.rippletest.net:51234"));
  private static final HttpUrl FAUCET_URL = HttpUrl.parse("https://faucet.altnet.rippletest.net");
  @Autowired private LineReader lineReader;
  private ObjectMapper mapper = ObjectMapperFactory.create();

  @Override
  @ShellMethod(value = "Generate a new account")
  public String newAccount() {
    // create a new wallet which contains the public key and the seed
    SeedWalletGenerationResult result = DefaultWalletFactory.getInstance().randomWallet(true);
    return String.format("Address: %s  Seed: %s", result.wallet().classicAddress(), result.seed());
  }

  @Override
  @ShellMethod(value = "Get balance")
  public BigDecimal getBalance(Address address) {
    // go to test net and find the balance by its address
    try {
      return client
          .accountInfo(AccountInfoRequestParams.of(address))
          .accountData()
          .balance()
          .toXrp();
    } catch (JsonRpcClientErrorException exception) {
      throw new RuntimeException(exception.getMessage());
    }
  }

  @Override
  @ShellMethod(value = "Fund an account")
  public String fundAccount(String address) {
    // test faucet would fund the account: need to create a FaucetClient first
    return prettyPrint(
        FaucetClient.construct(FAUCET_URL).fundAccount(FundAccountRequest.of(Address.of(address))));
  }

  @Override
  @ShellMethod(value = "Get current network base fee")
  public XrpCurrencyAmount baseFee() throws JsonRpcClientErrorException {
    return client.fee().drops().baseFee();
  }

  @Override
  @ShellMethod(value = "Account sequence")
  public UnsignedInteger accountSequence(Address address) throws JsonRpcClientErrorException {
    return client.accountInfo(AccountInfoRequestParams.of(address)).accountData().sequence();
  }

  @Override
  @ShellMethod(value = "Prepare payment")
  public String preparePayment(BigDecimal amount, Address destination)
      throws JsonRpcClientErrorException {
    Wallet wallet = getWallet();
    Payment payment = buildPayment(wallet, amount, destination);
    return prettyPrint(payment);
  }

  @Override
  @ShellMethod(value = "Send payment")
  public String sendPayment(BigDecimal amount, Address destination)
      throws JsonRpcClientErrorException, JsonProcessingException {
    // Get the wallet (from input seed) and build a payment (which is signed with the public key and
    // includes the sending address and receiver address
    Wallet wallet = getWallet();
    Payment payment = buildPayment(wallet, amount, destination);

    // Use the private key from the wallet to sign the payment
    PrivateKey privateKey = PrivateKey.fromBase16EncodedPrivateKey(wallet.privateKey().get());

    SignedTransaction<Payment> signedPayment =
        new SingleKeySignatureService(privateKey).sign(KeyMetadata.EMPTY, payment);

    return prettyPrint(client.submit(signedPayment));
  }

  private Wallet getWallet() {
    String seed = lineReader.readLine("Enter seed> ", '*'); // the second arg is the mask
    return DefaultWalletFactory.getInstance().fromSeed(seed, true);
  }

  private Payment buildPayment(Wallet wallet, BigDecimal amount, Address destination)
      throws JsonRpcClientErrorException {
    return Payment.builder()
        .signingPublicKey(wallet.publicKey())
        .account(wallet.classicAddress())
        .fee(baseFee())
        .sequence(accountSequence(wallet.classicAddress()))
        .amount(XrpCurrencyAmount.ofXrp(amount))
        .destination(destination)
        .build();
  }

  private String prettyPrint(Object result) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    } catch (JsonProcessingException e) {
      return String.valueOf(result);
    }
  }
}
