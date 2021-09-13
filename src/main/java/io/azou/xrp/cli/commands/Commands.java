package io.azou.xrp.cli.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.UnsignedInteger;
import java.math.BigDecimal;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

public interface Commands {

  String newAccount();

  BigDecimal getBalance(Address address);

  String fundAccount(String address);

  XrpCurrencyAmount baseFee() throws JsonRpcClientErrorException;

  UnsignedInteger accountSequence(Address address) throws JsonRpcClientErrorException;

  String preparePayment(BigDecimal amount, Address destination) throws JsonRpcClientErrorException;

  String sendPayment(BigDecimal amount, Address destination)
      throws JsonRpcClientErrorException, JsonProcessingException;
}
