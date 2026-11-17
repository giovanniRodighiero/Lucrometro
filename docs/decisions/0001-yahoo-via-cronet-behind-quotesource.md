# 0001: Yahoo Finance via Cronet, behind a QuoteSource interface

Status: accepted (2026-11-15)

## Context
Requirement: free data for Milan stocks and ETFs, no own server. Licensed APIs checked have no
free Milan quotes (docs/research/data-sources.md). Yahoo works but rejects non-browser TLS
fingerprints (plain OkHttp/curl got 429).

## Decision
- Use Yahoo's unofficial endpoints through `cronet-embedded` (Chrome's network stack, bundled,
  independent of device/Play Services).
- Hide it behind `QuoteSource` (`data/QuoteSource.kt`) so the provider can be swapped.

## Consequences
- Verified on the emulator for SMH.MI. Not yet verified on a real phone, intraday, or at volume.
- Unofficial API: may break or block at any time. No fallback provider in v1 (see docs/prd.md); Stooq
  and Alpha Vantage are candidates to evaluate if it does.
- APK grows by a few MB (native library).
- Rejected for now: OkHttp with tuned TLS (brittle), curl-impersonate build (last resort).
