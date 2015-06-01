package it.unica.co2.fasterxml;

import it.unica.co2.model.contract.Recursion;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="class", defaultImpl=Recursion.class)
public abstract class ContractMixIn {

}
