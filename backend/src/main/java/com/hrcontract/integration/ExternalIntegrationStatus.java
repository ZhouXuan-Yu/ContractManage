package com.hrcontract.integration;

/** Deliberately vendor-neutral status returned before a real external contract is available. */
public record ExternalIntegrationStatus(String capability, String provider, String mode, boolean available, String note) {}
